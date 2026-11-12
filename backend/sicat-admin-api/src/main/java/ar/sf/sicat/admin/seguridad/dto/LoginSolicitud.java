package ar.sf.sicat.admin.seguridad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginSolicitud(
        @NotBlank @Email String email,
        @NotBlank String password) {
}
