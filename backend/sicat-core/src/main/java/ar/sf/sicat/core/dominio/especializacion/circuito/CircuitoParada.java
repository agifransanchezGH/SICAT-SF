package ar.sf.sicat.core.dominio.especializacion.circuito;

import java.io.Serializable;

import ar.sf.sicat.core.dominio.establecimiento.Establecimiento;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Parada ordenada de un circuito en un establecimiento. PK (id_oferta, orden). */
@Entity
@Table(schema = "sicat", name = "circuito_parada")
@Getter
@Setter
@NoArgsConstructor
public class CircuitoParada {

    @EmbeddedId
    private Id id = new Id();

    @MapsId("idOferta")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_oferta")
    private Circuito circuito;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_establecimiento", nullable = false)
    private Establecimiento establecimiento;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {

        @Column(name = "id_oferta")
        private Long idOferta;

        @Column(name = "orden")
        private Short orden;
    }
}
