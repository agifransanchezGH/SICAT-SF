package ar.sf.sicat.admin.oferta.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import ar.sf.sicat.admin.especializacion.dto.AlojamientoDto;
import ar.sf.sicat.admin.especializacion.dto.CircuitoDto;
import ar.sf.sicat.admin.especializacion.dto.SalonDto;
import ar.sf.sicat.admin.especializacion.dto.VisitaGuiadaDto;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Oferta. En el listado de un establecimiento vienen los datos propios y las categorías;
 * en el detalle (GET /ofertas/{id}) además horarios y especializaciones.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OfertaDto(
        Long id,
        Long establecimientoId,
        String establecimiento,
        Long categoriaId,
        String categoria,
        List<CategoriaRef> categoriasAdicionales,
        String acceso,
        String publicoAdmitido,
        String frecuencia,
        LocalDate fechaRelevamiento,
        boolean activo,
        @Schema(description = "Solo en el detalle") List<HorarioDto> horarios,
        @Schema(description = "Solo en el detalle, si tiene") AlojamientoDto alojamiento,
        @Schema(description = "Solo en el detalle, si tiene") CircuitoDto circuito,
        @Schema(description = "Solo en el detalle") List<SalonDto> salones,
        @Schema(description = "Solo en el detalle") List<VisitaGuiadaDto> visitasGuiadas) {

    public record CategoriaRef(Long id, String nombre) {
    }
}
