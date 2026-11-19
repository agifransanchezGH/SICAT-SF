package ar.sf.sicat.admin.importacion.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Resultado de "Quedarme con este". */
public record ResolucionResultadoDto(
        @Schema(description = "Ids dados de baja") List<Long> bajas,
        @Schema(description = "Contactos copiados al que queda") int contactosCopiados,
        @Schema(description = "Características agregadas al que queda") int caracteristicasCopiadas) {
}
