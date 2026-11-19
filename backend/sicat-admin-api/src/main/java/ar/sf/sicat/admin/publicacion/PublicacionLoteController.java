package ar.sf.sicat.admin.publicacion;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.sf.sicat.admin.comun.Roles;
import ar.sf.sicat.admin.comun.lote.LoteSolicitud;
import ar.sf.sicat.admin.comun.lote.ResultadoLoteDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/v1/establecimientos/lote")
@Tag(name = "Publicación")
public class PublicacionLoteController {

    private final PublicacionLoteService servicio;

    public PublicacionLoteController(PublicacionLoteService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/publicar")
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Publica varios establecimientos",
            description = "Cada uno se valida y se publica en su propia transacción (mismos requisitos que /publicar). "
                    + "Responde 200 con el resultado de cada id: los que no cumplen traen en 'errores' lo que falta; "
                    + "un rechazo no revierte a los demás. Hasta " + LoteSolicitud.MAXIMO + " ids.")
    public ResultadoLoteDto publicar(@Valid @RequestBody LoteSolicitud solicitud) {
        return servicio.publicar(solicitud.ids());
    }
}
