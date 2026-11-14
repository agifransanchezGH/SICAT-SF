package ar.sf.sicat.admin.comun;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CaracteristicaAsignadaSolicitud(
        @NotNull Long caracteristicaId,
        @PositiveOrZero @Schema(description = "Solo si la característica admite cantidad") Integer cantidad) {
}
