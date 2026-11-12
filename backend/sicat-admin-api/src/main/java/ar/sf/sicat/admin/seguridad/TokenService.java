package ar.sf.sicat.admin.seguridad;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import ar.sf.sicat.admin.config.JwtProperties;
import ar.sf.sicat.admin.seguridad.dto.TokenRespuesta;
import ar.sf.sicat.core.seguridad.Usuario;

/**
 * Emite y valida los JWT de la API Admin (HS256).
 * Access y refresh son JWT sin estado que se distinguen por el claim {@code tipo} (ADR 0005).
 * El refresh no se guarda en la base: al renovar se verifica que el usuario siga activo.
 */
@Service
public class TokenService {

    public static final String EMISOR = "sicat-admin-api";
    public static final String TIPO_ACCESS = "access";
    public static final String TIPO_REFRESH = "refresh";

    private final JwtEncoder encoder;
    private final NimbusJwtDecoder refreshDecoder;
    private final JwtProperties props;

    public TokenService(JwtEncoder encoder, SecretKey jwtKey, JwtProperties props) {
        this.encoder = encoder;
        this.props = props;
        this.refreshDecoder = NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
        this.refreshDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(EMISOR), validadorTipo(TIPO_REFRESH)));
    }

    public TokenRespuesta emitir(Usuario usuario) {
        Instant ahora = Instant.now();
        Duration vidaAccess = Duration.ofMinutes(props.expiracionMinutos());
        String access = firmar(JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(usuario.getId().toString())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(vidaAccess))
                .claim("tipo", TIPO_ACCESS)
                .claim("email", usuario.getEmail())
                .claim("nombre", usuario.getNombreCompleto())
                .claim("roles", List.of(usuario.getRol().getNombre()))
                .build());
        String refresh = firmar(JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(usuario.getId().toString())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(Duration.ofDays(props.refreshExpiracionDias())))
                .claim("tipo", TIPO_REFRESH)
                .build());
        return new TokenRespuesta(access, refresh, "Bearer", vidaAccess.toSeconds());
    }

    /** Devuelve el id de usuario del refresh token, o lanza BadCredentialsException si no es válido. */
    public Long idUsuarioDeRefresh(String refreshToken) {
        try {
            return Long.valueOf(refreshDecoder.decode(refreshToken).getSubject());
        } catch (JwtException | NumberFormatException e) {
            throw new BadCredentialsException("Refresh token inválido o vencido");
        }
    }

    public static Long idUsuario(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    public static OAuth2TokenValidator<Jwt> validadorTipo(String tipo) {
        OAuth2Error error = new OAuth2Error("invalid_token", "Se esperaba un token de tipo " + tipo, null);
        return jwt -> tipo.equals(jwt.getClaimAsString("tipo"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(error);
    }

    private String firmar(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
