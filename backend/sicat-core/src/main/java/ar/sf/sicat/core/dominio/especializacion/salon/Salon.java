package ar.sf.sicat.core.dominio.especializacion.salon;

import java.math.BigDecimal;

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

/** Salón de una oferta (N por oferta). */
@Entity
@Table(schema = "sicat", name = "salon")
@Getter
@Setter
@NoArgsConstructor
public class Salon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_salon")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_oferta", nullable = false)
    private Oferta oferta;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "cap_auditorio")
    private Integer capAuditorio;

    @Column(name = "cap_banquete")
    private Integer capBanquete;

    @Column(name = "cap_coctel")
    private Integer capCoctel;

    private BigDecimal m2;

    @Column(name = "estado_conservacion")
    private String estadoConservacion;

    @Column(name = "estado_funcionamiento")
    private String estadoFuncionamiento;

    @Column(nullable = false)
    private Boolean activo = true;
}
