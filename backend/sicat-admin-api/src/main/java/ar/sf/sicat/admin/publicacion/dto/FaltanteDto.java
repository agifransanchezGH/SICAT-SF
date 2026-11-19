package ar.sf.sicat.admin.publicacion.dto;

import ar.sf.sicat.admin.publicacion.Requisito;
import io.swagger.v3.oas.annotations.media.Schema;

/** Requisito de publicación que no se cumple. */
public record FaltanteDto(
        @Schema(description = "Requisito incumplido") Requisito codigo,
        @Schema(description = "Texto corto para listados, p. ej. \"una oferta activa\"") String texto) {

    public static FaltanteDto de(Requisito r) {
        return new FaltanteDto(r, r.corto());
    }
}
