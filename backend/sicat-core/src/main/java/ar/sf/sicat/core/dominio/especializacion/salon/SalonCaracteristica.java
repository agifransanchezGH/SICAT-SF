package ar.sf.sicat.core.dominio.especializacion.salon;

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
@Table(schema = "sicat", name = "salon_caracteristica")
@Getter
@Setter
@NoArgsConstructor
public class SalonCaracteristica {

    @EmbeddedId
    private Id id = new Id();

    @MapsId("idSalon")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_salon")
    private Salon salon;

    @MapsId("idCaracteristica")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_caracteristica")
    private Caracteristica caracteristica;

    private Integer cantidad;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {

        @Column(name = "id_salon")
        private Long idSalon;

        @Column(name = "id_caracteristica")
        private Long idCaracteristica;
    }
}
