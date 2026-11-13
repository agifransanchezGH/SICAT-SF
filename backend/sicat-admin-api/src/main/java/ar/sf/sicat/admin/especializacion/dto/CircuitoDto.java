package ar.sf.sicat.admin.especializacion.dto;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

public record CircuitoDto(
        Long lugarPartidaId,
        String lugarPartida,
        Integer duracionMin,
        @Schema(description = "GeoJSON LineString") JsonNode trazado) {
}
