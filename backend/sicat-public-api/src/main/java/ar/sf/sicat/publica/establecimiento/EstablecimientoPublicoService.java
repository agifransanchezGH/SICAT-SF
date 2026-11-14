package ar.sf.sicat.publica.establecimiento;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.sf.sicat.core.comun.Bbox;
import ar.sf.sicat.core.comun.Coordenadas;
import ar.sf.sicat.core.comun.Direcciones;
import ar.sf.sicat.core.comun.EntidadNoEncontradaException;
import ar.sf.sicat.core.comun.PaginaDto;
import ar.sf.sicat.core.vistapublica.AlojamientoPublicoRepository;
import ar.sf.sicat.core.vistapublica.BusquedaPublica;
import ar.sf.sicat.core.vistapublica.CaracteristicaPublicaRepository;
import ar.sf.sicat.core.vistapublica.CircuitoParadaPublica;
import ar.sf.sicat.core.vistapublica.CircuitoParadaPublicaRepository;
import ar.sf.sicat.core.vistapublica.CircuitoPublicoRepository;
import ar.sf.sicat.core.vistapublica.ContactoPublicoRepository;
import ar.sf.sicat.core.vistapublica.DistincionPublicaRepository;
import ar.sf.sicat.core.vistapublica.EstablecimientoPublico;
import ar.sf.sicat.core.vistapublica.EstablecimientoPublicoRepository;
import ar.sf.sicat.core.vistapublica.HorarioPublico;
import ar.sf.sicat.core.vistapublica.HorarioPublicoRepository;
import ar.sf.sicat.core.vistapublica.OfertaCategoriaPublica;
import ar.sf.sicat.core.vistapublica.OfertaCategoriaPublicaRepository;
import ar.sf.sicat.core.vistapublica.OfertaPublica;
import ar.sf.sicat.core.vistapublica.OfertaPublicaRepository;
import ar.sf.sicat.core.vistapublica.SalonPublicoRepository;
import ar.sf.sicat.core.vistapublica.VisitaGuiadaPublicaRepository;
import ar.sf.sicat.publica.catalogo.CategoriaPublicaService;
import ar.sf.sicat.publica.establecimiento.dto.EstablecimientoResumenDto;
import ar.sf.sicat.publica.establecimiento.dto.FichaDto;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/** Consultas públicas sobre establecimientos. Solo lee las vistas del esquema publico. */
@Service
@Transactional(readOnly = true)
public class EstablecimientoPublicoService {

    public static final int RADIO_MAXIMO_METROS = 20_000;
    public static final int LIMITE_MAXIMO = 100;

    private final EstablecimientoPublicoRepository establecimientos;
    private final OfertaPublicaRepository ofertas;
    private final OfertaCategoriaPublicaRepository categoriasAdicionales;
    private final ContactoPublicoRepository contactos;
    private final CaracteristicaPublicaRepository caracteristicas;
    private final DistincionPublicaRepository distinciones;
    private final HorarioPublicoRepository horarios;
    private final AlojamientoPublicoRepository alojamientos;
    private final CircuitoPublicoRepository circuitos;
    private final CircuitoParadaPublicaRepository paradas;
    private final SalonPublicoRepository salones;
    private final VisitaGuiadaPublicaRepository visitas;
    private final CategoriaPublicaService categorias;
    private final ObjectMapper json;

    public EstablecimientoPublicoService(EstablecimientoPublicoRepository establecimientos,
                                         OfertaPublicaRepository ofertas,
                                         OfertaCategoriaPublicaRepository categoriasAdicionales,
                                         ContactoPublicoRepository contactos,
                                         CaracteristicaPublicaRepository caracteristicas,
                                         DistincionPublicaRepository distinciones, HorarioPublicoRepository horarios,
                                         AlojamientoPublicoRepository alojamientos, CircuitoPublicoRepository circuitos,
                                         CircuitoParadaPublicaRepository paradas, SalonPublicoRepository salones,
                                         VisitaGuiadaPublicaRepository visitas, CategoriaPublicaService categorias,
                                         ObjectMapper json) {
        this.establecimientos = establecimientos;
        this.ofertas = ofertas;
        this.categoriasAdicionales = categoriasAdicionales;
        this.contactos = contactos;
        this.caracteristicas = caracteristicas;
        this.distinciones = distinciones;
        this.horarios = horarios;
        this.alojamientos = alojamientos;
        this.circuitos = circuitos;
        this.paradas = paradas;
        this.salones = salones;
        this.visitas = visitas;
        this.categorias = categorias;
        this.json = json;
    }

