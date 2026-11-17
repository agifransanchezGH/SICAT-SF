package ar.sf.sicat.admin.lugar.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import ar.sf.sicat.core.comun.Coordenadas;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LugarDto(
        Long id,
        String nombre,
        Long padreId,
        String padre,
        Long tipoLugarId,
        String tipoLugar,
        Long localidadId,
        String localidad,
        String calle,
        Integer altura,
        BigDecimal km,
        String calleCruce,
        String complemento,
        Coordenadas coordenadas,
        @Schema(description = "Geometría GeoJSON (polígono, línea, etc.)") JsonNode geometria,
        String estadoGeo,
        @Schema(description = "Solo en el detalle") List<LugarRef> hijos,
        @Schema(description = "Establecimientos ubicados en este lugar (activos y dados de baja)")
        List<EstablecimientoRef> establecimientos) {

    public record LugarRef(Long id, String nombre) {
    }

    public record EstablecimientoRef(Long id, String nombre,
                                     ar.sf.sicat.core.dominio.establecimiento.EstadoPublicacion estadoPublicacion,
                                     boolean activo) {
    }
}
