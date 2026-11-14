package ar.sf.sicat.admin.catalogo.dto;

import java.util.List;

/** Nodo del árbol de categorías. */
public record CategoriaNodoDto(Long id, String nombre, Long padreId, boolean activo, List<CategoriaNodoDto> hijos) {
}
