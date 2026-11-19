package ar.sf.sicat.admin.importacion.dto;

import java.util.List;

import ar.sf.sicat.core.dominio.establecimiento.EstadoPublicacion;
import io.swagger.v3.oas.annotations.media.Schema;

/** Establecimientos activos con el mismo nombre normalizado (posibles duplicados a resolver), listos para comparar. */
public record GrupoDuplicadosDto(
        @Schema(description = "Nombre normalizado en común") String clave,
        @Schema(description = "true si además comparten dirección (duplicado casi seguro)") boolean mismaDireccion,
        List<Item> establecimientos) {

    public record Item(
            Long id,
            String nombre,
            String direccion,
            EstadoPublicacion estadoPublicacion,
            @Schema(description = "Nombre del oferente") String oferente,
            @Schema(description = "Categorías de sus ofertas activas (principal y adicionales)") List<String> categorias,
            @Schema(description = "Cantidad de ofertas activas") int ofertasActivas,
            @Schema(description = "Contactos del establecimiento, en orden") List<Contacto> contactos) {
    }

    public record Contacto(String tipo, String valor, boolean esPublico) {
    }
}
