package ar.sf.sicat.core.vistapublica;

import java.math.BigDecimal;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Solo lectura. Vista {@code publico.v_salon_publico}. */
@Entity
@Immutable
@Table(schema = "publico", name = "v_salon_publico")
@Getter
@NoArgsConstructor
public class SalonPublico {

    @Id
    @Column(name = "id_salon")
    private Long id;

    @Column(name = "id_oferta")
    private Long idOferta;

    private String nombre;

    @Column(name = "cap_auditorio")
    private Integer capAuditorio;

    @Column(name = "cap_banquete")
    private Integer capBanquete;

    @Column(name = "cap_coctel")
    private Integer capCoctel;

    private BigDecimal m2;
}
