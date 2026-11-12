package ar.sf.sicat.core.dominio.oferta;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import ar.sf.sicat.core.dominio.clasificacion.Categoria;
import ar.sf.sicat.core.dominio.establecimiento.Establecimiento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lo que un establecimiento ofrece, clasificado por una categoría principal y categorías adicionales.
 * Sus datos propios viven en las especializaciones (alojamiento, circuito, salones, visitas guiadas).
 */
@Entity
@Table(schema = "sicat", name = "oferta")
@Getter
@Setter
@NoArgsConstructor
public class Oferta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_oferta")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_establecimiento", nullable = false)
    private Establecimiento establecimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    private String acceso;

    @Column(name = "publico_admitido")
    private String publicoAdmitido;

    private String frecuencia;

    @Column(name = "fecha_relevamiento")
    private LocalDate fechaRelevamiento;

    @Column(nullable = false)
    private Boolean activo = true;

    /** Tabla sicat.oferta_categoria. */
    @ManyToMany
    @JoinTable(schema = "sicat", name = "oferta_categoria",
            joinColumns = @JoinColumn(name = "id_oferta"),
            inverseJoinColumns = @JoinColumn(name = "id_categoria"))
    private Set<Categoria> categoriasAdicionales = new HashSet<>();
}
