package ar.sf.sicat.core.vistapublica;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** Claves compuestas ({@code @IdClass}) de las vistas públicas. */
public final class ClaveCompuesta {

    private ClaveCompuesta() {
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class OfertaCategoria implements Serializable {
        private Long idOferta;
        private Long idCategoria;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Contacto implements Serializable {
        private Long idCanal;
        private Long idEstablecimiento;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Caracteristica implements Serializable {
        private Long idEstablecimiento;
        private Long idCaracteristica;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Distincion implements Serializable {
        private Long idEstablecimiento;
        private Long idDistincion;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class CircuitoParada implements Serializable {
        private Long idOferta;
        private Short orden;
    }
}
