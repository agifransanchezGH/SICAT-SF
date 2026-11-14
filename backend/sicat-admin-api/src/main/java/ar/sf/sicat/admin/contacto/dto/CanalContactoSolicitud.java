package ar.sf.sicat.admin.contacto.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Un canal dentro de un PUT de contactos. Con {@code id} se modifica ese canal; sin {@code id} se crea.
 * Los canales existentes que no vengan en la lista se eliminan.
 */
public record CanalContactoSolicitud(
        @Schema(description = "Id de un canal existente; null para uno nuevo") Long id,
        @NotNull Long tipoCanalId,
        @NotBlank @Size(max = 500) String valor,
        @Size(max = 200) String uso,
        @Schema(description = "Solo los públicos salen al sitio público (por defecto false)") Boolean esPublico,
        @Schema(description = "Por defecto, la posición en la lista") Short orden) {
}
