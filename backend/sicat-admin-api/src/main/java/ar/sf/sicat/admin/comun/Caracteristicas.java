package ar.sf.sicat.admin.comun;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ar.sf.sicat.core.comun.ReglaNegocioException;
import ar.sf.sicat.core.dominio.clasificacion.Caracteristica;

/** Reglas comunes de las características asignadas (a establecimientos y a salones). */
public final class Caracteristicas {

    private Caracteristicas() {
    }

    /** La cantidad solo se admite si la característica lo permite. */
    public static void validarCantidad(Caracteristica c, Integer cantidad) {
        if (cantidad != null && !Boolean.TRUE.equals(c.getAdmiteCantidad())) {
            throw new ReglaNegocioException("La característica '" + c.getNombre() + "' no admite cantidad");
        }
    }

    public static void validarSinRepetidos(List<CaracteristicaAsignadaSolicitud> lista) {
        Set<Long> vistos = new HashSet<>();
        for (CaracteristicaAsignadaSolicitud s : lista) {
            if (!vistos.add(s.caracteristicaId())) {
                throw new ReglaNegocioException("La característica " + s.caracteristicaId() + " está repetida");
            }
        }
    }

    public static CaracteristicaAsignadaDto dto(Caracteristica c, Integer cantidad) {
        return new CaracteristicaAsignadaDto(c.getId(), c.getGrupo(), c.getNombre(), cantidad);
    }
}
