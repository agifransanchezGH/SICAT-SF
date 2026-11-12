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

@Entity
@Table(schema = "sicat", name = "distincion")
@Getter
@Setter
@NoArgsConstructor
public class Distincion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_distincion")
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "organismo_otorgante")
    private String organismoOtorgante;

    @Column(nullable = false)
    private Boolean activo = true;
}
