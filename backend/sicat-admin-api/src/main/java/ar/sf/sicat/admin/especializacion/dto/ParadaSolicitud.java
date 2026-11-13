package ar.sf.sicat.admin.especializacion.dto;

import jakarta.validation.constraints.NotNull;

/** Parada de un circuito. El orden es la posición en la lista (1, 2, 3...). */
public record ParadaSolicitud(@NotNull Long establecimientoId) {
}
