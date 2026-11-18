package ar.sf.sicat.admin.catalogo.caracteristica.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Fusionar características del mismo grupo en una sola. */
public record FusionSolicitud(
        @Schema(description = "La característica que queda", example = "133") @NotNull Long conservarId,
        @Schema(description = "Las que se absorben y se borran", example = "[244]") @NotEmpty List<@NotNull Long> absorberIds,
        @Schema(description = "Nuevo nombre de la que queda (opcional)", example = "Grifo monocomando")
        @Size(max = 200) String nombre) {
}
