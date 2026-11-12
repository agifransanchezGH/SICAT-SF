package ar.sf.sicat.core.dominio.contacto;

import ar.sf.sicat.core.dominio.establecimiento.Establecimiento;
import ar.sf.sicat.core.dominio.oferente.Oferente;
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

/**
 * Canal de contacto de un establecimiento <b>o</b> de un oferente (exactamente uno; CHECK en la BD).
 * Solo los que tienen {@code esPublico = true} salen al sitio público.
 */
@Entity
@Table(schema = "sicat", name = "canal_contacto")
@Getter
@Setter
@NoArgsConstructor
public class CanalContacto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_canal")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_establecimiento")
    private Establecimiento establecimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_oferente")
    private Oferente oferente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_canal", nullable = false)
    private TipoCanal tipoCanal;

    @Column(nullable = false)
    private String valor;

    private String uso;

    @Column(name = "es_publico", nullable = false)
    private Boolean esPublico = false;

    private Short orden;
}
