package ar.sf.sicat.admin.importacion.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Qué se le copiaría al que queda si se elige "Quedarme con este": los contactos y características de los demás
 * que el que queda no tiene, sin repetir. Agrupado por establecimiento de origen.
 */
public record PreviaResolucionDto(
        Long conservarId,
        @Schema(description = "Total de contactos que se copiarían") int contactos,
        @Schema(description = "Total de características que se agregarían") int caracteristicas,
        List<Origen> origenes) {

    public record Origen(Long id, String nombre, List<GrupoDuplicadosDto.Contacto> contactos,
                         List<Caracteristica> caracteristicas) {
    }

    public record Caracteristica(String grupo, String nombre, Integer cantidad) {
    }
}
