package ar.sf.sicat.core.dominio.establecimiento;

import java.io.Serializable;

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
@Table(schema = "sicat", name = "establecimiento_distincion")
@Getter
@Setter
@NoArgsConstructor
public class EstablecimientoDistincion {

    @EmbeddedId
    private Id id = new Id();

    @MapsId("idEstablecimiento")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_establecimiento")
    private Establecimiento establecimiento;

    @MapsId("idDistincion")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_distincion")
    private Distincion distincion;

    @Column(name = "anio_otorgamiento")
    private Short anioOtorgamiento;

    @Column(name = "url_verificacion")
    private String urlVerificacion;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {

        @Column(name = "id_establecimiento")
        private Long idEstablecimiento;

        @Column(name = "id_distincion")
        private Long idDistincion;
    }
}
