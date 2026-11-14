package ar.sf.sicat.admin.importacion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import ar.sf.sicat.admin.importacion.lectura.Texto;
import ar.sf.sicat.core.comun.Direcciones;
import ar.sf.sicat.core.dominio.establecimiento.Establecimiento;
import ar.sf.sicat.core.dominio.lugar.Lugar;

/**
 * Índice de establecimientos para detectar duplicados: mismo nombre normalizado y, si ambos tienen
 * dirección, misma dirección normalizada (sucursales de una cadena con distinta dirección no son duplicados).
 * La dirección de un lugar se compara en sus dos formas: el texto original (nombre del lugar importado)
 * y la armada con calle y altura.
 */
final class Duplicados {

    private record Entrada(Long id, Set<String> direcciones) {
    }

    private final Map<String, List<Entrada>> porNombre = new HashMap<>();

    Duplicados(List<Establecimiento> existentes) {
        existentes.forEach(e -> porNombre.computeIfAbsent(Texto.clave(e.getNombre()), k -> new ArrayList<>())
                .add(new Entrada(e.getId(), claves(e.getLugar()))));
    }

    /** Id del establecimiento que coincide. */
    Optional<Long> buscar(String nombre, String direccion) {
        String dir = Texto.clave(direccion);
        return porNombre.getOrDefault(Texto.clave(nombre), List.of()).stream()
                .filter(e -> e.direcciones().isEmpty() || dir.isEmpty() || e.direcciones().contains(dir))
                .map(Entrada::id)
                .findFirst();
    }

    private static Set<String> claves(Lugar l) {
        Set<String> r = new LinkedHashSet<>();
        if (l != null) {
            r.add(Texto.clave(l.getNombre()));
            String armada = Direcciones.formatear(l.getCalle(), l.getAltura(), l.getKm(), l.getCalleCruce(), null);
            if (armada != null) {
                r.add(Texto.clave(armada));
            }
            r.remove("");
        }
        return r;
    }

    /** Dirección legible de un lugar para mostrar (la armada si es más precisa que el nombre). */
    static String direccion(Lugar l) {
        if (l == null) {
            return null;
        }
        String armada = Direcciones.formatear(l.getCalle(), l.getAltura(), l.getKm(), l.getCalleCruce(), null);
        return armada != null && !Texto.clave(l.getNombre()).startsWith(Texto.clave(armada)) ? armada : l.getNombre();
    }
}
