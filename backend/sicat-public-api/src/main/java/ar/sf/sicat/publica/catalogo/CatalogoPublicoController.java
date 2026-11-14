package ar.sf.sicat.publica.catalogo;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.sf.sicat.publica.catalogo.dto.CategoriaNodoDto;
import ar.sf.sicat.publica.catalogo.dto.LocalidadDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/public/v1")
@Tag(name = "Catálogos", description = "Valores para los filtros del sitio")
public class CatalogoPublicoController {

    private final CategoriaPublicaService servicio;

    public CatalogoPublicoController(CategoriaPublicaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/categorias")
    @Operation(summary = "Árbol de categorías activas")
    public List<CategoriaNodoDto> categorias() {
        return servicio.arbol();
    }

    @GetMapping("/localidades")
    @Operation(summary = "Localidades con establecimientos publicados")
    public List<LocalidadDto> localidades() {
        return servicio.localidades();
    }
}
