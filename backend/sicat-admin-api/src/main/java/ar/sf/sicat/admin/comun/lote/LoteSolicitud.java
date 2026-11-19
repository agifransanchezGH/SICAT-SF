package ar.sf.sicat.admin.comun.lote;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cuerpo de las acciones en lote: los ids a procesar (los repetidos se procesan una vez). */
public record LoteSolicitud(
        @Schema(description = "Entre 1 y " + LoteSolicitud.MAXIMO + " ids")
        @NotEmpty @Size(max = LoteSolicitud.MAXIMO) List<@NotNull Long> ids) {

    public static final int MAXIMO = 500;
}
