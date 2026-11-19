package ar.sf.sicat.admin.importacion;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.sf.sicat.admin.importacion.dto.GrupoDuplicadosDto;
import ar.sf.sicat.admin.importacion.dto.GrupoSolicitud;
import ar.sf.sicat.admin.importacion.dto.PreviaResolucionDto;
import ar.sf.sicat.admin.importacion.dto.ResolucionResultadoDto;
import ar.sf.sicat.admin.importacion.lectura.Texto;
import ar.sf.sicat.admin.publicacion.PublicacionService;
import ar.sf.sicat.core.comun.ConflictoException;
import ar.sf.sicat.core.comun.ReglaNegocioException;
import ar.sf.sicat.core.dominio.clasificacion.Categoria;
import ar.sf.sicat.core.dominio.contacto.CanalContacto;
import ar.sf.sicat.core.dominio.contacto.CanalContactoRepository;
import ar.sf.sicat.core.dominio.establecimiento.DescarteDuplicado;
import ar.sf.sicat.core.dominio.establecimiento.DescarteDuplicadoRepository;
import ar.sf.sicat.core.dominio.establecimiento.Establecimiento;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoCaracteristica;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoCaracteristicaRepository;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoRepository;
import ar.sf.sicat.core.dominio.oferta.Oferta;
import ar.sf.sicat.core.dominio.oferta.OfertaRepository;

/**
 * Posibles duplicados: establecimientos activos con el mismo nombre normalizado. Un grupo marcado "no son
 * duplicados" deja de aparecer mientras conserve los mismos integrantes.
 */
@Service
public class DuplicadosService {

    private final EstablecimientoRepository establecimientos;
    private final DescarteDuplicadoRepository descartes;
    private final OfertaRepository ofertas;
    private final CanalContactoRepository contactos;
    private final EstablecimientoCaracteristicaRepository caracteristicas;

    public DuplicadosService(EstablecimientoRepository establecimientos, DescarteDuplicadoRepository descartes,
                             OfertaRepository ofertas, CanalContactoRepository contactos,
                             EstablecimientoCaracteristicaRepository caracteristicas) {
        this.establecimientos = establecimientos;
        this.descartes = descartes;
        this.ofertas = ofertas;
        this.contactos = contactos;
        this.caracteristicas = caracteristicas;
    }

