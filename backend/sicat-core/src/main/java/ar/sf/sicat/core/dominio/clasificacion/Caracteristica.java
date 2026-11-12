package ar.sf.sicat.core.dominio.clasificacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Catálogo de características agrupadas (p. ej. grupo "Servicios", nombre "Wi-Fi"). */
@Entity
@Table(schema = "sicat", name = "caracteristica")
@Getter
@Setter
@NoArgsConstructor
public class Caracteristica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caracteristica")
    private Long id;

    @Column(nullable = false)
    private String grupo;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "admite_cantidad", nullable = false)
    private Boolean admiteCantidad = false;

    @Column(nullable = false)
    private Boolean activo = true;
}
