package ar.sf.sicat.admin.oferente.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import ar.sf.sicat.core.dominio.establecimiento.EstadoPublicacion;
import io.swagger.v3.oas.annotations.media.Schema;

/** Oferente. {@code establecimientos} solo viene en el detalle (GET /oferentes/{id}). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OferenteDto(
        Long id,
        String nombre,
        Long tipoOferenteId,
        String tipoOferente,
        boolean activo,
        @Schema(description = "Solo en el detalle") List<EstablecimientoRef> establecimientos) {

    public record EstablecimientoRef(Long id, String nombre, EstadoPublicacion estadoPublicacion, boolean activo) {
    }
}
