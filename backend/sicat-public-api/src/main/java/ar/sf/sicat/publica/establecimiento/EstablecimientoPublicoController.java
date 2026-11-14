package ar.sf.sicat.publica.establecimiento;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.sf.sicat.core.comun.PaginaDto;
import ar.sf.sicat.publica.establecimiento.dto.EstablecimientoResumenDto;
import ar.sf.sicat.publica.establecimiento.dto.FichaDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/public/v1/establecimientos")
@Tag(name = "Establecimientos", description = "Solo establecimientos publicados")
public class EstablecimientoPublicoController {

    private final EstablecimientoPublicoService servicio;

    public EstablecimientoPublicoController(EstablecimientoPublicoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Listado paginado",
            description = "q busca en nombre y categorías (sin distinguir mayúsculas ni acentos); categoria incluye subcategorías.")
    public PaginaDto<EstablecimientoResumenDto> listar(
            @Parameter(example = "parrilla") @RequestParam(required = false) String q,
            @Parameter(description = "Id de categoría") @RequestParam(required = false) Long categoria,
            @Parameter(description = "Id de localidad") @RequestParam(required = false) Long localidad,
            @Parameter(description = "minLng,minLat,maxLng,maxLat") @RequestParam(required = false) String bbox,
            @ParameterObject @PageableDefault(size = 20)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "destacado", direction = Sort.Direction.DESC),
                    @SortDefault(sort = "nombre")}) Pageable pageable) {
        return servicio.listar(q, categoria, localidad, bbox, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ficha", description = "Bloque común + ofertas con horarios y detalle. 404 si no está publicado.")
    public FichaDto ficha(@PathVariable Long id) {
        return servicio.ficha(id);
    }

    @GetMapping("/cercanos")
    @Operation(summary = "Cercanos a un punto", description = "Del más cercano al más lejano, con distanciaMetros.")
    public List<EstablecimientoResumenDto> cercanos(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double lng,
            @RequestParam(defaultValue = "1000") @Min(1) @Max(EstablecimientoPublicoService.RADIO_MAXIMO_METROS) int radioMetros,
            @RequestParam(defaultValue = "20") @Min(1) @Max(EstablecimientoPublicoService.LIMITE_MAXIMO) int limite) {
        return servicio.cercanos(lat, lng, radioMetros, limite);
    }
}
