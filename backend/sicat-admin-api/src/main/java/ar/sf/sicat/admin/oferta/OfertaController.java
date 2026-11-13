package ar.sf.sicat.admin.oferta;

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

import ar.sf.sicat.admin.comun.IdsSolicitud;
import ar.sf.sicat.admin.comun.Roles;
import ar.sf.sicat.admin.oferta.dto.HorarioDto;
import ar.sf.sicat.admin.oferta.dto.HorarioSolicitud;
import ar.sf.sicat.admin.oferta.dto.OfertaDto;
import ar.sf.sicat.admin.oferta.dto.OfertaSolicitud;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/v1")
@Tag(name = "Ofertas", description = "Lo que ofrece un establecimiento, sus categorías y horarios")
public class OfertaController {

    private final OfertaService servicio;
    private final HorarioService horarios;

    public OfertaController(OfertaService servicio, HorarioService horarios) {
        this.servicio = servicio;
        this.horarios = horarios;
    }

    @GetMapping("/establecimientos/{id}/ofertas")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Ofertas del establecimiento", description = "Por defecto solo activas.")
    public List<OfertaDto> deEstablecimiento(@PathVariable Long id,
                                             @RequestParam(defaultValue = "false") boolean incluirInactivas) {
        return servicio.deEstablecimiento(id, incluirInactivas);
    }

    @PostMapping("/establecimientos/{id}/ofertas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Crea una oferta en el establecimiento")
    public OfertaDto crear(@PathVariable Long id, @Valid @RequestBody OfertaSolicitud solicitud) {
        return servicio.crear(id, solicitud);
    }

    @GetMapping("/ofertas/{id}")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Detalle con categorías, horarios y especializaciones")
    public OfertaDto detalle(@PathVariable Long id) {
        return servicio.detalle(id);
    }

    @PutMapping("/ofertas/{id}")
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Modifica una oferta")
    public OfertaDto modificar(@PathVariable Long id, @Valid @RequestBody OfertaSolicitud solicitud) {
        return servicio.modificar(id, solicitud);
    }

    @DeleteMapping("/ofertas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Baja lógica")
    public void darDeBaja(@PathVariable Long id) {
        servicio.darDeBaja(id);
    }

    @GetMapping("/ofertas/{id}/categorias")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Categorías adicionales")
    public List<OfertaDto.CategoriaRef> categorias(@PathVariable Long id) {
        return servicio.categoriasAdicionales(id);
    }

    @PutMapping("/ofertas/{id}/categorias")
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Reemplaza las categorías adicionales", description = "422 si incluye la categoría principal.")
    public List<OfertaDto.CategoriaRef> reemplazarCategorias(@PathVariable Long id,
                                                             @Valid @RequestBody IdsSolicitud solicitud) {
        return servicio.reemplazarCategoriasAdicionales(id, solicitud.ids());
    }

    @GetMapping("/ofertas/{id}/horarios")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Horarios generales de la oferta")
    public List<HorarioDto> horarios(@PathVariable Long id) {
        return horarios.deOferta(id);
    }

    @PutMapping("/ofertas/{id}/horarios")
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Reemplaza los horarios generales",
            description = "Con id se modifica, sin id se crea; los que no vienen se eliminan.")
    public List<HorarioDto> reemplazarHorarios(@PathVariable Long id,
                                               @Valid @RequestBody List<@Valid HorarioSolicitud> lista) {
        return horarios.reemplazarDeOferta(id, lista);
    }
}
