package ar.sf.sicat.core.dominio.establecimiento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persona de contacto interna del establecimiento. No es pública. */
@Entity
@Table(schema = "sicat", name = "referente")
@Getter
@Setter
@NoArgsConstructor
public class Referente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_referente")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_establecimiento", nullable = false)
    private Establecimiento establecimiento;

    @Column(nullable = false)
    private String nombre;

    private String cargo;

    private String telefono;

    private String correo;

    @Column(nullable = false)
    private Boolean activo = true;
}
