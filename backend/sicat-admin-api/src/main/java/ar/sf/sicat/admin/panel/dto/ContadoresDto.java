package ar.sf.sicat.admin.panel.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Pendientes de trabajo que el menú del panel muestra junto a cada sección. */
public record ContadoresDto(
        @Schema(description = "Lugares con estadoGeo AUTOMATICO, APROXIMADO, NO_ENCONTRADO o PENDIENTE")
        long ubicacionesARevisar,
        @Schema(description = "Establecimientos activos en BORRADOR u OCULTO que cumplen los requisitos de publicación")
        long listosParaPublicar,
        @Schema(description = "Grupos de establecimientos activos con el mismo nombre normalizado")
        long posiblesDuplicados,
        @Schema(description = "Establecimientos activos con un contacto de Instagram, Facebook o web a revisar")
        long contactosARevisar) {
}
