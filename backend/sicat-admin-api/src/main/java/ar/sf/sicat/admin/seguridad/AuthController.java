package ar.sf.sicat.admin.seguridad;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.sf.sicat.admin.seguridad.dto.LoginSolicitud;
import ar.sf.sicat.admin.seguridad.dto.RefreshSolicitud;
import ar.sf.sicat.admin.seguridad.dto.SesionRespuesta;
import ar.sf.sicat.admin.seguridad.dto.UsuarioDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/v1/auth")
@Tag(name = "Autenticación")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Inicia sesión", description = "Devuelve access token, refresh token y el usuario. 401 si las credenciales no son válidas.")
    public SesionRespuesta login(@Valid @RequestBody LoginSolicitud solicitud) {
        return auth.login(solicitud.email(), solicitud.password());
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Renueva la sesión", description = "Recibe el refresh token y devuelve un par de tokens nuevo. 401 si venció o el usuario está inactivo.")
    public SesionRespuesta refresh(@Valid @RequestBody RefreshSolicitud solicitud) {
        return auth.renovar(solicitud.refreshToken());
    }

    @GetMapping("/me")
    @Operation(summary = "Usuario y rol actuales")
    public UsuarioDto me(@AuthenticationPrincipal Jwt jwt) {
        return auth.actual(TokenService.idUsuario(jwt));
    }
}
