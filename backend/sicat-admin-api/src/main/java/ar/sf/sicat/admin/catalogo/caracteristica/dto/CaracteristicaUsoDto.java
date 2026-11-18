package ar.sf.sicat.admin.catalogo.caracteristica.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Una característica del catálogo con cuántos la usan, para decidir fusiones. */
public record CaracteristicaUsoDto(
        Long id,
        String grupo,
        String nombre,
        boolean admiteCantidad,
        boolean esPublica,
        boolean activo,
        @Schema(description = "Establecimientos que la tienen asignada") long establecimientos,
        @Schema(description = "Salones que la tienen asignada") long salones,
        @Schema(description = "Categorías a las que aplica") long categorias) {
}
