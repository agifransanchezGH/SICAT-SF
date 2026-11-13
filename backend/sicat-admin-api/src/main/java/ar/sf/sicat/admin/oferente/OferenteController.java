package ar.sf.sicat.admin.oferente;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

import ar.sf.sicat.admin.comun.Roles;
import ar.sf.sicat.admin.contacto.ContactoService;
import ar.sf.sicat.admin.contacto.dto.CanalContactoDto;
import ar.sf.sicat.admin.contacto.dto.CanalContactoSolicitud;
import ar.sf.sicat.admin.oferente.dto.OferenteDto;
import ar.sf.sicat.admin.oferente.dto.OferenteSolicitud;
import ar.sf.sicat.core.comun.PaginaDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/v1/oferentes")
@Tag(name = "Oferentes")
public class OferenteController {

    private final OferenteService servicio;
    private final ContactoService contactos;

    public OferenteController(OferenteService servicio, ContactoService contactos) {
        this.servicio = servicio;
        this.contactos = contactos;
    }

    @GetMapping
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Lista oferentes", description = "Paginado. Filtros: q (nombre), tipo (id de tipo de oferente).")
    public PaginaDto<OferenteDto> listar(
            @Parameter(description = "Texto a buscar en el nombre") @RequestParam(required = false) String q,
            @Parameter(description = "Id del tipo de oferente") @RequestParam(required = false) Long tipo,
            @RequestParam(defaultValue = "false") boolean incluirInactivos,
            @ParameterObject @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {
        return servicio.listar(q, tipo, incluirInactivos, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Detalle con sus establecimientos")
    public OferenteDto detalle(@PathVariable Long id) {
        return servicio.detalle(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Crea un oferente")
    public OferenteDto crear(@Valid @RequestBody OferenteSolicitud solicitud) {
        return servicio.crear(solicitud);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Modifica un oferente")
    public OferenteDto modificar(@PathVariable Long id, @Valid @RequestBody OferenteSolicitud solicitud) {
        return servicio.modificar(id, solicitud);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Baja lógica")
    public void darDeBaja(@PathVariable Long id) {
        servicio.darDeBaja(id);
    }

    @GetMapping("/{id}/contactos")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Canales de contacto del oferente")
    public List<CanalContactoDto> contactos(@PathVariable Long id) {
        return contactos.deOferente(id);
    }

    @PutMapping("/{id}/contactos")
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Reemplaza los canales de contacto",
            description = "Con id se modifica, sin id se crea; los que no vienen se eliminan.")
    public List<CanalContactoDto> reemplazarContactos(@PathVariable Long id,
                                                      @Valid @RequestBody List<@Valid CanalContactoSolicitud> lista) {
        return contactos.reemplazarDeOferente(id, lista);
    }
}
