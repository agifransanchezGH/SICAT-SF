package ar.sf.sicat.core.comun;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** Coordenadas WGS84 tal como viajan en JSON: {@code { "lat": -31.63, "lng": -60.70 }}. */
public record Coordenadas(
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double lng) {
}
