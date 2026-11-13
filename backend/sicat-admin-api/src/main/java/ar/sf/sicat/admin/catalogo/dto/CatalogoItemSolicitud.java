package ar.sf.sicat.admin.catalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Alta o modificación de un ítem de catálogo. {@code provincia} es obligatoria en localidad y
 * {@code grupo} en caracteristica. {@code activo} permite reactivar un ítem dado de baja.
 */
public record CatalogoItemSolicitud(
        @NotBlank @Size(max = 200) String nombre,
        @Size(max = 200) @Schema(description = "Obligatoria en localidad") String provincia,
        @Size(max = 200) @Schema(description = "Solo distincion") String organismoOtorgante,
        @Size(max = 200) @Schema(description = "Obligatorio en caracteristica") String grupo,
        @Schema(description = "Solo caracteristica (por defecto false)") Boolean admiteCantidad,
        @Schema(description = "Solo en PUT: true reactiva el ítem") Boolean activo) {
}
