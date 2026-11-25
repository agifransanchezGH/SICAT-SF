package ar.sf.sicat.admin.mapa;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ar.sf.sicat.admin.establecimiento.EstablecimientoService;
import ar.sf.sicat.core.comun.DatoInvalidoException;
import ar.sf.sicat.core.comun.GeoJson;
import ar.sf.sicat.core.dominio.especializacion.circuito.Circuito;
import ar.sf.sicat.core.dominio.especializacion.circuito.CircuitoParadaRepository;
import ar.sf.sicat.core.dominio.especializacion.circuito.CircuitoRepository;
import ar.sf.sicat.core.dominio.establecimiento.EstadoPublicacion;
import ar.sf.sicat.core.dominio.establecimiento.Establecimiento;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoRepository;
import ar.sf.sicat.core.dominio.lugar.Lugar;
import ar.sf.sicat.core.dominio.oferta.Oferta;
import ar.sf.sicat.core.dominio.oferta.OfertaRepository;

/** GeoJSON (FeatureCollection) para el mapa del panel: incluye borradores y ocultos, no solo lo publicado. */
@Service
@Transactional(readOnly = true)
public class MapaService {

    public static final int RADIO_MAXIMO_METROS = 20_000;
    public static final int LIMITE_MAXIMO = 200;

    private final EstablecimientoRepository establecimientos;
    private final EstablecimientoService servicio;
    private final CircuitoRepository circuitos;
    private final CircuitoParadaRepository paradas;
    private final ObjectMapper json;
    private final OfertaRepository ofertas;

    public MapaService(EstablecimientoRepository establecimientos, EstablecimientoService servicio,
                       CircuitoRepository circuitos, CircuitoParadaRepository paradas, ObjectMapper json,
                       OfertaRepository ofertas) {
        this.ofertas = ofertas;
        this.establecimientos = establecimientos;
        this.servicio = servicio;
        this.circuitos = circuitos;
        this.paradas = paradas;
        this.json = json;
    }

    /**
     * Un Point por establecimiento activo con coordenadas. Mismos filtros que el listado.
     * Propiedades: id, nombre, estadoPublicacion, estadoGeo, lugarId, destacado y categorias (ids de las categorías
     * de sus ofertas activas, principal y adicionales; para contar por categoría en el mapa).
     */
    public ObjectNode establecimientos(String q, Long categoria, EstadoPublicacion estado, Long localidadId, String bbox) {
        List<Establecimiento> lista = establecimientos.findAll(
                servicio.filtro(q, categoria, estado, null, localidadId, bbox, null, false, false, false), Sort.by("nombre"))
                .stream().filter(e -> e.getLugar() != null && e.getLugar().getPunto() != null).toList();
        // Categorías de todos los puntos en una consulta (sin N+1).
        Map<Long, Set<Long>> categoriasDe = lista.isEmpty() ? Map.of()
                : ofertas.conCategoriasDe(lista.stream().map(Establecimiento::getId).toList()).stream()
                        .filter(o -> Boolean.TRUE.equals(o.getActivo()))
                        .collect(Collectors.groupingBy(o -> o.getEstablecimiento().getId(),
                                Collectors.flatMapping(MapaService::idsDeCategorias, Collectors.toCollection(TreeSet::new))));
        ObjectNode coleccion = coleccion();
        ArrayNode features = coleccion.putArray("features");
        for (Establecimiento e : lista) {
            ObjectNode props = punto(features, e, e.getLugar(), null);
            ArrayNode cats = props.putArray("categorias");
            categoriasDe.getOrDefault(e.getId(), Set.of()).forEach(cats::add);
        }
        return coleccion;
    }

    private static Stream<Long> idsDeCategorias(Oferta o) {
        return Stream.concat(Stream.of(o.getCategoria().getId()), o.getCategoriasAdicionales().stream().map(c -> c.getId()));
    }

