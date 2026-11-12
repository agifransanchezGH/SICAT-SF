package ar.sf.sicat.core.dominio.lugar;

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
@Table(schema = "sicat", name = "localidad")
@Getter
@Setter
@NoArgsConstructor
public class Localidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_localidad")
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String provincia;

    @Column(nullable = false)
    private Boolean activo = true;
}
