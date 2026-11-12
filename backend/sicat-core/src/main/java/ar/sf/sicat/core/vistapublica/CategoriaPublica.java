package ar.sf.sicat.core.vistapublica;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Solo lectura. Vista {@code publico.v_categoria_publica}. */
@Entity
@Immutable
@Table(schema = "publico", name = "v_categoria_publica")
@Getter
@NoArgsConstructor
public class CategoriaPublica {

    @Id
    @Column(name = "id_categoria")
    private Long id;

    @Column(name = "id_categoria_padre")
    private Long idCategoriaPadre;

    private String nombre;
}
