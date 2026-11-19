package ar.sf.sicat.admin.importacion.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Un grupo de posibles duplicados tal como se vio en pantalla: su clave y sus integrantes. Si el grupo cambió
 * mientras tanto (alguien dio de baja o creó uno con el mismo nombre), la acción responde 409.
 */
public record GrupoSolicitud(
        @Schema(description = "Clave del grupo (GET /establecimientos/duplicados)") @NotBlank String clave,
        @Schema(description = "Ids de todos los integrantes que se vieron") @NotNull @Size(min = 2, max = 50)
        List<@NotNull Long> integrantes) {
}
