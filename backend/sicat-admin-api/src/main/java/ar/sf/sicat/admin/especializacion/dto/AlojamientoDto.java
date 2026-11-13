package ar.sf.sicat.admin.especializacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

/** Datos de alojamiento de una oferta (1 a 1). Se usa igual para leer y para el PUT. */
public record AlojamientoDto(
        @Min(1) @Max(5) @Schema(description = "1 a 5; null si no está categorizado") Short estrellas,
        @PositiveOrZero Integer cantHabitaciones,
        @PositiveOrZero Integer totalPlazas,
        @PositiveOrZero Integer habAccesibles,
        @PositiveOrZero Integer plazasAccesibles,
        @PositiveOrZero Integer personalPermanente,
        @PositiveOrZero Integer personalNoPermanente) {
}
