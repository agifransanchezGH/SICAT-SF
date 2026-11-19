package ar.sf.sicat.admin.importacion;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.sf.sicat.admin.comun.Roles;
import ar.sf.sicat.admin.importacion.dto.GrupoDuplicadosDto;
import ar.sf.sicat.admin.importacion.dto.GrupoSolicitud;
import ar.sf.sicat.admin.importacion.dto.PreviaResolucionDto;
import ar.sf.sicat.admin.importacion.dto.ResolucionResultadoDto;
import ar.sf.sicat.admin.importacion.dto.ResolucionDuplicadosSolicitud;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/v1/establecimientos/duplicados")
@Tag(name = "Posibles duplicados", description = "Establecimientos activos con el mismo nombre normalizado")
public class DuplicadosController {

    private final DuplicadosService servicio;

    public DuplicadosController(DuplicadosService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Posibles duplicados",
            description = "Grupos de establecimientos activos con el mismo nombre normalizado, sin los marcados "
                    + "\"no son duplicados\". Primero los que comparten dirección. Cada integrante trae dirección, "
                    + "oferente, contactos, categorías, cantidad de ofertas activas y estado de publicación.")
    public List<GrupoDuplicadosDto> buscar() {
        return servicio.buscar();
    }

    @PostMapping("/descartes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Marca un grupo como \"no son duplicados\"",
            description = "El grupo deja de aparecer mientras tenga exactamente estos integrantes; si entra o sale "
                    + "uno, vuelve a aparecer. 409 si el grupo cambió desde que se cargó la lista.")
    public void descartar(@Valid @RequestBody GrupoSolicitud grupo) {
        servicio.descartar(grupo);
    }

    @PostMapping("/resolucion/previa")
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Vista previa de \"Quedarme con este\"",
            description = "No cambia nada. Devuelve, por cada uno que se daría de baja, los contactos y características "
                    + "que el que queda no tiene (sin repetir: mismo tipo y valor sin importar formato; teléfonos por "
                    + "sus dígitos). 409 si el grupo cambió.")
    public PreviaResolucionDto previa(@Valid @RequestBody ResolucionDuplicadosSolicitud solicitud) {
        return servicio.previa(solicitud.conservarId(), solicitud.grupo());
    }

    @PostMapping("/resolucion")
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "\"Quedarme con este\": da de baja a los demás del grupo",
            description = "Con copiarContactos / copiarCaracteristicas le copia al que queda lo que le falta (lo mismo "
                    + "que muestra la vista previa) y da de baja a los demás integrantes; todo en una transacción (un "
                    + "publicado dado de baja pasa a OCULTO). Solo ADMIN, como la baja individual. 409 si el grupo "
                    + "cambió desde que se cargó la lista.")
    public ResolucionResultadoDto quedarseCon(@Valid @RequestBody ResolucionDuplicadosSolicitud solicitud) {
        return servicio.quedarseCon(solicitud.conservarId(), solicitud.grupo(),
                Boolean.TRUE.equals(solicitud.copiarContactos()), Boolean.TRUE.equals(solicitud.copiarCaracteristicas()));
    }
}
