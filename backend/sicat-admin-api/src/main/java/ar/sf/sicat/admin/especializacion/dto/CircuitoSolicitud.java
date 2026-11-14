package ar.sf.sicat.admin.especializacion.dto;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

public record CircuitoSolicitud(
        @Schema(description = "Lugar desde donde parte") Long lugarPartidaId,
        @Positive @Schema(description = "Duración en minutos") Integer duracionMin,
        @Schema(description = "GeoJSON LineString con el recorrido",
                example = "{\"type\":\"LineString\",\"coordinates\":[[-60.70,-31.63],[-60.69,-31.64]]}")
        JsonNode trazado) {
}
