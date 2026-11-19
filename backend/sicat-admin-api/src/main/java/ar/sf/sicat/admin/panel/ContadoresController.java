package ar.sf.sicat.admin.panel;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.sf.sicat.admin.comun.Roles;
import ar.sf.sicat.admin.panel.dto.ContadoresDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/v1/contadores")
@Tag(name = "Panel", description = "Datos de apoyo para la navegación del panel")
public class ContadoresController {

    private final ContadoresService servicio;

    public ContadoresController(ContadoresService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Pendientes de trabajo para el menú",
            description = "Ubicaciones a revisar, establecimientos listos para publicar, grupos de posibles "
                    + "duplicados y establecimientos con contactos a revisar. Mismos criterios que los filtros de "
                    + "cada pantalla.")
    public ContadoresDto contadores() {
        return servicio.contadores();
    }
}
