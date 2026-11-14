package ar.sf.sicat.admin.auditoria.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.JsonNode;

import ar.sf.sicat.core.auditoria.AccionAuditoria;
import ar.sf.sicat.core.auditoria.OrigenAuditoria;
import io.swagger.v3.oas.annotations.media.Schema;

public record AuditoriaDto(
        Long id,
        @Schema(example = "sicat.establecimiento") String tabla,
        @Schema(description = "Id del registro; JSON si la clave es compuesta", example = "12") String clave,
        AccionAuditoria accion,
        @Schema(description = "null si el cambio no lo hizo un usuario (origen SISTEMA)") UsuarioResumen usuario,
        OffsetDateTime fechaHora,
        OrigenAuditoria origen,
        @Schema(description = "Campos antes del cambio (null en ALTA)") JsonNode datosAnteriores,
        @Schema(description = "Campos después del cambio (null en ELIMINACION)") JsonNode datosNuevos) {
}
