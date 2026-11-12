package ar.sf.sicat.admin.seguridad.dto;

import java.time.OffsetDateTime;

import ar.sf.sicat.admin.seguridad.RolSistema;

/** Usuario tal como se expone por HTTP (sin hash de contraseña). */
public record UsuarioDto(
        Long id,
        String nombreCompleto,
        String email,
        RolSistema rol,
        boolean activo,
        OffsetDateTime ultimoAcceso) {
}
