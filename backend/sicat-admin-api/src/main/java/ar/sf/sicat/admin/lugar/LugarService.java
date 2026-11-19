package ar.sf.sicat.admin.lugar;

import static ar.sf.sicat.admin.comun.Entidades.buscar;
import static ar.sf.sicat.admin.comun.Entidades.buscarOpcional;
import static ar.sf.sicat.admin.comun.Entidades.texto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.sf.sicat.admin.lugar.dto.LugarDto;
import ar.sf.sicat.admin.lugar.dto.LugarSolicitud;
import ar.sf.sicat.admin.publicacion.PublicacionService;
import ar.sf.sicat.core.comun.Bbox;
import ar.sf.sicat.core.comun.ConflictoException;
import ar.sf.sicat.core.comun.Geo;
import ar.sf.sicat.core.comun.GeoJson;
import ar.sf.sicat.core.comun.PaginaDto;
import ar.sf.sicat.core.comun.ReglaNegocioException;
import ar.sf.sicat.core.dominio.especializacion.circuito.CircuitoRepository;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoRepository;
import ar.sf.sicat.core.dominio.lugar.LocalidadRepository;
import ar.sf.sicat.core.dominio.lugar.Lugar;
import ar.sf.sicat.core.dominio.lugar.LugarRepository;
import ar.sf.sicat.core.dominio.lugar.TipoLugarRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class LugarService {

    private final LugarRepository lugares;
    private final TipoLugarRepository tipos;
    private final LocalidadRepository localidades;
    private final EstablecimientoRepository establecimientos;
    private final CircuitoRepository circuitos;
    private final PublicacionService publicacion;

    public LugarService(LugarRepository lugares, TipoLugarRepository tipos, LocalidadRepository localidades,
                        EstablecimientoRepository establecimientos, CircuitoRepository circuitos,
                        PublicacionService publicacion) {
        this.lugares = lugares;
        this.tipos = tipos;
        this.localidades = localidades;
        this.establecimientos = establecimientos;
        this.circuitos = circuitos;
        this.publicacion = publicacion;
    }

    /** Orden de la cola de revisión: primero lo más dudoso. */
    public static final String ORDEN_DUDA = "duda";

    /**
     * Con {@code orden = duda} se ordena NO_ENCONTRADO, APROXIMADO, PENDIENTE, AUTOMATICO y el resto, y por nombre
     * dentro de cada estado (el orden de {@code pageable} se ignora). Cada fila trae su señal de calidad.
     */
    @Transactional(readOnly = true)
    public PaginaDto<LugarDto> listar(String q, Long tipoLugarId, Long padreId, Long localidadId, String bbox,
                                      java.util.Set<String> estadosGeo, String orden, Pageable pageable) {
        boolean porDuda = ORDEN_DUDA.equals(orden);
        Bbox caja = Bbox.parse(bbox);
        List<Long> enCaja = caja == null ? null
                : lugares.idsEnBbox(caja.minLng(), caja.minLat(), caja.maxLng(), caja.maxLat());
        Specification<Lugar> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String patron = "%" + q.trim().toLowerCase() + "%";
                // También por el nombre de un establecimiento ubicado ahí (muchos lugares se llaman como su dirección).
                var sub = query.subquery(Long.class);
                var est = sub.from(ar.sf.sicat.core.dominio.establecimiento.Establecimiento.class);
                sub.select(est.get("lugar").get("id")).where(cb.like(cb.lower(est.get("nombre")), patron));
                p.add(cb.or(cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(cb.lower(root.get("calle")), patron),
                        root.get("id").in(sub)));
            }
            if (tipoLugarId != null) {
                p.add(cb.equal(root.get("tipoLugar").get("id"), tipoLugarId));
            }
            if (padreId != null) {
                p.add(cb.equal(root.get("lugarPadre").get("id"), padreId));
            }
            if (localidadId != null) {
                p.add(cb.equal(root.get("localidad").get("id"), localidadId));
            }
            if (enCaja != null) {
                p.add(enCaja.isEmpty() ? cb.disjunction() : root.get("id").in(enCaja));
            }
            if (estadosGeo != null && !estadosGeo.isEmpty()) {
                p.add(root.get("estadoGeo").in(estadosGeo));
            }
            // La consulta de conteo (resultado Long) no lleva orden.
            if (porDuda && !Long.class.equals(query.getResultType())) {
                var estado = root.get("estadoGeo");
                query.orderBy(cb.asc(cb.selectCase()
                                .when(cb.equal(estado, "NO_ENCONTRADO"), 0)
                                .when(cb.equal(estado, "APROXIMADO"), 1)
                                .when(cb.equal(estado, "PENDIENTE"), 2)
                                .when(cb.equal(estado, "AUTOMATICO"), 3)
                                .otherwise(4)),
                        cb.asc(cb.lower(root.get("nombre"))), cb.asc(root.get("id")));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        Pageable pagina = porDuda ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()) : pageable;
        var resultado = lugares.findAll(spec, pagina);
        Map<Long, Long> compartidos = compartidos(resultado.getContent());
        return PaginaDto.de(resultado, l -> dto(l, false, compartidos.getOrDefault(l.getId(), 0L)));
    }

    /** Una consulta para toda la página: cuántos otros lugares comparten el punto de cada uno. */
    private Map<Long, Long> compartidos(List<Lugar> pagina) {
        List<Long> ids = pagina.stream().filter(l -> l.getPunto() != null).map(Lugar::getId).toList();
        Map<Long, Long> r = new HashMap<>();
        if (!ids.isEmpty()) {
            lugares.puntosCompartidos(ids).forEach(f -> r.put(((Number) f[0]).longValue(), ((Number) f[1]).longValue()));
        }
        return r;
    }

    @Transactional(readOnly = true)
    public LugarDto detalle(Long id) {
        Lugar l = buscar(lugares, id, "Lugar");
        return dto(l, true, compartidos(List.of(l)).getOrDefault(id, 0L));
    }

    @Transactional
    public LugarDto crear(LugarSolicitud s) {
        Lugar l = new Lugar();
        aplicar(l, s);
        return dto(lugares.save(l), true);
    }

    @Transactional
    public LugarDto modificar(Long id, LugarSolicitud s) {
        Lugar l = buscar(lugares, id, "Lugar");
        aplicar(l, s);
        publicacion.verificarPublicados(establecimientos.findByLugarId(id));
        return dto(l, true);
    }

    /** Lugar no tiene baja lógica: se elimina solo si nadie lo usa. */
    @Transactional
    public void eliminar(Long id) {
        Lugar l = buscar(lugares, id, "Lugar");
        if (lugares.existsByLugarPadreId(id)) {
            throw new ConflictoException("El lugar contiene otros lugares");
        }
        if (establecimientos.existsByLugarId(id)) {
            throw new ConflictoException("El lugar está asignado a uno o más establecimientos");
        }
        if (circuitos.existsByLugarPartidaId(id)) {
            throw new ConflictoException("El lugar es punto de partida de un circuito");
        }
        lugares.delete(l);
    }

    private void aplicar(Lugar l, LugarSolicitud s) {
        Lugar padre = buscarOpcional(lugares, s.padreId(), "Lugar padre");
        for (Lugar p = padre; p != null; p = p.getLugarPadre()) {
            if (l.getId() != null && p.getId().equals(l.getId())) {
                throw new ReglaNegocioException("Un lugar no puede quedar dentro de sí mismo o de un lugar que contiene");
            }
        }
        l.setNombre(s.nombre().trim());
        l.setLugarPadre(padre);
        l.setTipoLugar(buscarOpcional(tipos, s.tipoLugarId(), "Tipo de lugar"));
        l.setLocalidad(buscarOpcional(localidades, s.localidadId(), "Localidad"));
        l.setCalle(texto(s.calle()));
        l.setAltura(s.altura());
        l.setKm(s.km());
        l.setCalleCruce(texto(s.calleCruce()));
        l.setComplemento(texto(s.complemento()));
        l.setPunto(Geo.punto(s.coordenadas()));
        l.setGeometria(GeoJson.aGeometria(s.geometria()));
        l.setEstadoGeo(texto(s.estadoGeo()));
    }

    LugarDto dto(Lugar l, boolean conHijos) {
        return dto(l, conHijos, null);
    }

    /** {@code compartidos} null: sin señal de calidad (respuestas de alta y modificación). */
    LugarDto dto(Lugar l, boolean conHijos, Long compartidos) {
        List<LugarDto.LugarRef> hijos = !conHijos || l.getId() == null ? null
                : lugares.findByLugarPadreIdOrderByNombre(l.getId()).stream()
                        .map(h -> new LugarDto.LugarRef(h.getId(), h.getNombre())).toList();
        return new LugarDto(l.getId(), l.getNombre(),
                l.getLugarPadre() == null ? null : l.getLugarPadre().getId(),
                l.getLugarPadre() == null ? null : l.getLugarPadre().getNombre(),
                l.getTipoLugar() == null ? null : l.getTipoLugar().getId(),
                l.getTipoLugar() == null ? null : l.getTipoLugar().getNombre(),
                l.getLocalidad() == null ? null : l.getLocalidad().getId(),
                l.getLocalidad() == null ? null : l.getLocalidad().getNombre(),
                l.getCalle(), l.getAltura(), l.getKm(), l.getCalleCruce(), l.getComplemento(),
                Geo.coordenadas(l.getPunto()), GeoJson.aGeoJson(l.getGeometria()), l.getEstadoGeo(), hijos,
                l.getId() == null ? List.of() : establecimientos.findByLugarId(l.getId()).stream()
                        .sorted(java.util.Comparator.comparing(e -> e.getNombre(), String.CASE_INSENSITIVE_ORDER))
                        .map(e -> new LugarDto.EstablecimientoRef(e.getId(), e.getNombre(), e.getEstadoPublicacion(),
                                Boolean.TRUE.equals(e.getActivo())))
                        .toList(),
                compartidos == null ? null : CalidadGeo.de(l, Geo.coordenadas(l.getPunto()), compartidos));
    }
}
