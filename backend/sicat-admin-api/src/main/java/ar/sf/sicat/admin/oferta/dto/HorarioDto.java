package ar.sf.sicat.admin.oferta.dto;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

public record HorarioDto(
        Long id,
        @Schema(description = "1 = lunes … 7 = domingo") short diaSemana,
        @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "09:00") LocalTime desde,
        @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "18:30") LocalTime hasta) {
}
