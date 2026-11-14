package ar.sf.sicat.core.vistapublica;

import java.text.Normalizer;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Solo lectura. Vista {@code publico.v_busqueda_publica} (V9): nombre y categorías de cada establecimiento
 * publicado, en minúsculas y sin acentos.
 */
@Entity
@Immutable
@Table(schema = "publico", name = "v_busqueda_publica")
@Getter
@NoArgsConstructor
public class BusquedaPublica {

    @Id
    @Column(name = "id_establecimiento")
    private Long idEstablecimiento;

    private String texto;

    /** Misma normalización que la vista: minúsculas y sin acentos ni diéresis (ñ → n). */
    public static String normalizar(String s) {
        String sinMarcas = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinMarcas.toLowerCase().trim();
    }
}
