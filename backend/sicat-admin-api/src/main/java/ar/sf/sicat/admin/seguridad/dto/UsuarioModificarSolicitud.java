package ar.sf.sicat.admin.seguridad.dto;

import ar.sf.sicat.admin.seguridad.RolSistema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code password} es opcional: si viene, se reemplaza la contraseña. */
public record UsuarioModificarSolicitud(
        @NotBlank @Size(max = 200) String nombreCompleto,
        @NotBlank @Email @Size(max = 200) String email,
        @Size(min = 8, max = 72) String password,
        @NotNull RolSistema rol) {
}
