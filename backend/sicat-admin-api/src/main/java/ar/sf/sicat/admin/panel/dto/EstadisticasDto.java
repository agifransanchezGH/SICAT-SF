package ar.sf.sicat.admin.panel.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Cifras del tablero de inicio del panel. Cada una coincide con lo que muestra el listado filtrado correspondiente. */
public record EstadisticasDto(
        Establecimientos establecimientos,
        @Schema(description = "Establecimientos por categoría raíz (con sus subcategorías), de mayor a menor")
        List<Categoria> categorias,
        @Schema(description = "Oferentes activos") long oferentes,
        @Schema(description = "Ofertas activas de establecimientos activos") long ofertasActivas,
        @Schema(description = "Circuitos (ofertas activas con datos de circuito)") long circuitos,
        Pendientes pendientes) {

    @Schema(description = "Establecimientos activos y su estado de publicación")
    public record Establecimientos(long activos, long borradores, long publicados, long ocultos) {
    }

    @Schema(description = "Establecimientos activos con una oferta activa en la categoría o en sus subcategorías "
            + "(principal o adicional); mismo criterio que el filtro categoria del listado")
    public record Categoria(Long id, String nombre, long establecimientos, List<Categoria> subcategorias) {
    }

    @Schema(description = "Trabajo pendiente; mismos números que los contadores del menú")
    public record Pendientes(
            @Schema(description = "Lugares en la cola \"A revisar\": ubicación AUTOMATICO, APROXIMADO, NO_ENCONTRADO o PENDIENTE")
            long ubicacionesARevisar,
            @Schema(description = "Grupos de posibles duplicados sin resolver") long posiblesDuplicados,
            @Schema(description = "BORRADOR u OCULTO que cumplen los requisitos de publicación") long listosParaPublicar,
            @Schema(description = "Establecimientos con contactos a revisar") long contactosARevisar) {
    }
}
