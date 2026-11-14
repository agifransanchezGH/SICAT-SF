package ar.sf.sicat.admin.importacion;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ar.sf.sicat.admin.comun.Roles;
import ar.sf.sicat.admin.importacion.dto.EstadoGeocodificacion;
import ar.sf.sicat.admin.importacion.dto.GrupoDuplicadosDto;
import ar.sf.sicat.admin.importacion.dto.ResultadoImportacion;
import ar.sf.sicat.admin.importacion.geo.GeocodificacionService;
import ar.sf.sicat.admin.importacion.modelo.Perfil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/admin/v1")
@Tag(name = "Importación", description = "Migración desde las planillas Excel de la Dirección de Turismo")
public class ImportacionController {

    private final ImportacionService importacion;
    private final GeocodificacionService geocodificacion;
    private final DuplicadosService duplicados;

    public ImportacionController(ImportacionService importacion, GeocodificacionService geocodificacion,
                                 DuplicadosService duplicados) {
        this.importacion = importacion;
        this.geocodificacion = geocodificacion;
        this.duplicados = duplicados;
    }

    public record PerfilDto(String archivo, String archivoEsperado, List<Perfil.HojaConfig> hojas) {
    }

    @GetMapping("/importacion/perfiles")
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Planillas que se pueden importar", description = "'archivo' es el valor para /importacion/{archivo}/...")
    public List<PerfilDto> perfiles() {
        return Arrays.stream(Perfil.values())
                .map(p -> new PerfilDto(p.ruta(), p.archivoEsperado(), p.hojas()))
                .toList();
    }

    @PostMapping(value = "/importacion/{archivo}/validar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Simula la importación sin guardar",
            description = "Devuelve, por establecimiento, si es NUEVO, DUPLICADO_EN_BASE, DUPLICADO_EN_ARCHIVO o ERROR.")
    public ResultadoImportacion validar(
            @Parameter(example = "gastronomia") @PathVariable String archivo,
            @RequestPart("archivo") MultipartFile planilla) {
        return importacion.validar(Perfil.desdeRuta(archivo), planilla);
    }

    @PostMapping(value = "/importacion/{archivo}/ejecutar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Importa los establecimientos NUEVO",
            description = "Todo en una transacción. Quedan en BORRADOR con la dirección pendiente de geocodificar. "
                    + "Los duplicados y los que tienen errores no se importan.")
    public ResultadoImportacion ejecutar(@PathVariable String archivo, @RequestPart("archivo") MultipartFile planilla) {
        return importacion.ejecutar(Perfil.desdeRuta(archivo), planilla);
    }

    @PostMapping("/importacion/geocodificar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Geocodifica en segundo plano las direcciones pendientes",
            description = "OpenStreetMap, una por segundo. Consultar el avance en GET /importacion/geocodificacion.")
    public EstadoGeocodificacion geocodificar(
            @RequestParam(defaultValue = "500") @Min(1) @Max(5000) int limite) {
        return geocodificacion.iniciar(limite);
    }

    @GetMapping("/importacion/geocodificacion")
    @PreAuthorize(Roles.ADMIN)
    @Operation(summary = "Avance de la geocodificación")
    public EstadoGeocodificacion estadoGeocodificacion() {
        return geocodificacion.estado();
    }

    @GetMapping("/establecimientos/duplicados")
    @PreAuthorize(Roles.CARGA)
    @Operation(summary = "Posibles duplicados", description = "Establecimientos activos con el mismo nombre normalizado.")
    public List<GrupoDuplicadosDto> duplicados() {
        return duplicados.buscar();
    }
}
