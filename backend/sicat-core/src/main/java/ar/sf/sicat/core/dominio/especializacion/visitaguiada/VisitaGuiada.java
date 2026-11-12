package ar.sf.sicat.core.dominio.especializacion.visitaguiada;

import ar.sf.sicat.core.dominio.oferta.Oferta;
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

/** Visita guiada de una oferta (N por oferta). Sus horarios están en {@code horario.id_visita}. */
@Entity
@Table(schema = "sicat", name = "visita_guiada")
@Getter
@Setter
@NoArgsConstructor
public class VisitaGuiada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_visita")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_oferta", nullable = false)
    private Oferta oferta;

    private String modalidad;

    @Column(name = "requiere_reserva", nullable = false)
    private Boolean requiereReserva = false;

    @Column(nullable = false)
    private Boolean habilitada = true;
}
