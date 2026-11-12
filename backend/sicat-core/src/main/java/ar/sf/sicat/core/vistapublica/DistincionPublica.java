package ar.sf.sicat.core.vistapublica;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Solo lectura. Vista {@code publico.v_distincion_publica}. */
@Entity
@Immutable
@Table(schema = "publico", name = "v_distincion_publica")
@IdClass(ClaveCompuesta.Distincion.class)
@Getter
@NoArgsConstructor
public class DistincionPublica {

    @Id
    @Column(name = "id_establecimiento")
    private Long idEstablecimiento;

    @Id
    @Column(name = "id_distincion")
    private Long idDistincion;

    private String nombre;

    @Column(name = "organismo_otorgante")
    private String organismoOtorgante;

    @Column(name = "anio_otorgamiento")
    private Short anioOtorgamiento;

    @Column(name = "url_verificacion")
    private String urlVerificacion;
}
