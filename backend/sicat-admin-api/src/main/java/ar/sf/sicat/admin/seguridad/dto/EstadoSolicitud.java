package ar.sf.sicat.admin.seguridad.dto;

import jakarta.validation.constraints.NotNull;

public record EstadoSolicitud(@NotNull Boolean activo) {
}
