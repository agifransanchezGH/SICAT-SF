package ar.sf.sicat.admin.seguridad;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.sf.sicat.admin.seguridad.dto.EstadoSolicitud;
import ar.sf.sicat.admin.seguridad.dto.UsuarioCrearSolicitud;
import ar.sf.sicat.admin.seguridad.dto.UsuarioDto;
import ar.sf.sicat.admin.seguridad.dto.UsuarioModificarSolicitud;
import ar.sf.sicat.core.comun.PaginaDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/v1/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Usuarios", description = "Solo ADMIN")
public class UsuarioController {

    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Lista usuarios (paginado)")
    public PaginaDto<UsuarioDto> listar(@PageableDefault(size = 20, sort = "nombreCompleto") Pageable pageable) {
        return servicio.listar(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un usuario", description = "409 si el email ya existe.")
    public UsuarioDto crear(@Valid @RequestBody UsuarioCrearSolicitud solicitud) {
        return servicio.crear(solicitud);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifica usuario y rol", description = "La contraseña es opcional. Un ADMIN no puede quitarse su propio rol.")
    public UsuarioDto modificar(@PathVariable Long id,
                                @Valid @RequestBody UsuarioModificarSolicitud solicitud,
                                @AuthenticationPrincipal Jwt jwt) {
        return servicio.modificar(id, solicitud, TokenService.idUsuario(jwt));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Activa o desactiva", description = "Un usuario inactivo no puede iniciar sesión ni renovar tokens.")
    public UsuarioDto cambiarEstado(@PathVariable Long id,
                                    @Valid @RequestBody EstadoSolicitud solicitud,
                                    @AuthenticationPrincipal Jwt jwt) {
        return servicio.cambiarEstado(id, solicitud.activo(), TokenService.idUsuario(jwt));
    }
}
