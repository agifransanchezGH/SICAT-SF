package ar.sf.sicat.core.vistapublica;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Solo lectura. Vista {@code publico.v_establecimiento_publico}: establecimientos PUBLICADOS y activos. */
@Entity
@Immutable
@Table(schema = "publico", name = "v_establecimiento_publico")
@Getter
@NoArgsConstructor
public class EstablecimientoPublico {

    @Id
    @Column(name = "id_establecimiento")
    private Long id;

    @Column(name = "id_oferente")
    private Long idOferente;

    private String nombre;

    private String descripcion;

    @Column(name = "anio_inauguracion")
    private Short anioInauguracion;

    @Column(name = "estado_funcionamiento")
    private String estadoFuncionamiento;

    @Column(name = "motivo_estado")
    private String motivoEstado;

    @Column(name = "fecha_reapertura")
    private LocalDate fechaReapertura;

    private Boolean destacado;

    private String oferente;

    private String lugar;

    private String calle;

    private Integer altura;

    private BigDecimal km;

    @Column(name = "calle_cruce")
    private String calleCruce;

    private String complemento;

    @Column(name = "id_localidad")
    private Long idLocalidad;

    private String localidad;

    private String provincia;

    private Double lat;

    private Double lng;
}
