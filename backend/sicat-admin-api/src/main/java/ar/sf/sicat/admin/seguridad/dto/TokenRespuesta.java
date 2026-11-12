package ar.sf.sicat.admin.seguridad.dto;

/** Par de tokens emitido por TokenService. {@code expiraEn} en segundos (vida del access token). */
public record TokenRespuesta(String accessToken, String refreshToken, String tipo, long expiraEn) {
}
