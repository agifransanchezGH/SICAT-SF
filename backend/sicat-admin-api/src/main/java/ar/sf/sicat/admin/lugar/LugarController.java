package ar.sf.sicat.admin.lugar;

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
import ar.sf.sicat.admin.lugar.dto.LugarDto;
import ar.sf.sicat.admin.lugar.dto.LugarSolicitud;
import ar.sf.sicat.core.comun.PaginaDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/v1/lugares")
@Tag(name = "Lugares")
public class LugarController {

    private final LugarService servicio;

    public LugarController(LugarService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Lista lugares", description = "Paginado. Filtros: q (nombre o calle), tipoLugar, padreId, localidadId, bbox.")
    public PaginaDto<LugarDto> listar(
            @RequestParam(required = false) String q,
            @Parameter(description = "Id del tipo de lugar") @RequestParam(required = false) Long tipoLugar,
            @RequestParam(required = false) Long padreId,
            @RequestParam(required = false) Long localidadId,
            @Parameter(description = "minLng,minLat,maxLng,maxLat", example = "-60.75,-31.68,-60.65,-31.58")
            @RequestParam(required = false) String bbox,
            @ParameterObject @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {
        return servicio.listar(q, tipoLugar, padreId, localidadId, bbox, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Detalle con lugares hijos")
    public LugarDto detalle(@PathVariable Long id) {
        return servicio.detalle(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Crea un lugar")
    public LugarDto crear(@Valid @RequestBody LugarSolicitud solicitud) {
        return servicio.crear(solicitud);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Modifica un lugar")
    public LugarDto modificar(@PathVariable Long id, @Valid @RequestBody LugarSolicitud solicitud) {
        return servicio.modificar(id, solicitud);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Elimina un lugar", description = "409 si contiene otros lugares o lo usa un establecimiento o circuito.")
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}
