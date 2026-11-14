package ar.sf.sicat.publica.mapa;

import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ar.sf.sicat.core.vistapublica.CircuitoParadaPublicaRepository;
import ar.sf.sicat.core.vistapublica.CircuitoPublico;
import ar.sf.sicat.core.vistapublica.CircuitoPublicoRepository;
import ar.sf.sicat.core.vistapublica.EstablecimientoPublico;
import ar.sf.sicat.core.vistapublica.EstablecimientoPublicoRepository;
import ar.sf.sicat.core.vistapublica.OfertaPublica;
import ar.sf.sicat.core.vistapublica.OfertaPublicaRepository;
import ar.sf.sicat.publica.establecimiento.EstablecimientoPublicoService;

/** GeoJSON (FeatureCollection) para el mapa público (Leaflet). */
@Service
@Transactional(readOnly = true)
public class MapaPublicoService {

    private final EstablecimientoPublicoRepository establecimientos;
    private final EstablecimientoPublicoService servicio;
    private final CircuitoPublicoRepository circuitos;
    private final CircuitoParadaPublicaRepository paradas;
    private final OfertaPublicaRepository ofertas;
    private final ObjectMapper json;

    public MapaPublicoService(EstablecimientoPublicoRepository establecimientos, EstablecimientoPublicoService servicio,
                              CircuitoPublicoRepository circuitos, CircuitoParadaPublicaRepository paradas,
                              OfertaPublicaRepository ofertas, ObjectMapper json) {
        this.establecimientos = establecimientos;
        this.servicio = servicio;
        this.circuitos = circuitos;
        this.paradas = paradas;
        this.ofertas = ofertas;
        this.json = json;
    }

    /** Un Point por establecimiento publicado con coordenadas. Propiedades: id, nombre, categorias, destacado. */
    @Cacheable("mapa")
    public ObjectNode establecimientos(String q, Long categoria, Long localidad, String bbox) {
        List<EstablecimientoPublico> lista = establecimientos.findAll(
                servicio.filtro(q, categoria, localidad, bbox), Sort.by("nombre")).stream()
                .filter(e -> e.getLat() != null && e.getLng() != null)
                .toList();
        Map<Long, List<String>> cats = servicio.categoriasPorEstablecimiento(
                lista.stream().map(EstablecimientoPublico::getId).toList());
        ObjectNode coleccion = coleccion();
        ArrayNode features = coleccion.putArray("features");
        for (EstablecimientoPublico e : lista) {
            ObjectNode f = feature(features, e.getId());
            ObjectNode geom = f.putObject("geometry");
            geom.put("type", "Point");
            geom.putArray("coordinates").add(e.getLng()).add(e.getLat());
            ObjectNode props = f.putObject("properties");
            props.put("id", e.getId());
            props.put("nombre", e.getNombre());
            props.put("destacado", Boolean.TRUE.equals(e.getDestacado()));
            ArrayNode c = props.putArray("categorias");
            cats.getOrDefault(e.getId(), List.of()).forEach(c::add);
        }
        return coleccion;
    }

    /** Un LineString por circuito publicado con trazado. Propiedades: id (oferta), establecimiento, duración, paradas. */
    @Cacheable("circuitos")
    public ObjectNode circuitos() {
        List<CircuitoPublico> lista = circuitos.findAll(Sort.by("idOferta")).stream()
                .filter(c -> c.getTrazadoGeojson() != null).toList();
        Map<Long, OfertaPublica> ofertaPorId = new java.util.HashMap<>();
        if (!lista.isEmpty()) {
            List<Long> ids = lista.stream().map(CircuitoPublico::getIdOferta).toList();
            ofertas.findByIdEstablecimientoIn(establecimientosDe(ids)).forEach(o -> ofertaPorId.put(o.getId(), o));
        }
        ObjectNode coleccion = coleccion();
        ArrayNode features = coleccion.putArray("features");
        for (CircuitoPublico c : lista) {
            ObjectNode f = feature(features, c.getIdOferta());
            f.set("geometry", leer(c.getTrazadoGeojson()));
            ObjectNode props = f.putObject("properties");
            props.put("id", c.getIdOferta());
            OfertaPublica o = ofertaPorId.get(c.getIdOferta());
            if (o != null) {
                props.put("establecimientoId", o.getIdEstablecimiento());
                props.put("categoria", o.getCategoria());
            }
            if (c.getDuracionMin() != null) {
                props.put("duracionMin", c.getDuracionMin());
            }
            if (c.getLugarPartida() != null) {
                props.put("lugarPartida", c.getLugarPartida());
            }
            ArrayNode ps = props.putArray("paradas");
            paradas.findByIdOfertaOrderByOrden(c.getIdOferta()).forEach(p -> {
                ObjectNode pn = ps.addObject();
                pn.put("orden", p.getOrden());
                pn.put("establecimientoId", p.getIdEstablecimiento());
                pn.put("nombre", p.getNombre());
            });
        }
        return coleccion;
    }

    private List<Long> establecimientosDe(List<Long> idsOferta) {
        return idsOferta.stream()
                .map(id -> ofertas.findById(id).map(OfertaPublica::getIdEstablecimiento).orElse(null))
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
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

    private ObjectNode leer(String texto) {
        try {
            return (ObjectNode) json.readTree(texto);
        } catch (JsonProcessingException | ClassCastException e) {
            return null;
        }
    }
}
