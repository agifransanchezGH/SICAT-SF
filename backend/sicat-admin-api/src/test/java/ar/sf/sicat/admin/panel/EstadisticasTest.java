package ar.sf.sicat.admin.panel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;

import org.junit.jupiter.api.Test;

import ar.sf.sicat.admin.PruebaAbm;

/** GET /estadisticas: cifras del tablero. La base es compartida entre tests: se miden diferencias. */
class EstadisticasTest extends PruebaAbm {

    private long numero(String json, String ruta) {
        return ((Number) leer(json, ruta)).longValue();
    }

    @Test
    void cadaCifraSigueSuCriterio() throws Exception {
        String antes = get_("/estadisticas", 200);

        long oferente = crearOferente();
        long raiz = crearCategoria(null);
        long sub = crearCategoria(raiz);
        long otraRaiz = crearCategoria(null);
        // uno publicado con una oferta en la subcategoría
        long publicado = crearEstablecimiento(oferente, crearLugar(-31.63, -60.70, null));
        crearOferta(publicado, sub);
        post_("/establecimientos/" + publicado + "/publicar", "", 200);
        // un borrador con una oferta en la raíz y la otra raíz como categoría adicional
        long borrador = crearEstablecimiento(oferente, null);
        long oferta = crearOferta(borrador, raiz);
        put_("/ofertas/" + oferta + "/categorias", "{\"ids\":[%d]}".formatted(otraRaiz), 200);
        // uno dado de baja no cuenta en nada
        long baja = crearEstablecimiento(oferente, null);
        crearOferta(baja, sub);
        delete_("/establecimientos/" + baja, 204);

        String despues = get_("/estadisticas", 200);
        assertThat(numero(despues, "$.establecimientos.activos")).isEqualTo(numero(antes, "$.establecimientos.activos") + 2);
        assertThat(numero(despues, "$.establecimientos.publicados")).isEqualTo(numero(antes, "$.establecimientos.publicados") + 1);
        assertThat(numero(despues, "$.establecimientos.borradores")).isEqualTo(numero(antes, "$.establecimientos.borradores") + 1);
        assertThat(numero(despues, "$.establecimientos.ocultos")).isEqualTo(numero(antes, "$.establecimientos.ocultos"));
        assertThat(numero(despues, "$.oferentes")).isEqualTo(numero(antes, "$.oferentes") + 1);
        assertThat(numero(despues, "$.ofertasActivas")).isEqualTo(numero(antes, "$.ofertasActivas") + 2);

        // la raíz cuenta su rama (el de la subcategoría y el suyo); la subcategoría, solo el suyo; la adicional también cuenta
        String porId = "$.categorias[?(@.id == %d)]";
        assertThat((List<Integer>) leer(despues, porId.formatted(raiz) + ".establecimientos")).containsExactly(2);
        assertThat((List<Integer>) leer(despues, porId.formatted(raiz) + ".subcategorias[0].establecimientos")).containsExactly(1);
        assertThat((List<Integer>) leer(despues, porId.formatted(raiz) + ".subcategorias[0].id")).containsExactly((int) sub);
        assertThat((List<Integer>) leer(despues, porId.formatted(otraRaiz) + ".establecimientos")).containsExactly(1);
        // las subcategorías no aparecen como raíces
        assertThat((List<Object>) leer(despues, porId.formatted(sub))).isEmpty();

        // los pendientes son los mismos números que los contadores del menú
        String contadores = get_("/contadores", 200);
        assertThat(numero(despues, "$.pendientes.ubicacionesARevisar")).isEqualTo(numero(contadores, "$.ubicacionesARevisar"));
        assertThat(numero(despues, "$.pendientes.posiblesDuplicados")).isEqualTo(numero(contadores, "$.posiblesDuplicados"));
        assertThat(numero(despues, "$.pendientes.listosParaPublicar")).isEqualTo(numero(contadores, "$.listosParaPublicar"));
        assertThat(numero(despues, "$.pendientes.contactosARevisar")).isEqualTo(numero(contadores, "$.contactosARevisar"));
    }

    @Test
    void cualquierUsuarioLasLeeYSinSesionNo() throws Exception {
        pedir(get(API + "/estadisticas"), bearerDe("CONSULTA"), 200);
        pedir(get(API + "/estadisticas"), null, 401);
    }
}
