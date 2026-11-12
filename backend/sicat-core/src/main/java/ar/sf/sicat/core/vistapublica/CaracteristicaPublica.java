package ar.sf.sicat.core.vistapublica;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Solo lectura. Vista {@code publico.v_caracteristica_publica}. */
@Entity
@Immutable
@Table(schema = "publico", name = "v_caracteristica_publica")
@IdClass(ClaveCompuesta.Caracteristica.class)
@Getter
@NoArgsConstructor
public class CaracteristicaPublica {

    @Id
    @Column(name = "id_establecimiento")
    private Long idEstablecimiento;

    @Id
    @Column(name = "id_caracteristica")
    private Long idCaracteristica;

    private String grupo;

    private String nombre;

    private Integer cantidad;
}
