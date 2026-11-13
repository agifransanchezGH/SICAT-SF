package ar.sf.sicat.admin.especializacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** Los horarios de la visita se cargan aparte en /visitas-guiadas/{id}/horarios. */
public record VisitaGuiadaSolicitud(
        @Size(max = 200) @Schema(example = "Grupal con guía") String modalidad,
        @Schema(description = "Por defecto false") Boolean requiereReserva,
        @Schema(description = "Por defecto true") Boolean habilitada) {
}
