package ar.sf.sicat.publica.establecimiento.dto;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import ar.sf.sicat.core.comun.Coordenadas;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EstablecimientoResumenDto(
        Long id,
        String nombre,
        String direccion,
        String localidad,
        Coordenadas coordenadas,
        @Schema(description = "Categorías de sus ofertas (principales y adicionales)") List<String> categorias,
        boolean destacado,
        @Schema(description = "Solo en /cercanos") Integer distanciaMetros) implements Serializable {

    public EstablecimientoResumenDto conDistancia(int metros) {
        return new EstablecimientoResumenDto(id, nombre, direccion, localidad, coordenadas, categorias, destacado, metros);
    }
}
