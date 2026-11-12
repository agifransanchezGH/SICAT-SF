package ar.sf.sicat.admin.seguridad.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Respuesta de /auth/login y /auth/refresh. */
public record SesionRespuesta(
        @Schema(description = "JWT para el header Authorization: Bearer") String accessToken,
        @Schema(description = "JWT para renovar la sesión en /auth/refresh") String refreshToken,
        @Schema(example = "Bearer") String tipo,
        @Schema(description = "Segundos de vida del access token", example = "3600") long expiraEn,
        UsuarioDto usuario) {

    public static SesionRespuesta de(TokenRespuesta token, UsuarioDto usuario) {
        return new SesionRespuesta(token.accessToken(), token.refreshToken(), token.tipo(), token.expiraEn(), usuario);
    }
}
