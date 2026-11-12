package ar.sf.sicat.core.dominio.horario;

import java.time.LocalTime;

import ar.sf.sicat.core.dominio.especializacion.visitaguiada.VisitaGuiada;
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

/** Horario de una oferta, o de una visita guiada puntual de esa oferta. */
@Entity
@Table(schema = "sicat", name = "horario")
@Getter
@Setter
@NoArgsConstructor
public class Horario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_oferta", nullable = false)
    private Oferta oferta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_visita")
    private VisitaGuiada visita;

    /** 1 = lunes … 7 = domingo (ISO 8601). */
    @Column(name = "dia_semana", nullable = false)
    private Short diaSemana;

    @Column(name = "hora_desde", nullable = false)
    private LocalTime horaDesde;

    @Column(name = "hora_hasta", nullable = false)
    private LocalTime horaHasta;
}
