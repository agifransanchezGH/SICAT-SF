package ar.sf.sicat.admin.auditoria.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import ar.sf.sicat.core.auditoria.AccionAuditoria;
import ar.sf.sicat.core.auditoria.OrigenAuditoria;
import io.swagger.v3.oas.annotations.Parameter;

/** Filtros de GET /auditoria. Todos opcionales. Fechas en hora de Argentina, ambas inclusive. */
public record AuditoriaFiltro(
        @Parameter(description = "Tabla, con o sin esquema: 'sicat.establecimiento' o 'establecimiento'") String tabla,
        @Parameter(description = "Clave del registro (id)") String clave,
        @Parameter(description = "Id del usuario que hizo el cambio") Long usuario,
        AccionAuditoria accion,
        OrigenAuditoria origen,
        @Parameter(description = "Desde (yyyy-MM-dd)") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @Parameter(description = "Hasta (yyyy-MM-dd)") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
}
