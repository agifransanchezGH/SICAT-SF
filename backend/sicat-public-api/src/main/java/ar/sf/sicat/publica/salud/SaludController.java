package ar.sf.sicat.publica.salud;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.sf.sicat.core.vistapublica.CategoriaPublicaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/public/v1")
@Tag(name = "Salud")
public class SaludController {

    private final CategoriaPublicaRepository categorias;

    public SaludController(CategoriaPublicaRepository categorias) {
        this.categorias = categorias;
    }

    /** Verifica que la API responde y que puede leer el esquema publico. */
    @GetMapping("/health")
    @Operation(summary = "Estado del servicio")
    public Map<String, String> health() {
        categorias.count();
        return Map.of("estado", "OK");
    }
}