    // ------------------------------------------------------------------ listado

    @Cacheable("listado")
    public PaginaDto<EstablecimientoResumenDto> listar(String q, Long categoria, Long localidad, String bbox,
                                                      Pageable pageable) {
        var pagina = establecimientos.findAll(filtro(q, categoria, localidad, bbox), pageable);
        Map<Long, List<String>> cats = categoriasPorEstablecimiento(pagina.map(EstablecimientoPublico::getId).toList());
        return PaginaDto.de(pagina, e -> resumen(e, cats));
    }

    /** Filtros comunes del listado y del mapa. */
    public Specification<EstablecimientoPublico> filtro(String q, Long categoria, Long localidad, String bbox) {
        Bbox caja = Bbox.parse(bbox);
        Set<Long> rama = categoria == null ? null : categorias.conDescendientes(categoria);
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<BusquedaPublica> b = sub.from(BusquedaPublica.class);
                List<Predicate> palabras = new ArrayList<>();
                for (String palabra : BusquedaPublica.normalizar(q).split("\\s+")) {
                    palabras.add(cb.like(b.get("texto"), "%" + palabra + "%"));
                }
                sub.select(b.get("idEstablecimiento")).where(palabras.toArray(Predicate[]::new));
                p.add(root.get("id").in(sub));
            }
            if (rama != null) {
                Subquery<Long> principal = query.subquery(Long.class);
                Root<OfertaPublica> o = principal.from(OfertaPublica.class);
                principal.select(o.get("idEstablecimiento")).where(o.get("idCategoria").in(rama));

                Subquery<Long> ofertasAdicionales = query.subquery(Long.class);
                Root<OfertaCategoriaPublica> oc = ofertasAdicionales.from(OfertaCategoriaPublica.class);
                ofertasAdicionales.select(oc.get("idOferta")).where(oc.get("idCategoria").in(rama));
                Subquery<Long> adicional = query.subquery(Long.class);
                Root<OfertaPublica> o2 = adicional.from(OfertaPublica.class);
                adicional.select(o2.get("idEstablecimiento")).where(o2.get("id").in(ofertasAdicionales));

                p.add(cb.or(root.get("id").in(principal), root.get("id").in(adicional)));
            }
            if (localidad != null) {
                p.add(cb.equal(root.get("idLocalidad"), localidad));
            }
            if (caja != null) {
                p.add(cb.between(root.get("lat"), caja.minLat(), caja.maxLat()));
                p.add(cb.between(root.get("lng"), caja.minLng(), caja.maxLng()));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    // ------------------------------------------------------------------ cercanos

    @Cacheable("cercanos")
    public List<EstablecimientoResumenDto> cercanos(double lat, double lng, int radioMetros, int limite) {
        List<Object[]> filas = establecimientos.cercanos(lat, lng, radioMetros, limite);
        List<Long> ids = filas.stream().map(f -> ((Number) f[0]).longValue()).toList();
        Map<Long, EstablecimientoPublico> porId = establecimientos.findByIdIn(ids).stream()
                .collect(Collectors.toMap(EstablecimientoPublico::getId, Function.identity()));
        Map<Long, List<String>> cats = categoriasPorEstablecimiento(ids);
        return filas.stream()
                .map(f -> resumen(porId.get(((Number) f[0]).longValue()), cats)
                        .conDistancia((int) Math.round(((Number) f[1]).doubleValue())))
                .toList();
    }

    // ------------------------------------------------------------------ ficha

    @Cacheable("ficha")
    public FichaDto ficha(Long id) {
        EstablecimientoPublico e = establecimientos.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Establecimiento", id));

        List<OfertaPublica> listaOfertas = ofertas.findByIdEstablecimiento(id).stream()
                .sorted(Comparator.comparing(OfertaPublica::getId)).toList();
        List<Long> idsOferta = listaOfertas.stream().map(OfertaPublica::getId).toList();

        Map<Long, List<String>> adicionales = categoriasAdicionales.findByIdOfertaIn(idsOferta).stream()
                .collect(Collectors.groupingBy(OfertaCategoriaPublica::getIdOferta,
                        Collectors.mapping(OfertaCategoriaPublica::getCategoria, Collectors.toList())));
        List<HorarioPublico> todosHorarios = idsOferta.isEmpty() ? List.of()
                : horarios.findByIdOfertaInOrderByDiaSemanaAscHoraDesdeAsc(idsOferta);
        Map<Long, List<FichaDto.Horario>> horariosOferta = agrupar(todosHorarios.stream()
                .filter(h -> h.getIdVisita() == null).toList(), HorarioPublico::getIdOferta);
        Map<Long, List<FichaDto.Horario>> horariosVisita = agrupar(todosHorarios.stream()
                .filter(h -> h.getIdVisita() != null).toList(), HorarioPublico::getIdVisita);
        Map<Long, Map<String, Object>> detalles = detalles(idsOferta, horariosVisita);

        List<FichaDto.Oferta> ofertasDto = listaOfertas.stream()
                .map(o -> new FichaDto.Oferta(o.getId(), o.getCategoria(),
                        adicionales.getOrDefault(o.getId(), List.of()).stream().sorted().toList(),
                        o.getAcceso(), o.getPublicoAdmitido(), o.getFrecuencia(),
                        horariosOferta.getOrDefault(o.getId(), List.of()),
                        detalles.getOrDefault(o.getId(), Map.of())))
                .toList();

        return new FichaDto(e.getId(), e.getNombre(), e.getDescripcion(), e.getOferente(), direccion(e), e.getLugar(),
                e.getLocalidad(), e.getProvincia(), coordenadas(e.getLat(), e.getLng()), e.getAnioInauguracion(),
                e.getEstadoFuncionamiento(), e.getMotivoEstado(), e.getFechaReapertura(),
                Boolean.TRUE.equals(e.getDestacado()),
                contactos.findByIdEstablecimientoOrderByOrden(id).stream()
                        .map(c -> new FichaDto.Contacto(c.getTipo(), c.getValor(), c.getUso())).toList(),
                caracteristicas.findByIdEstablecimiento(id).stream()
                        .sorted(Comparator.comparing((ar.sf.sicat.core.vistapublica.CaracteristicaPublica c) -> c.getGrupo())
                                .thenComparing(c -> c.getNombre()))
                        .map(c -> new FichaDto.Caracteristica(c.getGrupo(), c.getNombre(), c.getCantidad())).toList(),
                distinciones.findByIdEstablecimiento(id).stream()
                        .map(d -> new FichaDto.Distincion(d.getNombre(), d.getOrganismoOtorgante(),
                                d.getAnioOtorgamiento(), d.getUrlVerificacion())).toList(),
                ofertasDto);
    }

    /** Detalle de cada oferta: solo las claves de las especializaciones que tiene. */
    private Map<Long, Map<String, Object>> detalles(List<Long> idsOferta, Map<Long, List<FichaDto.Horario>> horariosVisita) {
        Map<Long, Map<String, Object>> d = new LinkedHashMap<>();
        if (idsOferta.isEmpty()) {
            return d;
        }
        alojamientos.findByIdOfertaIn(idsOferta).forEach(a -> d.computeIfAbsent(a.getIdOferta(), k -> new LinkedHashMap<>())
                .put("alojamiento", new FichaDto.Alojamiento(a.getEstrellas(), a.getCantHabitaciones(),
                        a.getTotalPlazas(), a.getHabAccesibles(), a.getPlazasAccesibles())));
        Map<Long, List<FichaDto.Parada>> paradasPorCircuito = paradas.findByIdOfertaInOrderByIdOfertaAscOrdenAsc(idsOferta)
                .stream().collect(Collectors.groupingBy(CircuitoParadaPublica::getIdOferta, LinkedHashMap::new,
                        Collectors.mapping(p -> new FichaDto.Parada(p.getOrden(), p.getIdEstablecimiento(), p.getNombre(),
                                coordenadas(p.getLat(), p.getLng())), Collectors.toList())));
        circuitos.findByIdOfertaIn(idsOferta).forEach(c -> d.computeIfAbsent(c.getIdOferta(), k -> new LinkedHashMap<>())
                .put("circuito", new FichaDto.Circuito(c.getDuracionMin(), c.getLugarPartida(),
                        coordenadas(c.getLatPartida(), c.getLngPartida()), geoJson(c.getTrazadoGeojson()),
                        paradasPorCircuito.getOrDefault(c.getIdOferta(), List.of()))));
        salones.findByIdOfertaIn(idsOferta).stream()
                .sorted(Comparator.comparing(s -> s.getNombre()))
                .forEach(s -> listaEn(d, s.getIdOferta(), "salones").add(new FichaDto.Salon(s.getNombre(),
                        s.getCapAuditorio(), s.getCapBanquete(), s.getCapCoctel(), s.getM2())));
        visitas.findByIdOfertaIn(idsOferta).forEach(v -> listaEn(d, v.getIdOferta(), "visitasGuiadas")
                .add(new FichaDto.VisitaGuiada(v.getModalidad(), Boolean.TRUE.equals(v.getRequiereReserva()),
                        horariosVisita.getOrDefault(v.getId(), List.of()))));
        return d;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> listaEn(Map<Long, Map<String, Object>> d, Long idOferta, String clave) {
        return (List<Object>) d.computeIfAbsent(idOferta, k -> new LinkedHashMap<>())
                .computeIfAbsent(clave, k -> new ArrayList<>());
    }

    private static Map<Long, List<FichaDto.Horario>> agrupar(List<HorarioPublico> lista, Function<HorarioPublico, Long> clave) {
        return lista.stream().collect(Collectors.groupingBy(clave, LinkedHashMap::new,
                Collectors.mapping(h -> new FichaDto.Horario(h.getDiaSemana(), h.getHoraDesde(), h.getHoraHasta()),
                        Collectors.toList())));
    }

    private Object geoJson(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            return json.readTree(texto);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    // ------------------------------------------------------------------ auxiliares

    /** Categorías (principales y adicionales) de cada establecimiento, sin repetir. */
    public Map<Long, List<String>> categoriasPorEstablecimiento(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<OfertaPublica> lista = ofertas.findByIdEstablecimientoIn(ids);
        Map<Long, Long> establecimientoDeOferta = lista.stream()
                .collect(Collectors.toMap(OfertaPublica::getId, OfertaPublica::getIdEstablecimiento));
        Map<Long, Set<String>> r = new LinkedHashMap<>();
        lista.forEach(o -> r.computeIfAbsent(o.getIdEstablecimiento(), k -> new LinkedHashSet<>()).add(o.getCategoria()));
        if (!establecimientoDeOferta.isEmpty()) {
            categoriasAdicionales.findByIdOfertaIn(new ArrayList<>(establecimientoDeOferta.keySet()))
                    .forEach(oc -> r.computeIfAbsent(establecimientoDeOferta.get(oc.getIdOferta()),
                            k -> new LinkedHashSet<>()).add(oc.getCategoria()));
        }
        return r.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, en -> List.copyOf(en.getValue())));
    }

    public EstablecimientoResumenDto resumen(EstablecimientoPublico e, Map<Long, List<String>> cats) {
        return new EstablecimientoResumenDto(e.getId(), e.getNombre(), direccion(e), e.getLocalidad(),
                coordenadas(e.getLat(), e.getLng()), cats.getOrDefault(e.getId(), List.of()),
                Boolean.TRUE.equals(e.getDestacado()), null);
    }

    static String direccion(EstablecimientoPublico e) {
        return Direcciones.formatear(e.getCalle(), e.getAltura(), e.getKm(), e.getCalleCruce(), e.getComplemento());
    }

    static Coordenadas coordenadas(Double lat, Double lng) {
        return lat == null || lng == null ? null : new Coordenadas(lat, lng);
    }
}
