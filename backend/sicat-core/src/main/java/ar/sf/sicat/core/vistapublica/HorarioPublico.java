package ar.sf.sicat.core.vistapublica;

import java.time.LocalTime;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Solo lectura. Vista {@code publico.v_horario_publico}. */
@Entity
@Immutable
@Table(schema = "publico", name = "v_horario_publico")
@Getter
@NoArgsConstructor
public class HorarioPublico {

    @Id
    @Column(name = "id_horario")
    private Long id;

    @Column(name = "id_oferta")
    private Long idOferta;

    @Column(name = "id_visita")
    private Long idVisita;

    @Column(name = "dia_semana")
    private Short diaSemana;

    @Column(name = "hora_desde")
    private LocalTime horaDesde;

    @Column(name = "hora_hasta")
    private LocalTime horaHasta;
}
