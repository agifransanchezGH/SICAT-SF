package ar.sf.sicat.admin.comun;

/** Característica asignada a un establecimiento o a un salón, con su cantidad si corresponde. */
public record CaracteristicaAsignadaDto(Long caracteristicaId, String grupo, String nombre, Integer cantidad) {
}
