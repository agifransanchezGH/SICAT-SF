package ar.sf.sicat.publica.mapa;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(value = "/api/public/v1/mapa", produces = {"application/geo+json", MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "Mapa", description = "GeoJSON para Leaflet")
public class MapaPublicoController {

    private final MapaPublicoService servicio;

    public MapaPublicoController(MapaPublicoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Establecimientos publicados como FeatureCollection de puntos",
            description = "Mismos filtros que el listado: q, categoria, localidad, bbox.")
    public ObjectNode establecimientos(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoria,
            @RequestParam(required = false) Long localidad,
            @Parameter(description = "minLng,minLat,maxLng,maxLat") @RequestParam(required = false) String bbox) {
        return servicio.establecimientos(q, categoria, localidad, bbox);
    }

    @GetMapping("/circuitos")
    @Operation(summary = "Circuitos publicados como FeatureCollection de líneas")
    public ObjectNode circuitos() {
        return servicio.circuitos();
    }
}
