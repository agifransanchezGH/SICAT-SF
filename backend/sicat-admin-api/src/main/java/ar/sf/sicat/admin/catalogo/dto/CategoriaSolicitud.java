package ar.sf.sicat.admin.catalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaSolicitud(
        @NotBlank @Size(max = 200) String nombre,
        @Schema(description = "Categoría padre; null para una categoría raíz") Long padreId,
        @Schema(description = "Solo en PUT: true reactiva la categoría") Boolean activo) {
}
