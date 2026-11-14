package ar.sf.sicat.admin.catalogo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/** Ítem de catálogo. Los campos propios de cada catálogo solo aparecen en ese catálogo. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CatalogoItemDto(
        Long id,
        String nombre,
        Boolean activo,
        @Schema(description = "Solo localidad") String provincia,
        @Schema(description = "Solo distincion") String organismoOtorgante,
        @Schema(description = "Solo caracteristica") String grupo,
        @Schema(description = "Solo caracteristica") Boolean admiteCantidad) {

    public static CatalogoItemDto simple(Long id, String nombre, Boolean activo) {
        return new CatalogoItemDto(id, nombre, activo, null, null, null, null);
    }
}
