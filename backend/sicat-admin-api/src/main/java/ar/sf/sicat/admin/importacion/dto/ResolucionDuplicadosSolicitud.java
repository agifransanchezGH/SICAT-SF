package ar.sf.sicat.admin.importacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * "Quedarme con este": el que queda y el grupo tal como se vio; los demás integrantes se dan de baja. Opcionalmente
 * se le copian al que queda los contactos y características de los demás que no tiene (ver la vista previa).
 */
public record ResolucionDuplicadosSolicitud(
        @Schema(description = "Id del establecimiento que queda") @NotNull Long conservarId,
        @NotNull @Valid GrupoSolicitud grupo,
        @Schema(description = "Copiar los contactos que le faltan (por defecto false)") Boolean copiarContactos,
        @Schema(description = "Agregar las características que le faltan (por defecto false)") Boolean copiarCaracteristicas) {
}
