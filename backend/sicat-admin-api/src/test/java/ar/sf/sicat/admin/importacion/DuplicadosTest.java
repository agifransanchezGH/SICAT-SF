package ar.sf.sicat.admin.importacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ar.sf.sicat.admin.PruebaAbm;

/** Posibles duplicados: comparación, "no son duplicados" persistente y "quedarme con este". */
class DuplicadosTest extends PruebaAbm {

    private long crearConNombre(long oferente, Long lugar, String nombre) throws Exception {
        return id(post_("/establecimientos", "{\"oferenteId\":%d,\"lugarId\":%s,\"nombre\":\"%s\"}"
                .formatted(oferente, lugar, nombre), 201));
    }

    /** Grupo cuya clave contiene el sufijo único del nombre, o null si no aparece. */
    private Map<String, Object> grupo(String sufijo) throws Exception {
        List<Map<String, Object>> grupos = leer(get_("/establecimientos/duplicados", 200), "$");
        return grupos.stream().filter(g -> ((String) g.get("clave")).contains(sufijo.toLowerCase())).findFirst().orElse(null);
    }

    private long contadorDuplicados() throws Exception {
        return ((Number) leer(get_("/contadores", 200), "$.posiblesDuplicados")).longValue();
    }

    private String cuerpoGrupo(Map<String, Object> g, long... extra) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) g.get("establecimientos");
        StringBuilder ids = new StringBuilder();
        items.forEach(i -> ids.append(ids.isEmpty() ? "" : ",").append(i.get("id")));
        for (long e : extra) {
            ids.append(",").append(e);
        }
        return "{\"clave\":\"%s\",\"integrantes\":[%s]}".formatted(g.get("clave"), ids);
    }

    @Test
    void cadaIntegranteTraeLoNecesarioParaCompararlo() throws Exception {
        String nombre = unico("Museo Comparable");
        String sufijo = nombre.substring(nombre.lastIndexOf(' ') + 1);
        long oferente = crearOferente();
        long a = crearConNombre(oferente, crearLugar(-31.63, -60.70, null), nombre);
        crearConNombre(oferente, null, nombre.toUpperCase());
        crearOferta(a, crearCategoria(null));
        long instagram = id(post_("/catalogos/tipo-canal", "{\"nombre\":\"%s\"}".formatted(unico("Instagram")), 201));
        put_("/establecimientos/" + a + "/contactos",
                "[{\"tipoCanalId\":%d,\"valor\":\"@museo\",\"esPublico\":true}]".formatted(instagram), 200);

        Map<String, Object> g = grupo(sufijo);
        assertThat(g).isNotNull();
        String json = get_("/establecimientos/duplicados", 200);
        String base = "$[?(@.clave == '" + g.get("clave") + "')].establecimientos[0]";
        assertThat((List<String>) leer(json, base + ".direccion")).containsExactly("San Martín 2000");
        assertThat((List<Integer>) leer(json, base + ".ofertasActivas")).containsExactly(1);
        assertThat((List<String>) leer(json, base + ".contactos[0].valor")).containsExactly("@museo");
        assertThat((List<List<String>>) leer(json, base + ".categorias")).singleElement().asList().hasSize(1);
        assertThat((List<String>) leer(json, base + ".oferente")).singleElement().isNotNull();
    }

    @Test
    void noSonDuplicadosPersisteHastaQueCambiaElGrupo() throws Exception {
        String nombre = unico("Sucursal");
        String sufijo = nombre.substring(nombre.lastIndexOf(' ') + 1);
        long oferente = crearOferente();
        crearConNombre(oferente, null, nombre);
        crearConNombre(oferente, null, nombre);
        Map<String, Object> g = grupo(sufijo);
        long antes = contadorDuplicados();

        // integrantes que no coinciden con el grupo actual: 409
        post_("/establecimientos/duplicados/descartes", cuerpoGrupo(g, 999_999_999L), 409);

        post_("/establecimientos/duplicados/descartes", cuerpoGrupo(g), 204);
        assertThat(grupo(sufijo)).isNull();
        assertThat(contadorDuplicados()).isEqualTo(antes - 1);
        // repetirlo no falla ni crea otro descarte
        post_("/establecimientos/duplicados/descartes", cuerpoGrupo(g), 204);
        // queda auditado
        assertThat((List<Object>) leer(get_("/auditoria?tabla=descarte_duplicado", 200), "$.contenido")).isNotEmpty();

        // entra un integrante nuevo: el grupo vuelve a aparecer
        crearConNombre(oferente, null, nombre);
        assertThat(grupo(sufijo)).isNotNull();
        assertThat(contadorDuplicados()).isEqualTo(antes);
    }

    @Test
    void quedarmeConEsteDaDeBajaALosDemasEnUnaTransaccion() throws Exception {
        String nombre = unico("Hotel Repetido");
        String sufijo = nombre.substring(nombre.lastIndexOf(' ') + 1);
        long oferente = crearOferente();
        long queda = crearConNombre(oferente, null, nombre);
        long publicado = crearConNombre(oferente, crearLugar(-31.63, -60.70, null), nombre);
        crearOferta(publicado, crearCategoria(null));
        post_("/establecimientos/" + publicado + "/publicar", "", 200);
        long tercero = crearConNombre(oferente, null, nombre);
        Map<String, Object> g = grupo(sufijo);

        // conservar uno que no es del grupo: 422
        post_("/establecimientos/duplicados/resolucion",
                "{\"conservarId\":%d,\"grupo\":%s}".formatted(999_999_999L, cuerpoGrupo(g)), 422);
        // solo ADMIN, como la baja individual
        pedir(post(API + "/establecimientos/duplicados/resolucion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"conservarId\":%d,\"grupo\":%s}".formatted(queda, cuerpoGrupo(g))), bearerDe("CARGADOR"), 403);

        String bajas = post_("/establecimientos/duplicados/resolucion",
                "{\"conservarId\":%d,\"grupo\":%s}".formatted(queda, cuerpoGrupo(g)), 200);
        assertThat((List<Integer>) leer(bajas, "$.bajas")).containsExactlyInAnyOrder((int) publicado, (int) tercero);

        assertThat((Boolean) leer(get_("/establecimientos/" + queda, 200), "$.activo")).isTrue();
        String delPublicado = get_("/establecimientos/" + publicado, 200);
        assertThat((Boolean) leer(delPublicado, "$.activo")).isFalse();
        assertThat((String) leer(delPublicado, "$.estadoPublicacion")).isEqualTo("OCULTO");
        assertThat(grupo(sufijo)).isNull();
    }

    @Test
    void deTresSoloDosSonDuplicados_elNoMarcadoNoSeTocaYNoVuelveAAparecer() throws Exception {
        String nombre = unico("Parrilla Sucursal");
        String sufijo = nombre.substring(nombre.lastIndexOf(' ') + 1);
        long oferente = crearOferente();
        long queda = crearConNombre(oferente, null, nombre);
        long repetido = crearConNombre(oferente, null, nombre);
        long sucursal = crearConNombre(oferente, null, nombre);
        long instagram = id(post_("/catalogos/tipo-canal", "{\"nombre\":\"%s\"}".formatted(unico("Instagram")), 201));
        put_("/establecimientos/" + repetido + "/contactos",
                "[{\"tipoCanalId\":%d,\"valor\":\"@repetido\",\"esPublico\":true}]".formatted(instagram), 200);
        put_("/establecimientos/" + sucursal + "/contactos",
                "[{\"tipoCanalId\":%d,\"valor\":\"@sucursal\",\"esPublico\":true}]".formatted(instagram), 200);
        Map<String, Object> g = grupo(sufijo);
        long antes = contadorDuplicados();
        String resolucion = "{\"conservarId\":%d,\"grupo\":%s,\"marcados\":%s,\"copiarContactos\":true}";

        // validaciones de los marcados: uno solo (400 por @Size), uno ajeno, o sin el que queda (422)
        post_("/establecimientos/duplicados/resolucion", resolucion.formatted(queda, cuerpoGrupo(g), "[" + queda + "]"), 400);
        post_("/establecimientos/duplicados/resolucion",
                resolucion.formatted(queda, cuerpoGrupo(g), "[%d,%d]".formatted(queda, 999_999_999L)), 422);
        post_("/establecimientos/duplicados/resolucion",
                resolucion.formatted(queda, cuerpoGrupo(g), "[%d,%d]".formatted(repetido, sucursal)), 422);

        // la vista previa solo mira a los marcados: no ofrece el contacto de la sucursal
        String previa = post_("/establecimientos/duplicados/resolucion/previa",
                resolucion.formatted(queda, cuerpoGrupo(g), "[%d,%d]".formatted(queda, repetido)), 200);
        assertThat((List<Integer>) leer(previa, "$.origenes[*].id")).containsExactly((int) repetido);
        assertThat((Integer) leer(previa, "$.contactos")).isEqualTo(1);

        String r = post_("/establecimientos/duplicados/resolucion",
                resolucion.formatted(queda, cuerpoGrupo(g), "[%d,%d]".formatted(queda, repetido)), 200);
        assertThat((List<Integer>) leer(r, "$.bajas")).containsExactly((int) repetido);
        assertThat((Integer) leer(r, "$.contactosCopiados")).isEqualTo(1);

        assertThat((Boolean) leer(get_("/establecimientos/" + repetido, 200), "$.activo")).isFalse();
        // el no marcado no se toca: sigue activo y con su contacto
        assertThat((Boolean) leer(get_("/establecimientos/" + sucursal, 200), "$.activo")).isTrue();
        assertThat((List<String>) leer(get_("/establecimientos/" + sucursal + "/contactos", 200), "$[*].valor"))
                .containsExactly("@sucursal");
        // y no vuelve a aparecer como duplicado del que quedó
        assertThat(grupo(sufijo)).isNull();
        assertThat(contadorDuplicados()).isEqualTo(antes - 1);
    }

    @Test
    void permisosYValidaciones() throws Exception {
        String consulta = bearerDe("CONSULTA");
        pedir(get(API + "/establecimientos/duplicados"), consulta, 403);
        pedir(post(API + "/establecimientos/duplicados/descartes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"clave\":\"x\",\"integrantes\":[1,2]}"), consulta, 403);
        post_("/establecimientos/duplicados/descartes", "{\"clave\":\"\",\"integrantes\":[1]}", 400);
    }
}
