package ar.sf.sicat.admin.publicacion.dto;

import java.util.List;

import ar.sf.sicat.core.dominio.establecimiento.EstadoPublicacion;
import io.swagger.v3.oas.annotations.media.Schema;

/** Estado de publicación y lista de lo que falta para poder publicar (para mostrar un checklist). */
public record EstadoPublicacionDto(
        EstadoPublicacion estado,
        boolean puedePublicarse,
        @Schema(description = "Vacía si cumple todos los requisitos") List<String> faltantes) {
}
