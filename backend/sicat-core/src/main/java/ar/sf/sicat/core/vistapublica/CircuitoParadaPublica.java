package ar.sf.sicat.core.vistapublica;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Solo lectura. Vista {@code publico.v_circuito_parada_publica} (solo paradas publicadas). */
@Entity
@Immutable
@Table(schema = "publico", name = "v_circuito_parada_publica")
@IdClass(ClaveCompuesta.CircuitoParada.class)
@Getter
@NoArgsConstructor
public class CircuitoParadaPublica {

    @Id
    @Column(name = "id_oferta")
    private Long idOferta;

    @Id
    private Short orden;

    @Column(name = "id_establecimiento")
    private Long idEstablecimiento;

    private String nombre;

    private Double lat;

    private Double lng;
}
