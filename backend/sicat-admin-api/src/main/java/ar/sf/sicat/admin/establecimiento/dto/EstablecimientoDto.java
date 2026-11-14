package ar.sf.sicat.admin.establecimiento.dto;

import java.time.LocalDate;
import java.util.List;

import ar.sf.sicat.core.comun.Coordenadas;
import ar.sf.sicat.core.dominio.establecimiento.EstadoPublicacion;

/** Detalle de un establecimiento: datos comunes, oferente, lugar y resumen de sus ofertas. */
public record EstablecimientoDto(
        Long id,
        String nombre,
        String descripcion,
        Short anioInauguracion,
        String estadoFuncionamiento,
        String motivoEstado,
        LocalDate fechaReapertura,
        EstadoPublicacion estadoPublicacion,
        boolean destacado,
        boolean activo,
        Ref oferente,
        LugarRef lugar,
        List<OfertaRef> ofertas) {

    public record Ref(Long id, String nombre) {
    }

    public record LugarRef(Long id, String nombre, String direccion, String localidad, Coordenadas coordenadas) {
    }

    public record OfertaRef(Long id, Long categoriaId, String categoria, boolean activo) {
    }
}
