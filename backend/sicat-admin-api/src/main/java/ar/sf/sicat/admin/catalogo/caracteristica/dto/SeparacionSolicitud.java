package ar.sf.sicat.admin.catalogo.caracteristica.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Partir una característica que en realidad son varias pegadas ("Servicio médico.Estacionamiento"). */
public record SeparacionSolicitud(
        @Schema(description = "Nombres de las partes, en el mismo grupo", example = "[\"Servicio médico\", \"Estacionamiento\"]")
        @NotNull @Size(min = 2, max = 10) List<@NotBlank @Size(max = 200) String> nombres) {
}
