package ar.sf.sicat.core.dominio.establecimiento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Grupo de posibles duplicados marcado como "no son duplicados". Vale mientras el grupo tenga exactamente los
 * mismos integrantes; si cambian, el grupo vuelve a aparecer.
 */
@Entity
@Table(schema = "sicat", name = "descarte_duplicado")
@Getter
@Setter
@NoArgsConstructor
public class DescarteDuplicado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_descarte_duplicado")
    private Long id;

    /** Nombre normalizado del grupo. */
    @Column(nullable = false)
    private String clave;

    /** Ids de los integrantes, ordenados y separados por coma ("12,40,371"). */
    @Column(nullable = false)
    private String integrantes;

    public DescarteDuplicado(String clave, String integrantes) {
        this.clave = clave;
        this.integrantes = integrantes;
    }
}
