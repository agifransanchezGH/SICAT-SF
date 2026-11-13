package ar.sf.sicat.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

/** Base de los tests de ABM: atajos HTTP con el ADMIN y creación de datos de prueba por la API. */
public abstract class PruebaAbm extends PruebaIntegracion {

    private String bearer;

    protected String admin() throws Exception {
        if (bearer == null) {
            bearer = bearerAdmin();
        }
        return bearer;
    }

    protected static String unico(String prefijo) {
        return prefijo + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Las respuestas omiten campos nulos (NON_NULL): una hoja ausente se lee como null. */
    private static final Configuration HOJA_AUSENTE_ES_NULL =
            Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL);

    protected static <T> T leer(String json, String ruta) {
        return JsonPath.using(HOJA_AUSENTE_ES_NULL).parse(json).read(ruta);
    }

    protected static long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    // ------------------------------------------------------------------ HTTP

    protected String pedir(MockHttpServletRequestBuilder req, String auth, int estado) throws Exception {
        if (auth != null) {
            req.header("Authorization", auth);
        }
        return mvc.perform(req).andExpect(status().is(estado)).andReturn().getResponse().getContentAsString();
    }

    protected String get_(String url, int estado) throws Exception {
        return pedir(get(API + url), admin(), estado);
    }

    protected String post_(String url, String cuerpo, int estado) throws Exception {
        return pedir(post(API + url).contentType(MediaType.APPLICATION_JSON).content(cuerpo), admin(), estado);
    }

    protected String put_(String url, String cuerpo, int estado) throws Exception {
        return pedir(put(API + url).contentType(MediaType.APPLICATION_JSON).content(cuerpo), admin(), estado);
    }

    protected String delete_(String url, int estado) throws Exception {
        return pedir(delete(API + url), admin(), estado);
    }

    /** Bearer de un usuario nuevo con el rol indicado. */
    protected String bearerDe(String rol) throws Exception {
        String email = UUID.randomUUID().toString().substring(0, 8) + "-" + rol.toLowerCase() + "@sicat.test";
        post_("/usuarios", """
                {"nombreCompleto":"Prueba %s","email":"%s","password":"clave-segura-1","rol":"%s"}
                """.formatted(rol, email, rol), 201);
        return "Bearer " + accessToken(email, "clave-segura-1");
    }

    // ------------------------------------------------------------------ datos de prueba

    protected long crearCatalogo(String catalogo, String extra) throws Exception {
        String cuerpo = "{\"nombre\":\"%s\"%s}".formatted(unico(catalogo), extra == null ? "" : "," + extra);
        return id(post_("/catalogos/" + catalogo, cuerpo, 201));
    }

    protected long crearOferente() throws Exception {
        long tipo = crearCatalogo("tipo-oferente", null);
        return id(post_("/oferentes", "{\"nombre\":\"%s\",\"tipoOferenteId\":%d}".formatted(unico("Oferente"), tipo), 201));
    }

    protected long crearLugar(double lat, double lng, Long localidadId) throws Exception {
        return id(post_("/lugares", """
                {"nombre":"%s","calle":"San Martín","altura":2000,"localidadId":%s,
                 "coordenadas":{"lat":%s,"lng":%s}}
                """.formatted(unico("Lugar"), localidadId, lat, lng), 201));
    }

    protected long crearEstablecimiento(long oferenteId, Long lugarId) throws Exception {
        return id(post_("/establecimientos", "{\"oferenteId\":%d,\"lugarId\":%s,\"nombre\":\"%s\"}"
                .formatted(oferenteId, lugarId, unico("Establecimiento")), 201));
    }

    protected long crearCategoria(Long padreId) throws Exception {
        return id(post_("/categorias", "{\"nombre\":\"%s\",\"padreId\":%s}".formatted(unico("Categoría"), padreId), 201));
    }

    protected long crearOferta(long establecimientoId, long categoriaId) throws Exception {
        return id(post_("/establecimientos/" + establecimientoId + "/ofertas",
                "{\"categoriaId\":%d,\"acceso\":\"Libre\"}".formatted(categoriaId), 201));
    }
}