    /** Los más cercanos al punto, ordenados por distancia; agrega la propiedad distanciaMetros. */
    public ObjectNode cercanos(double lat, double lng, int radioMetros, int limite) {
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new DatoInvalidoException("Coordenadas fuera de rango");
        }
        if (radioMetros < 1 || radioMetros > RADIO_MAXIMO_METROS || limite < 1 || limite > LIMITE_MAXIMO) {
            throw new DatoInvalidoException("radioMetros debe estar entre 1 y " + RADIO_MAXIMO_METROS
                    + " y limite entre 1 y " + LIMITE_MAXIMO);
        }
        ObjectNode coleccion = coleccion();
        ArrayNode features = coleccion.putArray("features");
        establecimientos.buscarCercanos(lat, lng, radioMetros).stream().limit(limite).forEach(e -> {
            Point p = e.getLugar().getPunto();
            punto(features, e, e.getLugar(), (int) Math.round(distanciaMetros(lat, lng, p.getY(), p.getX())));
        });
        return coleccion;
    }

    /**
     * Un LineString por circuito con trazado, de ofertas y establecimientos activos.
     * Propiedades: id (oferta), establecimientoId, establecimiento, estadoPublicacion, duracionMin, lugarPartida, paradas.
     */
    public ObjectNode circuitos() {
        ObjectNode coleccion = coleccion();
        ArrayNode features = coleccion.putArray("features");
        for (Circuito c : circuitos.findAll(Sort.by("id"))) {
            Establecimiento e = c.getOferta().getEstablecimiento();
            if (c.getTrazado() == null || !Boolean.TRUE.equals(c.getOferta().getActivo())
                    || !Boolean.TRUE.equals(e.getActivo())) {
                continue;
            }
            ObjectNode f = feature(features, c.getId());
            f.set("geometry", GeoJson.aGeoJson(c.getTrazado()));
            ObjectNode props = f.putObject("properties");
            props.put("id", c.getId());
            props.put("establecimientoId", e.getId());
            props.put("establecimiento", e.getNombre());
            props.put("estadoPublicacion", e.getEstadoPublicacion().name());
            if (c.getDuracionMin() != null) {
                props.put("duracionMin", c.getDuracionMin());
            }
            if (c.getLugarPartida() != null) {
                props.put("lugarPartida", c.getLugarPartida().getNombre());
            }
            ArrayNode ps = props.putArray("paradas");
            paradas.findByCircuitoIdOrderByIdOrden(c.getId()).forEach(p -> {
                ObjectNode pn = ps.addObject();
                pn.put("orden", p.getId().getOrden());
                pn.put("establecimientoId", p.getEstablecimiento().getId());
                pn.put("nombre", p.getEstablecimiento().getNombre());
            });
        }
        return coleccion;
    }

    private ObjectNode punto(ArrayNode features, Establecimiento e, Lugar l, Integer distancia) {
        ObjectNode f = feature(features, e.getId());
        ObjectNode geom = f.putObject("geometry");
        geom.put("type", "Point");
        geom.putArray("coordinates").add(l.getPunto().getX()).add(l.getPunto().getY());
        ObjectNode props = f.putObject("properties");
        props.put("id", e.getId());
        props.put("nombre", e.getNombre());
        props.put("estadoPublicacion", e.getEstadoPublicacion().name());
        props.put("lugarId", l.getId());
        if (l.getEstadoGeo() != null) {
            props.put("estadoGeo", l.getEstadoGeo());
        }
        props.put("destacado", Boolean.TRUE.equals(e.getDestacado()));
        if (distancia != null) {
            props.put("distanciaMetros", distancia);
        }
        return props;
    }

    private ObjectNode coleccion() {
        ObjectNode c = json.createObjectNode();
        c.put("type", "FeatureCollection");
        return c;
    }

    private static ObjectNode feature(ArrayNode features, Long id) {
        ObjectNode f = features.addObject();
        f.put("type", "Feature");
        f.put("id", id);
        return f;
    }

    /** Haversine; alcanza para mostrar la distancia (el filtro por radio lo hace PostGIS). */
    static double distanciaMetros(double lat1, double lng1, double lat2, double lng2) {
        double r = 6_371_008.8;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }
}
