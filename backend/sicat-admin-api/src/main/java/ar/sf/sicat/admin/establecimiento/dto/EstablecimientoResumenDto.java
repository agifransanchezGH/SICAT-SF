package ar.sf.sicat.admin.establecimiento.dto;

import java.util.List;

import ar.sf.sicat.core.comun.Coordenadas;
import ar.sf.sicat.core.dominio.establecimiento.EstadoPublicacion;

/** Fila del listado de establecimientos. */
public record EstablecimientoResumenDto(
        Long id,
        String nombre,
        Long oferenteId,
        String oferente,
        String localidad,
        Coordenadas coordenadas,
        EstadoPublicacion estadoPublicacion,
        boolean destacado,
        boolean activo,
        List<String> categorias) {
}
