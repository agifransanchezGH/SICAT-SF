package ar.sf.sicat.admin.especializacion.dto;

import ar.sf.sicat.core.comun.Coordenadas;
import io.swagger.v3.oas.annotations.media.Schema;

public record ParadaDto(
        short orden,
        Long establecimientoId,
        String establecimiento,
        @Schema(description = "Coordenadas del lugar del establecimiento, para marcar la parada en el mapa (null si no tiene)")
        Coordenadas coordenadas) {
}
