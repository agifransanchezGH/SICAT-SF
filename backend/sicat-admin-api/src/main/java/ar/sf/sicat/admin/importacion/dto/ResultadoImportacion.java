package ar.sf.sicat.admin.importacion.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/** Informe de /validar (simulación, no guarda) y de /ejecutar (guarda los NUEVO). */
public record ResultadoImportacion(
        String archivo,
        boolean ejecutado,
        @Schema(description = "Establecimientos leídos (en salones, varias filas pueden ser un mismo establecimiento)")
        int total,
        int nuevos,
        int duplicadosEnBase,
        int duplicadosEnArchivo,
        int conErrores,
        @Schema(description = "Solo en /ejecutar") int creados,
        @Schema(description = "Hojas del archivo que el perfil no reconoce (no se importan)") List<String> hojasIgnoradas,
        List<Item> items) {

    public enum Estado { NUEVO, DUPLICADO_EN_BASE, DUPLICADO_EN_ARCHIVO, ERROR }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Item(
            String hoja,
            int fila,
            String nombre,
            String categoria,
            String direccion,
            Estado estado,
            @Schema(description = "Si es duplicado de uno existente") Long establecimientoExistenteId,
            @Schema(description = "Solo en /ejecutar") Long establecimientoCreadoId,
            int contactos,
            int caracteristicas,
            List<String> errores,
            List<String> advertencias) {
    }
}
