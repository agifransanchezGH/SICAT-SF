package ar.sf.sicat.core.vistapublica;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Solo lectura. Vista {@code publico.v_alojamiento_publico} (sin datos de personal). */
@Entity
@Immutable
@Table(schema = "publico", name = "v_alojamiento_publico")
@Getter
@NoArgsConstructor
public class AlojamientoPublico {

    @Id
    @Column(name = "id_oferta")
    private Long idOferta;

    private Short estrellas;

    @Column(name = "cant_habitaciones")
    private Integer cantHabitaciones;

    @Column(name = "total_plazas")
    private Integer totalPlazas;

    @Column(name = "hab_accesibles")
    private Integer habAccesibles;

    @Column(name = "plazas_accesibles")
    private Integer plazasAccesibles;
}
