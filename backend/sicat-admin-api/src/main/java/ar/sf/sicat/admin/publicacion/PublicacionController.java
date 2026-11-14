package ar.sf.sicat.admin.publicacion;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.sf.sicat.admin.comun.Roles;
import ar.sf.sicat.admin.establecimiento.EstablecimientoService;
import ar.sf.sicat.admin.establecimiento.dto.EstablecimientoDto;
import ar.sf.sicat.admin.publicacion.dto.EstadoPublicacionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/v1/establecimientos/{id}")
@Tag(name = "Publicación", description = "Qué establecimientos salen en el sitio público")
public class PublicacionController {

    private final PublicacionService publicacion;
    private final EstablecimientoService establecimientos;

    public PublicacionController(PublicacionService publicacion, EstablecimientoService establecimientos) {
        this.publicacion = publicacion;
        this.establecimientos = establecimientos;
    }

    @GetMapping("/publicacion")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Estado de publicación y requisitos que faltan")
    public EstadoPublicacionDto estado(@PathVariable Long id) {
        return publicacion.estado(id);
    }

    @PostMapping("/publicar")
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Pasa a PUBLICADO",
            description = "Requiere establecimiento y oferente activos, lugar con coordenadas y una oferta activa "
                    + "con categoría activa. Si falta algo: 422 con la lista en 'errores'.")
    public EstablecimientoDto publicar(@PathVariable Long id) {
        publicacion.publicar(id);
        return establecimientos.detalle(id);
    }

    @PostMapping("/despublicar")
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Pasa a OCULTO", description = "Deja de verse en el sitio público. 422 si está en BORRADOR.")
    public EstablecimientoDto despublicar(@PathVariable Long id) {
        publicacion.despublicar(id);
        return establecimientos.detalle(id);
    }
}
