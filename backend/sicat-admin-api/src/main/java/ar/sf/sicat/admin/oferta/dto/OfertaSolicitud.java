package ar.sf.sicat.admin.oferta.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record OfertaSolicitud(
        @NotNull @Schema(description = "Categoría principal") Long categoriaId,
        @Size(max = 200) @Schema(example = "Libre y gratuito") String acceso,
        @Size(max = 200) @Schema(example = "Todo público") String publicoAdmitido,
        @Size(max = 200) @Schema(example = "Todo el año") String frecuencia,
        @PastOrPresent LocalDate fechaRelevamiento,
        @Schema(description = "Solo en PUT: true reactiva una oferta dada de baja") Boolean activo) {
}
