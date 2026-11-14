package ar.sf.sicat.publica.catalogo.dto;

import java.util.List;

public record CategoriaNodoDto(Long id, String nombre, List<CategoriaNodoDto> hijos) {
}
