package ar.sf.sicat.admin.establecimiento;

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

import ar.sf.sicat.admin.comun.CaracteristicaAsignadaDto;
import ar.sf.sicat.admin.comun.CaracteristicaAsignadaSolicitud;
import ar.sf.sicat.admin.comun.Roles;
import ar.sf.sicat.admin.contacto.ContactoService;
import ar.sf.sicat.admin.contacto.dto.CanalContactoDto;
import ar.sf.sicat.admin.contacto.dto.CanalContactoSolicitud;
import ar.sf.sicat.admin.establecimiento.dto.DistincionAsignadaDto;
import ar.sf.sicat.admin.establecimiento.dto.DistincionAsignadaSolicitud;
import ar.sf.sicat.admin.establecimiento.dto.EstablecimientoDto;
import ar.sf.sicat.admin.establecimiento.dto.EstablecimientoResumenDto;
import ar.sf.sicat.admin.establecimiento.dto.EstablecimientoSolicitud;
import ar.sf.sicat.admin.establecimiento.dto.ReferenteDto;
import ar.sf.sicat.admin.establecimiento.dto.ReferenteSolicitud;
import ar.sf.sicat.core.comun.PaginaDto;
import ar.sf.sicat.core.dominio.establecimiento.EstadoPublicacion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/v1/establecimientos")
@Tag(name = "Establecimientos")
public class EstablecimientoController {

    private final EstablecimientoService servicio;
    private final AnexosEstablecimientoService anexos;
    private final ContactoService contactos;

    public EstablecimientoController(EstablecimientoService servicio, AnexosEstablecimientoService anexos,
                                     ContactoService contactos) {
        this.servicio = servicio;
        this.anexos = anexos;
        this.contactos = contactos;
    }

    @GetMapping
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Lista establecimientos",
            description = "Paginado. Filtros: q, categoria (incluye subcategorías), estado, oferenteId, localidadId, bbox.")
    public PaginaDto<EstablecimientoResumenDto> listar(
            @Parameter(description = "Texto a buscar en el nombre") @RequestParam(required = false) String q,
            @Parameter(description = "Id de categoría") @RequestParam(required = false) Long categoria,
            @RequestParam(required = false) EstadoPublicacion estado,
            @RequestParam(required = false) Long oferenteId,
            @RequestParam(required = false) Long localidadId,
            @Parameter(description = "minLng,minLat,maxLng,maxLat") @RequestParam(required = false) String bbox,
            @RequestParam(defaultValue = "false") boolean incluirInactivos,
            @ParameterObject @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {
        return servicio.listar(q, categoria, estado, oferenteId, localidadId, bbox, incluirInactivos, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Detalle: datos comunes, oferente, lugar y ofertas")
    public EstablecimientoDto detalle(@PathVariable Long id) {
        return servicio.detalle(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Crea un establecimiento (queda en BORRADOR)")
    public EstablecimientoDto crear(@Valid @RequestBody EstablecimientoSolicitud solicitud) {
        return servicio.crear(solicitud);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Modifica los datos comunes")
    public EstablecimientoDto modificar(@PathVariable Long id, @Valid @RequestBody EstablecimientoSolicitud solicitud) {
        return servicio.modificar(id, solicitud);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Baja lógica")
    public void darDeBaja(@PathVariable Long id) {
        servicio.darDeBaja(id);
    }

    // ------------------------------------------------------------------ contactos

    @GetMapping("/{id}/contactos")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Canales de contacto del establecimiento")
    public List<CanalContactoDto> contactos(@PathVariable Long id) {
        return contactos.deEstablecimiento(id);
    }

    @PutMapping("/{id}/contactos")
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Reemplaza los canales de contacto",
            description = "Con id se modifica, sin id se crea; los que no vienen se eliminan.")
    public List<CanalContactoDto> reemplazarContactos(@PathVariable Long id,
                                                      @Valid @RequestBody List<@Valid CanalContactoSolicitud> lista) {
        return contactos.reemplazarDeEstablecimiento(id, lista);
    }

    // ------------------------------------------------------------------ referentes

    @GetMapping("/{id}/referentes")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Referentes (contactos internos, no públicos)")
    public List<ReferenteDto> referentes(@PathVariable Long id) {
        return anexos.referentes(id);
    }

    @PutMapping("/{id}/referentes")
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Reemplaza los referentes")
    public List<ReferenteDto> reemplazarReferentes(@PathVariable Long id,
                                                   @Valid @RequestBody List<@Valid ReferenteSolicitud> lista) {
        return anexos.reemplazarReferentes(id, lista);
    }

    // ------------------------------------------------------------------ distinciones

    @GetMapping("/{id}/distinciones")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Distinciones con año y URL de verificación")
    public List<DistincionAsignadaDto> distinciones(@PathVariable Long id) {
        return anexos.distinciones(id);
    }

    @PutMapping("/{id}/distinciones")
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Reemplaza las distinciones")
    public List<DistincionAsignadaDto> reemplazarDistinciones(
            @PathVariable Long id, @Valid @RequestBody List<@Valid DistincionAsignadaSolicitud> lista) {
        return anexos.reemplazarDistinciones(id, lista);
    }

    // ------------------------------------------------------------------ características

    @GetMapping("/{id}/caracteristicas")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Características con cantidad")
    public List<CaracteristicaAsignadaDto> caracteristicas(@PathVariable Long id) {
        return anexos.caracteristicas(id);
    }

    @PutMapping("/{id}/caracteristicas")
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Reemplaza las características", description = "422 si se indica cantidad en una que no la admite.")
    public List<CaracteristicaAsignadaDto> reemplazarCaracteristicas(
            @PathVariable Long id, @Valid @RequestBody List<@Valid CaracteristicaAsignadaSolicitud> lista) {
        return anexos.reemplazarCaracteristicas(id, lista);
    }
}
