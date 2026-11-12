package ar.sf.sicat.core.dominio.establecimiento;

import java.time.LocalDate;

import ar.sf.sicat.core.dominio.lugar.Lugar;
import ar.sf.sicat.core.dominio.oferente.Oferente;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** Entidad física de un oferente, ubicada en un lugar. Tiene ofertas. */
@Entity
@Table(schema = "sicat", name = "establecimiento")
@Getter
@Setter
@NoArgsConstructor
public class Establecimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_establecimiento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_oferente", nullable = false)
    private Oferente oferente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lugar")
    private Lugar lugar;

    @Column(nullable = false)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_publicacion", nullable = false)
    private EstadoPublicacion estadoPublicacion = EstadoPublicacion.BORRADOR;

    @Column(nullable = false)
    private Boolean destacado = false;

    @Column(nullable = false)
    private Boolean activo = true;
}
