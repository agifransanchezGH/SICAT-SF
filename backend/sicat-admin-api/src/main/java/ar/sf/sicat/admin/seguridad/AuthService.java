package ar.sf.sicat.admin.seguridad;

import java.time.OffsetDateTime;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.sf.sicat.admin.seguridad.dto.SesionRespuesta;
import ar.sf.sicat.admin.seguridad.dto.UsuarioDto;
import ar.sf.sicat.core.comun.EntidadNoEncontradaException;
import ar.sf.sicat.core.seguridad.Usuario;
import ar.sf.sicat.core.seguridad.UsuarioRepository;

@Service
public class AuthService {

    private static final String CREDENCIALES_INVALIDAS = "Email o contraseña incorrectos";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    /** Hash descartable para que un email inexistente tarde lo mismo que una contraseña errónea. */
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, TokenService tokens) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.hashFicticio = passwordEncoder.encode("sicat-hash-ficticio");
    }

    @Transactional
    public SesionRespuesta login(String email, String password) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(email.trim()).orElse(null);
        String hash = usuario != null ? usuario.getHashPassword() : hashFicticio;
        boolean coincide = passwordEncoder.matches(password, hash);
        if (usuario == null || !coincide || !Boolean.TRUE.equals(usuario.getActivo())) {
            throw new BadCredentialsException(CREDENCIALES_INVALIDAS);
        }
        usuario.setUltimoAcceso(OffsetDateTime.now());
        return sesion(usuario);
    }

    @Transactional(readOnly = true)
    public SesionRespuesta renovar(String refreshToken) {
        Long id = tokens.idUsuarioDeRefresh(refreshToken);
        Usuario usuario = usuarios.findById(id)
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .orElseThrow(() -> new BadCredentialsException("El usuario no existe o está inactivo"));
        return sesion(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioDto actual(Long idUsuario) {
        return usuarios.findById(idUsuario)
                .map(UsuarioMapper::aDto)
                .orElseThrow(() -> new EntidadNoEncontradaException("Usuario", idUsuario));
    }

    private SesionRespuesta sesion(Usuario usuario) {
        return SesionRespuesta.de(tokens.emitir(usuario), UsuarioMapper.aDto(usuario));
    }
}
