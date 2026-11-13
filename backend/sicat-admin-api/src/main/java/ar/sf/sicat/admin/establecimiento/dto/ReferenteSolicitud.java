package ar.sf.sicat.admin.establecimiento.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Con {@code id} se modifica ese referente; sin {@code id} se crea; los que no vienen se eliminan. */
public record ReferenteSolicitud(
        @Schema(description = "Id de un referente existente; null para uno nuevo") Long id,
        @NotBlank @Size(max = 200) String nombre,
        @Size(max = 200) String cargo,
        @Size(max = 100) String telefono,
        @Email @Size(max = 200) String correo,
        @Schema(description = "Por defecto true") Boolean activo) {
}