    /** Grupos sin descartar, primero los que comparten dirección, con lo necesario para compararlos en columnas. */
    @Transactional(readOnly = true)
    public List<GrupoDuplicadosDto> buscar() {
        Map<String, List<Establecimiento>> porNombre = establecimientos.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getActivo()))
                .collect(Collectors.groupingBy(e -> Texto.clave(e.getNombre()), TreeMap::new, Collectors.toList()));
        Set<String> descartados = descartados();
        Map<String, List<Establecimiento>> grupos = porNombre.entrySet().stream()
                .filter(en -> en.getValue().size() > 1)
                .filter(en -> !descartados.contains(clave(en.getKey(), en.getValue().stream().map(Establecimiento::getId).toList())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));

        // Ofertas y contactos de todos los integrantes: dos consultas en total.
        List<Long> ids = grupos.values().stream().flatMap(List::stream).map(Establecimiento::getId).toList();
        Map<Long, List<Oferta>> ofertasDe = ids.isEmpty() ? Map.of() : ofertas.conCategoriasDe(ids).stream()
                .collect(Collectors.groupingBy(o -> o.getEstablecimiento().getId()));
        Map<Long, List<CanalContacto>> contactosDe = ids.isEmpty() ? Map.of() : contactos.deEstablecimientos(ids).stream()
                .collect(Collectors.groupingBy(c -> c.getEstablecimiento().getId()));

        return grupos.entrySet().stream()
                .map(en -> {
                    List<GrupoDuplicadosDto.Item> items = en.getValue().stream()
                            .sorted(Comparator.comparing(Establecimiento::getId))
                            .map(e -> item(e, ofertasDe.getOrDefault(e.getId(), List.of()),
                                    contactosDe.getOrDefault(e.getId(), List.of())))
                            .toList();
                    long direcciones = items.stream().map(i -> Texto.clave(i.direccion())).distinct().count();
                    return new GrupoDuplicadosDto(en.getKey(), direcciones == 1, items);
                })
                .sorted(Comparator.comparing(GrupoDuplicadosDto::mismaDireccion).reversed()
                        .thenComparing(GrupoDuplicadosDto::clave))
                .toList();
    }

    /** Cantidad de grupos que devolvería {@link #buscar()}, sin cargar entidades. */
    @Transactional(readOnly = true)
    public long contarGrupos() {
        Set<String> descartados = descartados();
        return integrantesPorClave().entrySet().stream()
                .filter(en -> en.getValue().size() > 1 && !descartados.contains(clave(en.getKey(), en.getValue())))
                .count();
    }

    /** "No son duplicados": el grupo deja de aparecer mientras conserve exactamente estos integrantes. */
    @Transactional
    public void descartar(GrupoSolicitud grupo) {
        List<Long> actuales = verificarGrupo(grupo);
        String firma = firma(actuales);
        if (!descartes.existsByClaveAndIntegrantes(grupo.clave(), firma)) {
            descartes.save(new DescarteDuplicado(grupo.clave(), firma));
        }
    }

    /** Lo que se le copiaría al que queda: contactos y características de los demás que no tiene. */
    @Transactional(readOnly = true)
    public PreviaResolucionDto previa(Long conservarId, GrupoSolicitud grupo) {
        List<Long> actuales = verificarGrupo(grupo);
        verificarConservar(conservarId, actuales);
        Copia c = copia(conservarId, actuales);
        Map<Long, String> nombres = establecimientos.findAllById(actuales).stream()
                .collect(Collectors.toMap(Establecimiento::getId, Establecimiento::getNombre));
        List<PreviaResolucionDto.Origen> origenes = actuales.stream()
                .filter(id -> !id.equals(conservarId))
                .map(id -> new PreviaResolucionDto.Origen(id, nombres.get(id),
                        c.contactos().stream().filter(x -> x.getEstablecimiento().getId().equals(id))
                                .map(x -> new GrupoDuplicadosDto.Contacto(x.getTipoCanal().getNombre(), x.getValor(),
                                        Boolean.TRUE.equals(x.getEsPublico()))).toList(),
                        c.caracteristicas().stream().filter(x -> x.getEstablecimiento().getId().equals(id))
                                .map(x -> new PreviaResolucionDto.Caracteristica(x.getCaracteristica().getGrupo(),
                                        x.getCaracteristica().getNombre(), x.getCantidad())).toList()))
                .toList();
        return new PreviaResolucionDto(conservarId, c.contactos().size(), c.caracteristicas().size(), origenes);
    }

    /**
     * "Quedarme con este": opcionalmente le copia al que queda los contactos y características que le faltan, y da
     * de baja a los demás integrantes (un publicado pasa a OCULTO, como cualquier baja). Todo en una transacción y por
     * JPA (queda auditado): si algo falla, no cambia nada. Se copia en vez de mover: el dado de baja conserva sus datos
     * por si se lo reactiva.
     */
    @Transactional
    public ResolucionResultadoDto quedarseCon(Long conservarId, GrupoSolicitud grupo, boolean copiarContactos,
                                              boolean copiarCaracteristicas) {
        List<Long> actuales = verificarGrupo(grupo);
        verificarConservar(conservarId, actuales);
        Establecimiento queda = establecimientos.findById(conservarId).orElseThrow();
        Copia c = copia(conservarId, actuales);
        int contactosCopiados = 0;
        int caracteristicasCopiadas = 0;
        if (copiarContactos) {
            short orden = (short) (contactos.findByEstablecimientoIdOrderByOrden(conservarId).stream()
                    .map(CanalContacto::getOrden).filter(java.util.Objects::nonNull)
                    .mapToInt(Short::intValue).max().orElse(0));
            for (CanalContacto origen : c.contactos()) {
                CanalContacto nuevo = new CanalContacto();
                nuevo.setEstablecimiento(queda);
                nuevo.setTipoCanal(origen.getTipoCanal());
                nuevo.setValor(origen.getValor());
                nuevo.setUso(origen.getUso());
                nuevo.setEsPublico(origen.getEsPublico());
                nuevo.setOrden(++orden);
                contactos.save(nuevo);
                contactosCopiados++;
            }
        }
        if (copiarCaracteristicas) {
            for (EstablecimientoCaracteristica origen : c.caracteristicas()) {
                EstablecimientoCaracteristica nueva = new EstablecimientoCaracteristica();
                nueva.setEstablecimiento(queda);
                nueva.setCaracteristica(origen.getCaracteristica());
                nueva.setCantidad(origen.getCantidad());
                caracteristicas.save(nueva);
                caracteristicasCopiadas++;
            }
        }
        List<Long> bajas = actuales.stream().filter(id -> !id.equals(conservarId)).toList();
        establecimientos.findAllById(bajas).forEach(e -> {
            e.setActivo(false);
            PublicacionService.ocultarSiEstaPublicado(e);
        });
        return new ResolucionResultadoDto(bajas, contactosCopiados, caracteristicasCopiadas);
    }

    /** Contactos y características de los demás que el que queda no tiene, sin repetir entre ellos. */
    private record Copia(List<CanalContacto> contactos, List<EstablecimientoCaracteristica> caracteristicas) {
    }

    private Copia copia(Long conservarId, List<Long> integrantes) {
        List<Long> otros = integrantes.stream().filter(id -> !id.equals(conservarId)).sorted().toList();
        // Contactos: primero los del que queda (ya los tiene); después, en orden de id, los de los demás.
        Set<String> vistos = new java.util.HashSet<>();
        contactos.deEstablecimientos(List.of(conservarId))
                .forEach(x -> vistos.add(ClaveContacto.de(x.getTipoCanal().getNombre(), x.getValor())));
        List<CanalContacto> deOtros = contactos.deEstablecimientos(otros).stream()
                .sorted(Comparator.comparing((CanalContacto x) -> x.getEstablecimiento().getId())
                        .thenComparing(x -> x.getOrden() == null ? Short.MAX_VALUE : x.getOrden())
                        .thenComparing(CanalContacto::getId))
                .toList();
        List<CanalContacto> aCopiar = deOtros.stream()
                .filter(x -> vistos.add(ClaveContacto.de(x.getTipoCanal().getNombre(), x.getValor())))
                .toList();
        // Características: las que el que queda no tiene; si dos de los demás la tienen, la del de menor id.
        Set<Long> tiene = new java.util.HashSet<>();
        caracteristicas.findByEstablecimientoId(conservarId).forEach(x -> tiene.add(x.getCaracteristica().getId()));
        List<EstablecimientoCaracteristica> aAgregar = caracteristicas.deEstablecimientos(otros).stream()
                .sorted(Comparator.comparing((EstablecimientoCaracteristica x) -> x.getEstablecimiento().getId())
                        .thenComparing(x -> x.getCaracteristica().getGrupo())
                        .thenComparing(x -> x.getCaracteristica().getNombre()))
                .filter(x -> tiene.add(x.getCaracteristica().getId()))
                .toList();
        return new Copia(aCopiar, aAgregar);
    }

    private static void verificarConservar(Long conservarId, List<Long> actuales) {
        if (!actuales.contains(conservarId)) {
            throw new ReglaNegocioException("El establecimiento a conservar no es parte del grupo");
        }
    }

    /** Integrantes actuales del grupo; 409 si no son los que se vieron (el grupo cambió mientras tanto). */
    private List<Long> verificarGrupo(GrupoSolicitud grupo) {
        List<Long> actuales = integrantesPorClave().getOrDefault(grupo.clave(), List.of());
        if (actuales.size() < 2) {
            throw new ConflictoException("Ese grupo ya no tiene posibles duplicados: actualizá la lista");
        }
        if (!firma(actuales).equals(firma(grupo.integrantes()))) {
            throw new ConflictoException("El grupo cambió desde que se cargó la lista: actualizala y revisalo de nuevo");
        }
        return actuales;
    }

    private Map<String, List<Long>> integrantesPorClave() {
        return establecimientos.idsYNombresActivos().stream()
                .collect(Collectors.groupingBy(f -> Texto.clave((String) f[1]),
                        Collectors.mapping(f -> ((Number) f[0]).longValue(), Collectors.toList())));
    }

    /** Todos los descartes en una consulta, como "clave|firma". */
    private Set<String> descartados() {
        return descartes.findAll().stream().map(d -> d.getClave() + "|" + d.getIntegrantes()).collect(Collectors.toSet());
    }

    private static String clave(String clave, List<Long> integrantes) {
        return clave + "|" + firma(integrantes);
    }

    /** "12,40,371": ids sin repetir y ordenados. */
    static String firma(List<Long> ids) {
        return ids.stream().distinct().sorted().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static GrupoDuplicadosDto.Item item(Establecimiento e, List<Oferta> suyas, List<CanalContacto> canales) {
        List<Oferta> activas = suyas.stream().filter(o -> Boolean.TRUE.equals(o.getActivo())).toList();
        List<String> categorias = activas.stream()
                .flatMap(o -> Stream.concat(Stream.of(o.getCategoria()), o.getCategoriasAdicionales().stream()))
                .map(Categoria::getNombre)
                .collect(Collectors.toCollection(LinkedHashSet::new)).stream().toList();
        return new GrupoDuplicadosDto.Item(e.getId(), e.getNombre(), Duplicados.direccion(e.getLugar()),
                e.getEstadoPublicacion(), e.getOferente().getNombre(), categorias, activas.size(),
                canales.stream().map(c -> new GrupoDuplicadosDto.Contacto(c.getTipoCanal().getNombre(), c.getValor(),
                        Boolean.TRUE.equals(c.getEsPublico()))).toList());
    }
}
