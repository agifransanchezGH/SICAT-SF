package ar.sf.sicat.admin.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Configuración del JWT (HS256). {@code sicat.jwt.*} en application.yml. */
@Validated
@ConfigurationProperties(prefix = "sicat.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32) String secret,
        @Min(1) long expiracionMinutos) {
}
