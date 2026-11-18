package ar.sf.sicat.admin.catalogo.caracteristica;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.sf.sicat.admin.catalogo.caracteristica.dto.CaracteristicaUsoDto;
import ar.sf.sicat.admin.catalogo.caracteristica.dto.FusionSolicitud;
import ar.sf.sicat.admin.catalogo.caracteristica.dto.LimpiezaResultadoDto;
import ar.sf.sicat.admin.catalogo.caracteristica.dto.SeparacionSolicitud;
import ar.sf.sicat.admin.comun.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/v1/caracteristicas")
@Tag(name = "Limpieza de características", description = "Fusionar duplicados, separar entradas pegadas y pasar a notas internas (ADR 0017)")
@PreAuthorize(Roles.ADMIN)
public class LimpiezaCaracteristicasController {

    private final LimpiezaCaracteristicasService servicio;

    public LimpiezaCaracteristicasController(LimpiezaCaracteristicasService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/uso")
    @Operation(summary = "Características con sus usos",
            description = "Activas e inactivas, con cuántos establecimientos, salones y categorías las usan. Filtro opcional por grupo.")
    public List<CaracteristicaUsoDto> uso(
            @Parameter(example = "Accesibilidad: baños") @RequestParam(required = false) String grupo) {
        return servicio.uso(grupo);
    }

    @PostMapping("/fusiones")
    @Operation(summary = "Fusiona características del mismo grupo",
            description = "Pasa los usos de las absorbidas a la que queda (sin duplicar asignaciones) y borra las absorbidas. "
                    + "Opcionalmente renombra la que queda. 400 si los ids están repetidos o la que queda está entre las "
                    + "absorbidas, 404 si alguna no existe, 409 si el nombre nuevo ya existe en el grupo, 422 si son de grupos distintos.")
    public LimpiezaResultadoDto fusionar(@Valid @RequestBody FusionSolicitud solicitud) {
        return servicio.fusionar(solicitud);
    }

    @PostMapping("/{id}/separacion")
    @Operation(summary = "Separa una característica en varias",
            description = "Para entradas pegadas (\"Servicio médico.Estacionamiento\"). Cada parte se crea o se reutiliza en el "
                    + "mismo grupo y recibe los mismos usos que la original, que se borra.")
    public LimpiezaResultadoDto separar(@PathVariable Long id, @Valid @RequestBody SeparacionSolicitud solicitud) {
        return servicio.separar(id, solicitud);
    }

    @PostMapping("/{id}/a-notas-internas")
    @Operation(summary = "Pasa una característica a notas internas",
            description = "Para lo que no es una característica (p. ej. una dirección): se agrega a las notas internas de cada "
                    + "establecimiento que la tenía y se borra. 409 si la usan salones o categorías.")
    public LimpiezaResultadoDto aNotasInternas(@PathVariable Long id) {
        return servicio.aNotasInternas(id);
    }
}
