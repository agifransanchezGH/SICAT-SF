package ar.sf.sicat.core.vistapublica;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Solo lectura. Vista {@code publico.v_visita_guiada_publica} (solo habilitadas). */
@Entity
@Immutable
@Table(schema = "publico", name = "v_visita_guiada_publica")
@Getter
@NoArgsConstructor
public class VisitaGuiadaPublica {

    @Id
    @Column(name = "id_visita")
    private Long id;

    @Column(name = "id_oferta")
    private Long idOferta;

    private String modalidad;

    @Column(name = "requiere_reserva")
    private Boolean requiereReserva;
}
