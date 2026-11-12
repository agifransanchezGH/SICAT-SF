package ar.sf.sicat.core.dominio.especializacion.alojamiento;

import ar.sf.sicat.core.dominio.oferta.Oferta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Extensión 1 a 1 de {@link Oferta} para alojamientos. */
@Entity
@Table(schema = "sicat", name = "alojamiento")
@Getter
@Setter
@NoArgsConstructor
public class Alojamiento {

    @Id
    @Column(name = "id_oferta")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_oferta")
    private Oferta oferta;

    private Short estrellas;

    @Column(name = "cant_habitaciones")
    private Integer cantHabitaciones;

    @Column(name = "total_plazas")
    private Integer totalPlazas;

    @Column(name = "hab_accesibles")
    private Integer habAccesibles;

    @Column(name = "plazas_accesibles")
    private Integer plazasAccesibles;

    @Column(name = "personal_permanente")
    private Integer personalPermanente;

    @Column(name = "personal_no_permanente")
    private Integer personalNoPermanente;
}
