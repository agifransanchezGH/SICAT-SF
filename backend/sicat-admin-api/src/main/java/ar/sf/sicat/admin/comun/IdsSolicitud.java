package ar.sf.sicat.admin.comun;

import java.util.List;

import jakarta.validation.constraints.NotNull;

/** Cuerpo de los PUT que reemplazan una lista de referencias: {@code { "ids": [1, 2, 3] }}. */
public record IdsSolicitud(@NotNull List<@NotNull Long> ids) {
}
