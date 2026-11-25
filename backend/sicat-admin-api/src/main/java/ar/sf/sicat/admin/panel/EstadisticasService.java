package ar.sf.sicat.admin.panel;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.sf.sicat.admin.panel.dto.ContadoresDto;
import ar.sf.sicat.admin.panel.dto.EstadisticasDto;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoRepository;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoRepository.ConteoCategoria;

/**
 * Tablero de inicio: todo sale de consultas agregadas (no se cargan registros). Los pendientes son los mismos
 * contadores del menú, para que el tablero y el menú nunca digan números distintos.
 */
@Service
public class EstadisticasService {

    private final EstablecimientoRepository establecimientos;
    private final ContadoresService contadores;

    public EstadisticasService(EstablecimientoRepository establecimientos, ContadoresService contadores) {
        this.establecimientos = establecimientos;
        this.contadores = contadores;
    }

    @Transactional(readOnly = true)
    public EstadisticasDto estadisticas() {
        EstablecimientoRepository.ResumenEstadisticas r = establecimientos.resumenEstadisticas();
        ContadoresDto c = contadores.contadores();
        return new EstadisticasDto(
                new EstadisticasDto.Establecimientos(r.getActivos(), r.getBorradores(), r.getPublicados(), r.getOcultos()),
                categorias(establecimientos.establecimientosPorCategoria()),
                r.getOferentes(), r.getOfertasActivas(), r.getCircuitos(),
                new EstadisticasDto.Pendientes(c.ubicacionesARevisar(), c.posiblesDuplicados(), c.listosParaPublicar(),
                        c.contactosARevisar()));
    }

    /** Raíces con sus subcategorías directas (cada una ya cuenta su propia rama), de mayor a menor. */
    static List<EstadisticasDto.Categoria> categorias(List<ConteoCategoria> filas) {
        Map<Long, List<ConteoCategoria>> hijas = filas.stream().filter(f -> f.getPadreId() != null)
                .collect(Collectors.groupingBy(ConteoCategoria::getPadreId));
        Comparator<EstadisticasDto.Categoria> orden = Comparator
                .comparingLong(EstadisticasDto.Categoria::establecimientos).reversed()
                .thenComparing(EstadisticasDto.Categoria::nombre, String.CASE_INSENSITIVE_ORDER);
        return filas.stream().filter(f -> f.getPadreId() == null)
                .map(raiz -> new EstadisticasDto.Categoria(raiz.getId(), raiz.getNombre(), raiz.getEstablecimientos(),
                        hijas.getOrDefault(raiz.getId(), List.of()).stream()
                                .map(h -> new EstadisticasDto.Categoria(h.getId(), h.getNombre(), h.getEstablecimientos(), List.of()))
                                .sorted(orden).toList()))
                .sorted(orden)
                .toList();
    }
}
