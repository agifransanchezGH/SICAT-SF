package ar.sf.sicat.admin.establecimiento.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos comunes. El estado de publicación no se cambia acá (ver /publicar y /despublicar, paso 6). */
public record EstablecimientoSolicitud(
        @NotNull Long oferenteId,
        @Schema(description = "Lugar ya creado en /lugares") Long lugarId,
        @NotBlank @Size(max = 300) String nombre,
        @Size(max = 5000) String descripcion,
        @Min(1500) @Max(2100) Short anioInauguracion,
        @Size(max = 100) String estadoFuncionamiento,
        @Size(max = 500) String motivoEstado,
        LocalDate fechaReapertura,
        @Schema(description = "Por defecto false") Boolean destacado,
        @Schema(description = "Solo en PUT: true reactiva un establecimiento dado de baja") Boolean activo) {
}
