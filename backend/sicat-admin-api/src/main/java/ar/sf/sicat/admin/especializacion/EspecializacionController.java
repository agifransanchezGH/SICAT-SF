package ar.sf.sicat.admin.especializacion;

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

import ar.sf.sicat.admin.comun.CaracteristicaAsignadaDto;
import ar.sf.sicat.admin.comun.CaracteristicaAsignadaSolicitud;
import ar.sf.sicat.admin.comun.Roles;
import ar.sf.sicat.admin.especializacion.dto.AlojamientoDto;
import ar.sf.sicat.admin.especializacion.dto.CircuitoDto;
import ar.sf.sicat.admin.especializacion.dto.CircuitoSolicitud;
import ar.sf.sicat.admin.especializacion.dto.ParadaDto;
import ar.sf.sicat.admin.especializacion.dto.ParadaSolicitud;
import ar.sf.sicat.admin.especializacion.dto.SalonDto;
import ar.sf.sicat.admin.especializacion.dto.SalonSolicitud;
import ar.sf.sicat.admin.especializacion.dto.VisitaGuiadaDto;
import ar.sf.sicat.admin.especializacion.dto.VisitaGuiadaSolicitud;
import ar.sf.sicat.admin.oferta.HorarioService;
import ar.sf.sicat.admin.oferta.dto.HorarioDto;
import ar.sf.sicat.admin.oferta.dto.HorarioSolicitud;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/v1")
@Tag(name = "Especializaciones", description = "Alojamiento, circuito, salones y visitas guiadas de una oferta")
public class EspecializacionController {

    private final EspecializacionService servicio;
    private final HorarioService horarios;

    public EspecializacionController(EspecializacionService servicio, HorarioService horarios) {
        this.servicio = servicio;
        this.horarios = horarios;
    }

    // ------------------------------------------------------------------ alojamiento

    @GetMapping("/ofertas/{id}/alojamiento")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Datos de alojamiento", description = "404 si la oferta no los tiene.")
    public AlojamientoDto alojamiento(@PathVariable Long id) {
        return servicio.alojamiento(id);
    }

    @PutMapping("/ofertas/{id}/alojamiento")
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Crea o reemplaza los datos de alojamiento")
    public AlojamientoDto guardarAlojamiento(@PathVariable Long id, @Valid @RequestBody AlojamientoDto solicitud) {
        return servicio.guardarAlojamiento(id, solicitud);
    }

    // ------------------------------------------------------------------ circuito

    @GetMapping("/ofertas/{id}/circuito")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Datos del circuito", description = "404 si la oferta no es un circuito.")
    public CircuitoDto circuito(@PathVariable Long id) {
        return servicio.circuito(id);
    }

    @PutMapping("/ofertas/{id}/circuito")
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Crea o reemplaza el circuito", description = "trazado: GeoJSON LineString.")
    public CircuitoDto guardarCircuito(@PathVariable Long id, @Valid @RequestBody CircuitoSolicitud solicitud) {
        return servicio.guardarCircuito(id, solicitud);
    }

    @GetMapping("/ofertas/{id}/circuito/paradas")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Paradas ordenadas del circuito")
    public List<ParadaDto> paradas(@PathVariable Long id) {
        return servicio.paradas(id);
    }

    @PutMapping("/ofertas/{id}/circuito/paradas")
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Reemplaza las paradas", description = "El orden es la posición en la lista.")
    public List<ParadaDto> reemplazarParadas(@PathVariable Long id,
                                             @Valid @RequestBody List<@Valid ParadaSolicitud> lista) {
        return servicio.reemplazarParadas(id, lista);
    }

    // ------------------------------------------------------------------ salones

    @GetMapping("/ofertas/{id}/salones")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Salones de la oferta", description = "Por defecto solo activos.")
    public List<SalonDto> salones(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean incluirInactivos) {
        return servicio.salones(id, incluirInactivos);
    }

    @PostMapping("/ofertas/{id}/salones")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Agrega un salón")
    public SalonDto crearSalon(@PathVariable Long id, @Valid @RequestBody SalonSolicitud solicitud) {
        return servicio.crearSalon(id, solicitud);
    }

    @PutMapping("/salones/{id}")
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Modifica un salón")
    public SalonDto modificarSalon(@PathVariable Long id, @Valid @RequestBody SalonSolicitud solicitud) {
        return servicio.modificarSalon(id, solicitud);
    }

    @DeleteMapping("/salones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Baja lógica de un salón")
    public void darDeBajaSalon(@PathVariable Long id) {
        servicio.darDeBajaSalon(id);
    }

    @GetMapping("/salones/{id}/caracteristicas")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Características del salón")
    public List<CaracteristicaAsignadaDto> caracteristicasSalon(@PathVariable Long id) {
        return servicio.caracteristicasSalon(id);
    }

    @PutMapping("/salones/{id}/caracteristicas")
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Reemplaza las características del salón")
    public List<CaracteristicaAsignadaDto> reemplazarCaracteristicasSalon(
            @PathVariable Long id, @Valid @RequestBody List<@Valid CaracteristicaAsignadaSolicitud> lista) {
        return servicio.reemplazarCaracteristicasSalon(id, lista);
    }

    // ------------------------------------------------------------------ visitas guiadas

    @GetMapping("/ofertas/{id}/visitas-guiadas")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Visitas guiadas de la oferta, con sus horarios")
    public List<VisitaGuiadaDto> visitas(@PathVariable Long id) {
        return servicio.visitas(id);
    }

    @PostMapping("/ofertas/{id}/visitas-guiadas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Agrega una visita guiada")
    public VisitaGuiadaDto crearVisita(@PathVariable Long id, @Valid @RequestBody VisitaGuiadaSolicitud solicitud) {
        return servicio.crearVisita(id, solicitud);
    }

    @PutMapping("/visitas-guiadas/{id}")
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Modifica una visita guiada")
    public VisitaGuiadaDto modificarVisita(@PathVariable Long id, @Valid @RequestBody VisitaGuiadaSolicitud solicitud) {
        return servicio.modificarVisita(id, solicitud);
    }

    @DeleteMapping("/visitas-guiadas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Elimina una visita guiada y sus horarios", description = "Para ocultarla sin borrarla, usar habilitada=false.")
    public void eliminarVisita(@PathVariable Long id) {
        servicio.eliminarVisita(id);
    }

    @GetMapping("/visitas-guiadas/{id}/horarios")
    @PreAuthorize(Roles.LECTURA)
    @Operation(summary = "Horarios de la visita guiada")
    public List<HorarioDto> horariosVisita(@PathVariable Long id) {
        return horarios.deVisita(id);
    }

    @PutMapping("/visitas-guiadas/{id}/horarios")
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Reemplaza los horarios de la visita guiada")
    public List<HorarioDto> reemplazarHorariosVisita(@PathVariable Long id,
                                                     @Valid @RequestBody List<@Valid HorarioSolicitud> lista) {
        return horarios.reemplazarDeVisita(id, lista);
    }
}
