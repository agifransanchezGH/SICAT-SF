package ar.sf.sicat.core.vistapublica;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Solo lectura. Vista {@code publico.v_oferta_categoria_publica}. */
@Entity
@Immutable
@Table(schema = "publico", name = "v_oferta_categoria_publica")
@IdClass(ClaveCompuesta.OfertaCategoria.class)
@Getter
@NoArgsConstructor
public class OfertaCategoriaPublica {

    @Id
    @Column(name = "id_oferta")
    private Long idOferta;

    @Id
    @Column(name = "id_categoria")
    private Long idCategoria;

    private String categoria;
}
