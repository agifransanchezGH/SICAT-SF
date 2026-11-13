package ar.sf.sicat.admin.oferta.dto;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Una franja horaria. Con {@code id} se modifica; sin {@code id} se crea; las que no vienen se eliminan.
 * Si {@code hasta} es menor que {@code desde} la franja cruza la medianoche (p. ej. 20:00 a 02:00).
 */
public record HorarioSolicitud(
        @Schema(description = "Id de una franja existente; null para una nueva") Long id,
        @NotNull @Min(1) @Max(7) @Schema(description = "1 = lunes … 7 = domingo") Short diaSemana,
        @NotNull @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "09:00") LocalTime desde,
        @NotNull @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "18:30") LocalTime hasta) {
}
