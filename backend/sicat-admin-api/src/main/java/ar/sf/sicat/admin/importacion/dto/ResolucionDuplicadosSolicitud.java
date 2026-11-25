package ar.sf.sicat.admin.importacion.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * "Quedarme con este": el que queda, el grupo tal como se vio y cuáles de sus integrantes son realmente duplicados
 * entre sí. Se dan de baja los marcados menos el que queda; los no marcados no se tocan. Opcionalmente se le copian
 * al que queda los contactos y características de los marcados que no tiene (ver la vista previa).
 */
public record ResolucionDuplicadosSolicitud(
        @Schema(description = "Id del establecimiento que queda (tiene que estar entre los marcados)") @NotNull Long conservarId,
        @NotNull @Valid GrupoSolicitud grupo,
        @Schema(description = "Integrantes que son duplicados entre sí, incluido el que queda (mínimo 2). Si no viene, "
                + "son todos los del grupo. Los no marcados quedan como establecimientos distintos del que queda")
        @Size(min = 2, max = 50) List<@NotNull Long> marcados,
        @Schema(description = "Copiar los contactos que le faltan (por defecto false)") Boolean copiarContactos,
        @Schema(description = "Agregar las características que le faltan (por defecto false)") Boolean copiarCaracteristicas) {
}
