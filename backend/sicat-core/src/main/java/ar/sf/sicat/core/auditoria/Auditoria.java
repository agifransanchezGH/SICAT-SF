package ar.sf.sicat.core.auditoria;

import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import ar.sf.sicat.core.seguridad.Usuario;
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

/**
 * Registro de auditoría. Ningún paquete de dominio debe referenciar este paquete
 * (regla 5 de ARQUITECTURA.md, verificada con ArchUnit).
 */
@Entity
@Table(schema = "auditoria", name = "auditoria")
@Getter
@Setter
@NoArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long id;

    @Column(name = "tabla_afectada", nullable = false)
    private String tablaAfectada;

    @Column(name = "clave_registro", nullable = false)
    private String claveRegistro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccionAuditoria accion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(name = "fecha_hora", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigenAuditoria origen = OrigenAuditoria.APP;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_anteriores", columnDefinition = "jsonb")
    private String datosAnteriores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_nuevos", columnDefinition = "jsonb")
    private String datosNuevos;
}
