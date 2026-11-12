package ar.sf.sicat.core.vistapublica;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Solo lectura. Vista {@code publico.v_oferta_publica}. */
@Entity
@Immutable
@Table(schema = "publico", name = "v_oferta_publica")
@Getter
@NoArgsConstructor
public class OfertaPublica {

    @Id
    @Column(name = "id_oferta")
    private Long id;

    @Column(name = "id_establecimiento")
    private Long idEstablecimiento;

    @Column(name = "id_categoria")
    private Long idCategoria;

    private String categoria;

    private String acceso;

    @Column(name = "publico_admitido")
    private String publicoAdmitido;

    private String frecuencia;
}
