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
import ar.sf.sicat.admin.catalogo.dto.CategoriaNodoDto;
import ar.sf.sicat.admin.catalogo.dto.CategoriaSolicitud;
import ar.sf.sicat.admin.comun.IdsSolicitud;
import ar.sf.sicat.admin.comun.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/v1/categorias")
@Tag(name = "Categorías", description = "Árbol de categorías y características que aplican a cada una")
public class CategoriaController {

    private final CategoriaService servicio;

    public CategoriaController(CategoriaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Árbol de categorías", description = "Con padreId devuelve solo esa rama. Por defecto sin inactivas.")
    public List<CategoriaNodoDto> arbol(@RequestParam(required = false) Long padreId,
                                        @RequestParam(defaultValue = "false") boolean incluirInactivas) {
        return servicio.arbol(padreId, incluirInactivas);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Crea una categoría")
    public CategoriaNodoDto crear(@Valid @RequestBody CategoriaSolicitud solicitud) {
        return servicio.crear(solicitud);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Modifica una categoría", description = "422 si el nuevo padre genera un ciclo.")
    public CategoriaNodoDto modificar(@PathVariable Long id, @Valid @RequestBody CategoriaSolicitud solicitud) {
        return servicio.modificar(id, solicitud);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Baja lógica")
    public void darDeBaja(@PathVariable Long id) {
        servicio.darDeBaja(id);
    }

    @GetMapping("/{id}/caracteristicas")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Características que aplican a la categoría")
    public List<CatalogoItemDto> caracteristicas(@PathVariable Long id) {
        return servicio.caracteristicas(id);
    }

    @PutMapping("/{id}/caracteristicas")
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Reemplaza las características que aplican a la categoría")
    public List<CatalogoItemDto> reemplazarCaracteristicas(@PathVariable Long id,
                                                           @Valid @RequestBody IdsSolicitud solicitud) {
        return servicio.reemplazarCaracteristicas(id, solicitud.ids());
    }
}
