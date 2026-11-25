package ar.sf.sicat.admin.panel;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.sf.sicat.admin.comun.Roles;
import ar.sf.sicat.admin.panel.dto.EstadisticasDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/v1/estadisticas")
@Tag(name = "Panel", description = "Datos de apoyo para la navegación del panel")
public class EstadisticasController {

    private final EstadisticasService servicio;

    public EstadisticasController(EstadisticasService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Cifras del tablero de inicio",
            description = "Establecimientos activos por estado de publicación, establecimientos por categoría raíz y "
                    + "subcategoría (mismo criterio que el filtro categoria del listado), oferentes activos, ofertas "
                    + "activas, circuitos y trabajo pendiente (los mismos números que GET /contadores). Todo sale de "
                    + "consultas agregadas.")
    public EstadisticasDto estadisticas() {
        return servicio.estadisticas();
    }
}
