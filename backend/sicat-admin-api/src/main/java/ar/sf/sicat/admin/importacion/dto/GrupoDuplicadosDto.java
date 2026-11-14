package ar.sf.sicat.admin.importacion.dto;

import java.util.List;

import ar.sf.sicat.core.dominio.establecimiento.EstadoPublicacion;
import io.swagger.v3.oas.annotations.media.Schema;

/** Establecimientos activos con el mismo nombre normalizado (posibles duplicados a resolver). */
public record GrupoDuplicadosDto(
        @Schema(description = "Nombre normalizado en común") String clave,
        @Schema(description = "true si además comparten dirección (duplicado casi seguro)") boolean mismaDireccion,
        List<Item> establecimientos) {

    public record Item(Long id, String nombre, String direccion, EstadoPublicacion estadoPublicacion) {
    }
}
