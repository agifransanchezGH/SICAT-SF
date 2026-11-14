package ar.sf.sicat.admin.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;

import ar.sf.sicat.admin.PruebaAbm;

class CatalogoAbmTest extends PruebaAbm {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "tipo-oferente  |",
            "tipo-canal     |",
            "tipo-lugar     |",
            "localidad      | \"provincia\":\"Santa Fe\"",
            "distincion     | \"organismoOtorgante\":\"Ministerio\"",
            "caracteristica | \"grupo\":\"Servicios\",\"admiteCantidad\":true"})
    void abmCompletoDeCadaCatalogo(String catalogo, String extra) throws Exception {
        String sufijo = extra == null ? "" : "," + extra;
        String nombre = unico("Ítem");
        String creado = post_("/catalogos/" + catalogo, "{\"nombre\":\"%s\"%s}".formatted(nombre, sufijo), 201);
        long id = id(creado);
        assertThat((Boolean) leer(creado, "$.activo")).isTrue();

        // duplicado → 409
        post_("/catalogos/" + catalogo, "{\"nombre\":\"%s\"%s}".formatted(nombre.toUpperCase(), sufijo), 409);

        // modificar
        String renombrado = unico("Renombrado");
        String modificado = put_("/catalogos/" + catalogo + "/" + id,
                "{\"nombre\":\"%s\"%s}".formatted(renombrado, sufijo), 200);
        assertThat((String) leer(modificado, "$.nombre")).isEqualTo(renombrado);

        // listar: aparece; baja lógica: desaparece salvo con incluirInactivos
        assertThat(nombres(get_("/catalogos/" + catalogo, 200))).contains(renombrado);
        delete_("/catalogos/" + catalogo + "/" + id, 204);
        assertThat(nombres(get_("/catalogos/" + catalogo, 200))).doesNotContain(renombrado);
        assertThat(nombres(get_("/catalogos/" + catalogo + "?incluirInactivos=true", 200))).contains(renombrado);

        // reactivar
        put_("/catalogos/" + catalogo + "/" + id, "{\"nombre\":\"%s\"%s,\"activo\":true}".formatted(renombrado, sufijo), 200);
        assertThat(nombres(get_("/catalogos/" + catalogo, 200))).contains(renombrado);
    }

    private static List<String> nombres(String json) {
        return leer(json, "$[*].nombre");
    }

    @Test
    void validacionesYErrores() throws Exception {
        post_("/catalogos/localidad", "{\"nombre\":\"Sin provincia\"}", 400);
        post_("/catalogos/caracteristica", "{\"nombre\":\"Sin grupo\"}", 400);
        post_("/catalogos/tipo-oferente", "{\"nombre\":\"\"}", 400);
        get_("/catalogos/no-existe", 404);
        put_("/catalogos/tipo-canal/999999", "{\"nombre\":\"X\"}", 404);
    }

    @Test
    void consultaPuedeLeerPeroNoModificar() throws Exception {
        String consulta = bearerDe("CONSULTA");
        String cargador = bearerDe("CARGADOR");
        pedir(get(API + "/catalogos/tipo-canal"), consulta, 200);
        pedir(post(API + "/catalogos/tipo-canal").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"X\"}"), consulta, 403);
        pedir(post(API + "/catalogos/tipo-canal").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"X\"}"), cargador, 403);
        pedir(get(API + "/catalogos/tipo-canal"), null, 401);
    }

    @Test
    void arbolDeCategoriasConCiclosYBaja() throws Exception {
        long raiz = crearCategoria(null);
        long hija = crearCategoria(raiz);
        long nieta = crearCategoria(hija);

        String rama = get_("/categorias?padreId=" + raiz, 200);
        assertThat(((Number) leer(rama, "$[0].id")).longValue()).isEqualTo(hija);
        assertThat(((Number) leer(rama, "$[0].hijos[0].id")).longValue()).isEqualTo(nieta);
        String arbol = get_("/categorias", 200);
        assertThat((List<Integer>) leer(arbol, "$[*].id")).contains((int) raiz);

        // la raíz no puede quedar dentro de su nieta
        put_("/categorias/" + raiz, "{\"nombre\":\"Raíz\",\"padreId\":%d}".formatted(nieta), 422);
        put_("/categorias/" + raiz, "{\"nombre\":\"Raíz\",\"padreId\":%d}".formatted(raiz), 422);
        String modificada = put_("/categorias/" + hija, "{\"nombre\":\"Hija renombrada\",\"padreId\":null}", 200);
        assertThat((Object) leer(modificada, "$.padreId")).isNull();

        delete_("/categorias/" + nieta, 204);
        assertThat((List<Object>) leer(get_("/categorias?padreId=" + hija, 200), "$")).isEmpty();
        assertThat((List<Object>) leer(get_("/categorias?padreId=" + hija + "&incluirInactivas=true", 200), "$"))
                .hasSize(1);
    }

    @Test
    void caracteristicasDeUnaCategoria() throws Exception {
        long categoria = crearCategoria(null);
        long wifi = crearCatalogo("caracteristica", "\"grupo\":\"Servicios\"");
        long pileta = crearCatalogo("caracteristica", "\"grupo\":\"Instalaciones\"");

        String asignadas = put_("/categorias/" + categoria + "/caracteristicas",
                "{\"ids\":[%d,%d,%d]}".formatted(wifi, pileta, wifi), 200);
        assertThat((List<Object>) leer(asignadas, "$")).hasSize(2);
        assertThat((String) leer(asignadas, "$[0].grupo")).isEqualTo("Instalaciones");

        put_("/categorias/" + categoria + "/caracteristicas", "{\"ids\":[%d]}".formatted(wifi), 200);
        String leidas = get_("/categorias/" + categoria + "/caracteristicas", 200);
        assertThat((List<Integer>) leer(leidas, "$[*].id")).containsExactly((int) wifi);

        put_("/categorias/" + categoria + "/caracteristicas", "{\"ids\":[999999]}", 404);
    }
}
