package ar.sf.sicat.admin.seguridad;

import ar.sf.sicat.admin.seguridad.dto.UsuarioDto;
import ar.sf.sicat.core.seguridad.Usuario;

final class UsuarioMapper {

    private UsuarioMapper() {
    }

    static UsuarioDto aDto(Usuario u) {
        return new UsuarioDto(
                u.getId(),
                u.getNombreCompleto(),
                u.getEmail(),
                RolSistema.valueOf(u.getRol().getNombre()),
                Boolean.TRUE.equals(u.getActivo()),
                u.getUltimoAcceso());
    }
}
