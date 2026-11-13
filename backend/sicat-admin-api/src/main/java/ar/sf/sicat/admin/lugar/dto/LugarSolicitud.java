package ar.sf.sicat.admin.lugar.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.JsonNode;

import ar.sf.sicat.core.comun.Coordenadas;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record LugarSolicitud(
        @NotBlank @Size(max = 300) String nombre,
        @Schema(description = "Lugar que contiene a este (p. ej. un parque dentro de un barrio)") Long padreId,
        Long tipoLugarId,
        Long localidadId,
        @Size(max = 200) String calle,
        @PositiveOrZero Integer altura,
        @DecimalMin("0") @Digits(integer = 5, fraction = 1) BigDecimal km,
        @Size(max = 200) String calleCruce,
        @Size(max = 100) String complemento,
        @Valid @Schema(description = "Ubicación puntual { lat, lng }") Coordenadas coordenadas,
        @Schema(description = "Geometría GeoJSON opcional (Polygon, LineString, ...)") JsonNode geometria,
        @Size(max = 100) String estadoGeo) {
}
