package ar.sf.sicat.admin.comun.lote;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Resultado de una acción en lote: cada elemento se procesa en su propia transacción. */
public record ResultadoLoteDto(
        int procesados,
        int exitosos,
        int fallidos,
        @Schema(description = "Un resultado por id, en el orden pedido") List<Item> resultados) {

    /** Resultado de un elemento. */
    public record Item(
            Long id,
            @Schema(description = "Nombre del elemento, o null si no existe") String nombre,
            boolean ok,
            @Schema(description = "Motivos del rechazo (vacío si ok)") List<String> errores) {
    }

    public static ResultadoLoteDto de(List<Item> resultados) {
        int ok = (int) resultados.stream().filter(Item::ok).count();
        return new ResultadoLoteDto(resultados.size(), ok, resultados.size() - ok, resultados);
    }
}
