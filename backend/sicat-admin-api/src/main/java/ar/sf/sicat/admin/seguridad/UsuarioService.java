package ar.sf.sicat.admin.seguridad;

import java.util.Locale;

import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.sf.sicat.admin.seguridad.dto.UsuarioCrearSolicitud;
import ar.sf.sicat.admin.seguridad.dto.UsuarioDto;
import ar.sf.sicat.admin.seguridad.dto.UsuarioModificarSolicitud;
import ar.sf.sicat.core.comun.ConflictoException;
import ar.sf.sicat.core.comun.EntidadNoEncontradaException;
import ar.sf.sicat.core.comun.PaginaDto;
import ar.sf.sicat.core.comun.ReglaNegocioException;
import ar.sf.sicat.core.seguridad.Rol;
import ar.sf.sicat.core.seguridad.RolRepository;
import ar.sf.sicat.core.seguridad.Usuario;
import ar.sf.sicat.core.seguridad.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarios, RolRepository roles, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PaginaDto<UsuarioDto> listar(Pageable pageable) {
        return PaginaDto.de(usuarios.findAll(pageable), UsuarioMapper::aDto);
    }

    @Transactional
    public UsuarioDto crear(UsuarioCrearSolicitud s) {
        String email = normalizar(s.email());
        validarEmailLibre(email, null);
        Usuario u = new Usuario();
        u.setNombreCompleto(s.nombreCompleto().trim());
        u.setEmail(email);
        u.setHashPassword(passwordEncoder.encode(s.password()));
        u.setRol(rol(s.rol()));
        u.setActivo(true);
        return UsuarioMapper.aDto(usuarios.save(u));
    }

    @Transactional
    public UsuarioDto modificar(Long id, UsuarioModificarSolicitud s, Long idUsuarioActual) {
        Usuario u = buscar(id);
        String email = normalizar(s.email());
        validarEmailLibre(email, id);
        if (id.equals(idUsuarioActual) && s.rol() != RolSistema.ADMIN) {
            throw new ReglaNegocioException("No podés quitarte el rol ADMIN a vos mismo");
        }
        u.setNombreCompleto(s.nombreCompleto().trim());
        u.setEmail(email);
        u.setRol(rol(s.rol()));
        if (s.password() != null && !s.password().isBlank()) {
            u.setHashPassword(passwordEncoder.encode(s.password()));
        }
        return UsuarioMapper.aDto(u);
    }

    @Transactional
    public UsuarioDto cambiarEstado(Long id, boolean activo, Long idUsuarioActual) {
        if (id.equals(idUsuarioActual) && !activo) {
            throw new ReglaNegocioException("No podés desactivar tu propio usuario");
        }
        Usuario u = buscar(id);
        u.setActivo(activo);
        return UsuarioMapper.aDto(u);
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new EntidadNoEncontradaException("Usuario", id));
    }

    private Rol rol(RolSistema rol) {
        return roles.findByNombre(rol.name())
                .orElseThrow(() -> new IllegalStateException("Falta el rol " + rol + " en seguridad.rol"));
    }

    private void validarEmailLibre(String email, Long idPropio) {
        usuarios.findByEmailIgnoreCase(email)
                .filter(otro -> !otro.getId().equals(idPropio))
                .ifPresent(otro -> {
                    throw new ConflictoException("Ya existe un usuario con el email " + email);
                });
    }

    static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
