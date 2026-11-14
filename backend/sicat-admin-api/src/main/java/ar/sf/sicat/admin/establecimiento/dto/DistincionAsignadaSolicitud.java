package ar.sf.sicat.admin.establecimiento.dto;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DistincionAsignadaSolicitud(
        @NotNull Long distincionId,
        @Min(1900) @Max(2100) Short anioOtorgamiento,
        @URL @Size(max = 1000) String urlVerificacion) {
}
