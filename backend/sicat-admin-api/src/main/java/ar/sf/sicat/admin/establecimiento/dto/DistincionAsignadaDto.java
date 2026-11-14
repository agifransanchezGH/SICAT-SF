package ar.sf.sicat.admin.establecimiento.dto;

public record DistincionAsignadaDto(
        Long distincionId,
        String nombre,
        String organismoOtorgante,
        Short anioOtorgamiento,
        String urlVerificacion) {
}
