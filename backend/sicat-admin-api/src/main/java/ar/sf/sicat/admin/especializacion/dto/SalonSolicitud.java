package ar.sf.sicat.admin.especializacion.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SalonSolicitud(
        @NotBlank @Size(max = 200) String nombre,
        @PositiveOrZero @Schema(description = "Capacidad en formato auditorio") Integer capAuditorio,
        @PositiveOrZero Integer capBanquete,
        @PositiveOrZero Integer capCoctel,
        @DecimalMin("0") BigDecimal m2,
        @Size(max = 100) String estadoConservacion,
        @Size(max = 100) String estadoFuncionamiento,
        @Schema(description = "Solo en PUT: true reactiva un salón dado de baja") Boolean activo) {
}
