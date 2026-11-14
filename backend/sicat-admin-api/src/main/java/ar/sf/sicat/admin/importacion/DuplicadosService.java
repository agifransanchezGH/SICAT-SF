package ar.sf.sicat.admin.importacion;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.sf.sicat.admin.importacion.dto.GrupoDuplicadosDto;
import ar.sf.sicat.admin.importacion.lectura.Texto;
import ar.sf.sicat.core.dominio.establecimiento.Establecimiento;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoRepository;

@Service
public class DuplicadosService {

    private final EstablecimientoRepository establecimientos;

    public DuplicadosService(EstablecimientoRepository establecimientos) {
        this.establecimientos = establecimientos;
    }

    /** Grupos de establecimientos activos con el mismo nombre normalizado; primero los que comparten dirección. */
    @Transactional(readOnly = true)
    public List<GrupoDuplicadosDto> buscar() {
        Map<String, List<Establecimiento>> porNombre = establecimientos.findAll().stream()
                .filter(e -> Boolean.TRUE.equals(e.getActivo()))
                .collect(Collectors.groupingBy(e -> Texto.clave(e.getNombre()), TreeMap::new, Collectors.toList()));
        return porNombre.entrySet().stream()
                .filter(en -> en.getValue().size() > 1)
                .map(en -> {
                    List<GrupoDuplicadosDto.Item> items = en.getValue().stream()
                            .sorted(Comparator.comparing(Establecimiento::getId))
                            .map(e -> new GrupoDuplicadosDto.Item(e.getId(), e.getNombre(),
                                    Duplicados.direccion(e.getLugar()), e.getEstadoPublicacion()))
                            .toList();
                    long direcciones = items.stream().map(i -> Texto.clave(i.direccion())).distinct().count();
                    return new GrupoDuplicadosDto(en.getKey(), direcciones == 1, items);
                })
                .sorted(Comparator.comparing(GrupoDuplicadosDto::mismaDireccion).reversed()
                        .thenComparing(GrupoDuplicadosDto::clave))
                .toList();
    }
}
