package ar.sf.sicat.admin.seguridad.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshSolicitud(@NotBlank String refreshToken) {
}
