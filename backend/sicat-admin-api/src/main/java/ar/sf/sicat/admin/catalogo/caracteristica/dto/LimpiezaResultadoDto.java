package ar.sf.sicat.admin.catalogo.caracteristica.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Qué cambió con una fusión, una separación o un paso a notas internas. */
public record LimpiezaResultadoDto(
        @Schema(description = "Las características que quedan (la conservada o las partes)") List<CaracteristicaUsoDto> resultantes,
        @Schema(description = "Asignaciones a establecimientos movidas o creadas") int establecimientos,
        @Schema(description = "Asignaciones a salones movidas o creadas") int salones,
        @Schema(description = "Categorías actualizadas") int categorias,
        @Schema(description = "Características borradas") int eliminadas) {
}
