package ar.sf.sicat.admin.catalogo;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.sf.sicat.admin.catalogo.dto.CatalogoItemDto;
import ar.sf.sicat.admin.catalogo.dto.CatalogoItemSolicitud;
import ar.sf.sicat.admin.comun.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/v1/catalogos/{nombre}")
@Tag(name = "Catálogos", description = "nombre: tipo-oferente, tipo-canal, tipo-lugar, localidad, distincion, caracteristica")
public class CatalogoController {

    private final CatalogoService servicio;

    public CatalogoController(CatalogoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Lista un catálogo", description = "Ordenado por nombre. Por defecto solo activos.")
    public List<CatalogoItemDto> listar(
            @Parameter(example = "tipo-oferente") @PathVariable String nombre,
            @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return servicio.listar(TipoCatalogo.desdeRuta(nombre), incluirInactivos);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Crea un ítem", description = "409 si ya existe uno con el mismo nombre.")
    public CatalogoItemDto crear(@PathVariable String nombre, @Valid @RequestBody CatalogoItemSolicitud solicitud) {
        return servicio.crear(TipoCatalogo.desdeRuta(nombre), solicitud);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Modifica un ítem", description = "Con activo=true se reactiva un ítem dado de baja.")
    public CatalogoItemDto modificar(@PathVariable String nombre, @PathVariable Long id,
                                     @Valid @RequestBody CatalogoItemSolicitud solicitud) {
        return servicio.modificar(TipoCatalogo.desdeRuta(nombre), id, solicitud);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Baja lógica", description = "Marca el ítem como inactivo; los registros que lo usan no cambian.")
    public void darDeBaja(@PathVariable String nombre, @PathVariable Long id) {
        servicio.darDeBaja(TipoCatalogo.desdeRuta(nombre), id);
    }
}
