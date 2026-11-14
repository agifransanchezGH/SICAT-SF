package ar.sf.sicat.admin.auditoria;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.sf.sicat.admin.auditoria.dto.AuditoriaDto;
import ar.sf.sicat.admin.auditoria.dto.AuditoriaFiltro;
import ar.sf.sicat.core.comun.PaginaDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/v1/auditoria")
@Tag(name = "Auditoría", description = "Solo lectura")
public class AuditoriaController {

    private final AuditoriaConsultaService servicio;

    public AuditoriaController(AuditoriaConsultaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Busca en la auditoría", description = "Paginado, por defecto del cambio más reciente al más antiguo.")
    public PaginaDto<AuditoriaDto> buscar(
            @ParameterObject AuditoriaFiltro filtro,
            @ParameterObject @PageableDefault(size = 20, sort = {"fechaHora", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return servicio.buscar(filtro, pageable);
    }

    @GetMapping("/{tabla}/{clave}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CARGADOR')")
    @Operation(summary = "Historial de un registro",
            description = "tabla con o sin esquema (p. ej. 'establecimiento'); clave = id del registro.")
    public List<AuditoriaDto> historial(@PathVariable String tabla, @PathVariable String clave) {
        return servicio.historial(tabla, clave);
    }
}
