package ar.sf.sicat.admin.mapa;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ar.sf.sicat.admin.comun.Roles;
import ar.sf.sicat.core.dominio.establecimiento.EstadoPublicacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(value = "/api/admin/v1/mapa", produces = {"application/geo+json", MediaType.APPLICATION_JSON_VALUE})
@Tag(name = "Mapa", description = "GeoJSON interno para Leaflet")
public class MapaController {

    private final MapaService servicio;

    public MapaController(MapaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/establecimientos")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Establecimientos activos como FeatureCollection de puntos",
            description = "Incluye borradores y ocultos. Filtros: q, categoria (con subcategorías), estado, localidadId, bbox.")
    public ObjectNode establecimientos(
            @RequestParam(required = false) String q,
            @Parameter(description = "Id de categoría") @RequestParam(required = false) Long categoria,
            @RequestParam(required = false) EstadoPublicacion estado,
            @RequestParam(required = false) Long localidadId,
            @Parameter(description = "minLng,minLat,maxLng,maxLat") @RequestParam(required = false) String bbox) {
        return servicio.establecimientos(q, categoria, estado, localidadId, bbox);
    }

    @GetMapping("/establecimientos/cercanos")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Establecimientos cercanos a un punto",
            description = "Ordenados por distancia; propiedad distanciaMetros. radioMetros hasta 20000, limite hasta 200.")
    public ObjectNode cercanos(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "1000") int radioMetros,
            @RequestParam(defaultValue = "50") int limite) {
        return servicio.cercanos(lat, lng, radioMetros, limite);
    }

    @GetMapping("/circuitos")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Trazados de circuitos como FeatureCollection de líneas", description = "Incluye borradores.")
    public ObjectNode circuitos() {
        return servicio.circuitos();
    }
}
