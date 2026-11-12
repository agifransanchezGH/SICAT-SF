package ar.sf.sicat.core.dominio.establecimiento;

import java.io.Serializable;

import ar.sf.sicat.core.dominio.clasificacion.Caracteristica;
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

@Entity
@Table(schema = "sicat", name = "establecimiento_caracteristica")
@Getter
@Setter
@NoArgsConstructor
public class EstablecimientoCaracteristica {

    @EmbeddedId
    private Id id = new Id();

    @MapsId("idEstablecimiento")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_establecimiento")
    private Establecimiento establecimiento;

    @MapsId("idCaracteristica")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_caracteristica")
    private Caracteristica caracteristica;

    /** Solo si la característica admite cantidad. */
    private Integer cantidad;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {

        @Column(name = "id_establecimiento")
        private Long idEstablecimiento;

        @Column(name = "id_caracteristica")
        private Long idCaracteristica;
    }
}
