package ar.sf.sicat.admin.oferente.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OferenteSolicitud(
        @NotBlank @Size(max = 300) String nombre,
        @NotNull Long tipoOferenteId,
        @Schema(description = "Solo en PUT: true reactiva un oferente dado de baja") Boolean activo) {
}
