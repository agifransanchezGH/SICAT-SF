package ar.sf.sicat.core.vistapublica;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Solo lectura. Vista {@code publico.v_contacto_publico}: canales con es_publico = true. */
@Entity
@Immutable
@Table(schema = "publico", name = "v_contacto_publico")
@IdClass(ClaveCompuesta.Contacto.class)
@Getter
@NoArgsConstructor
public class ContactoPublico {

    @Id
    @Column(name = "id_canal")
    private Long idCanal;

    @Id
    @Column(name = "id_establecimiento")
    private Long idEstablecimiento;

    private String tipo;

    private String valor;

    private String uso;

    private Short orden;

    /** ESTABLECIMIENTO u OFERENTE. */
    private String origen;
}
