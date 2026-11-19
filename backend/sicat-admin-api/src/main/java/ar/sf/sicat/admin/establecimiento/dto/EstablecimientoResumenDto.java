package ar.sf.sicat.admin.establecimiento.dto;

import java.util.List;

import ar.sf.sicat.admin.publicacion.dto.FaltanteDto;
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
        @io.swagger.v3.oas.annotations.media.Schema(description = "Estado de geocodificación del lugar (null si no tiene lugar o se cargó a mano)")
        String estadoGeo,
        EstadoPublicacion estadoPublicacion,
        boolean destacado,
        boolean activo,
        List<String> categorias,
        @io.swagger.v3.oas.annotations.media.Schema(description = "Requisitos de publicación que no cumple (vacío si se puede publicar)")
        List<FaltanteDto> faltantes) {
}
