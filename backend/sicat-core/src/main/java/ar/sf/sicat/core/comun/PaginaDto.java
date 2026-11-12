package ar.sf.sicat.core.comun;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Respuesta paginada estándar de ambas APIs:
 * {@code { contenido, pagina, tamanio, totalElementos, totalPaginas }}.
 */
public record PaginaDto<T>(
        List<T> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas) {

    public static <E, T> PaginaDto<T> de(Page<E> page, Function<E, T> mapper) {
        return new PaginaDto<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
