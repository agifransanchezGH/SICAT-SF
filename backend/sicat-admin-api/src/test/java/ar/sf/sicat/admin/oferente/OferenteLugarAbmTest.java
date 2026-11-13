package ar.sf.sicat.admin.oferente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import java.util.List;

import org.junit.jupiter.api.Test;

import ar.sf.sicat.admin.PruebaAbm;

class OferenteLugarAbmTest extends PruebaAbm {

    @Test
    void abmDeOferentes() throws Exception {
        long tipo = crearCatalogo("tipo-oferente", null);
        String nombre = unico("Hotelera Litoral");
        String creado = post_("/oferentes", "{\"nombre\":\"%s\",\"tipoOferenteId\":%d}".formatted(nombre, tipo), 201);
        long id = id(creado);
        assertThat((List<Object>) leer(creado, "$.establecimientos")).isEmpty();

        post_("/oferentes", "{\"nombre\":\"X\",\"tipoOferenteId\":999999}", 404);
        post_("/oferentes", "{\"nombre\":\"\",\"tipoOferenteId\":%d}".formatted(tipo), 400);

        String lista = get_("/oferentes?q=" + nombre.substring(0, 12) + "&tipo=" + tipo, 200);
        assertThat((Integer) leer(lista, "$.totalElementos")).isEqualTo(1);
        assertThat((Object) leer(lista, "$.contenido[0].establecimientos")).isNull();

        put_("/oferentes/" + id, "{\"nombre\":\"Hotelera Renombrada\",\"tipoOferenteId\":%d}".formatted(tipo), 200);
        crearEstablecimiento(id, null);
        String detalle = get_("/oferentes/" + id, 200);
        assertThat((String) leer(detalle, "$.nombre")).isEqualTo("Hotelera Renombrada");
        assertThat((List<Object>) leer(detalle, "$.establecimientos")).hasSize(1);

        delete_("/oferentes/" + id, 204);
        assertThat((Integer) leer(get_("/oferentes?tipo=" + tipo, 200), "$.totalElementos")).isZero();
        assertThat((Integer) leer(get_("/oferentes?tipo=" + tipo + "&incluirInactivos=true", 200), "$.totalElementos"))
                .isEqualTo(1);
        get_("/oferentes/999999", 404);
    }

    @Test
    void soloAdminDaDeBajaOferentes() throws Exception {
        long oferente = crearOferente();
        pedir(delete(API + "/oferentes/" + oferente), bearerDe("CARGADOR"), 403);
    }

    @Test
    void contactosDeUnOferente() throws Exception {
        long oferente = crearOferente();
        long whatsapp = crearCatalogo("tipo-canal", null);
        long mail = crearCatalogo("tipo-canal", null);

        String creados = put_("/oferentes/" + oferente + "/contactos", """
                [{"tipoCanalId":%d,"valor":"+54 342 555-0000","esPublico":true},
                 {"tipoCanalId":%d,"valor":"admin@hotel.test","uso":"Administración"}]
                """.formatted(whatsapp, mail), 200);
        assertThat((List<Object>) leer(creados, "$")).hasSize(2);
        assertThat((Integer) leer(creados, "$[1].orden")).isEqualTo(2);
        assertThat((Boolean) leer(creados, "$[1].esPublico")).isFalse();
        long idWhatsapp = ((Number) leer(creados, "$[0].id")).longValue();

        // se modifica el primero, se elimina el segundo y se agrega uno nuevo
        String reemplazados = put_("/oferentes/" + oferente + "/contactos", """
                [{"id":%d,"tipoCanalId":%d,"valor":"+54 342 555-1111","esPublico":true},
                 {"tipoCanalId":%d,"valor":"reservas@hotel.test"}]
                """.formatted(idWhatsapp, whatsapp, mail), 200);
        assertThat((List<String>) leer(reemplazados, "$[*].valor"))
                .containsExactly("+54 342 555-1111", "reservas@hotel.test");
        assertThat(((Number) leer(reemplazados, "$[0].id")).longValue()).isEqualTo(idWhatsapp);
        assertThat((List<Object>) leer(get_("/oferentes/" + oferente + "/contactos", 200), "$")).hasSize(2);

        // un id de otro oferente → 422; ítem inválido → 400
        put_("/oferentes/" + crearOferente() + "/contactos",
                "[{\"id\":%d,\"tipoCanalId\":%d,\"valor\":\"x\"}]".formatted(idWhatsapp, whatsapp), 422);
        put_("/oferentes/" + oferente + "/contactos", "[{\"tipoCanalId\":%d,\"valor\":\"\"}]".formatted(whatsapp), 400);

        put_("/oferentes/" + oferente + "/contactos", "[]", 200);
        assertThat((List<Object>) leer(get_("/oferentes/" + oferente + "/contactos", 200), "$")).isEmpty();
    }

    @Test
    void abmDeLugaresConGeografia() throws Exception {
        long localidad = crearCatalogo("localidad", "\"provincia\":\"Santa Fe\"");
        long tipo = crearCatalogo("tipo-lugar", null);
        String padre = post_("/lugares", """
                {"nombre":"%s","tipoLugarId":%d,"localidadId":%d,
                 "geometria":{"type":"Polygon","coordinates":[[[-60.71,-31.64],[-60.69,-31.64],[-60.69,-31.62],[-60.71,-31.62],[-60.71,-31.64]]]}}
                """.formatted(unico("Parque"), tipo, localidad), 201);
        long idPadre = id(padre);
        assertThat((String) leer(padre, "$.geometria.type")).isEqualTo("Polygon");

        String hijo = post_("/lugares", """
                {"nombre":"Anfiteatro","padreId":%d,"localidadId":%d,"calle":"Av. Costanera","km":2.5,
                 "coordenadas":{"lat":-31.63,"lng":-60.70}}
                """.formatted(idPadre, localidad), 201);
        long idHijo = id(hijo);
        assertThat((Double) leer(hijo, "$.coordenadas.lat")).isEqualTo(-31.63);

        String detallePadre = get_("/lugares/" + idPadre, 200);
        assertThat((List<Integer>) leer(detallePadre, "$.hijos[*].id")).containsExactly((int) idHijo);

        // filtros: bbox, padre, localidad, tipo, texto
        assertThat((List<Integer>) leer(get_("/lugares?bbox=-60.75,-31.68,-60.65,-31.58&localidadId=" + localidad, 200),
                "$.contenido[*].id")).containsExactly((int) idHijo);
        assertThat((Integer) leer(get_("/lugares?bbox=10,10,11,11&localidadId=" + localidad, 200), "$.totalElementos"))
                .isZero();
        assertThat((Integer) leer(get_("/lugares?padreId=" + idPadre, 200), "$.totalElementos")).isEqualTo(1);
        assertThat((Integer) leer(get_("/lugares?tipoLugar=" + tipo, 200), "$.totalElementos")).isEqualTo(1);
        assertThat((Integer) leer(get_("/lugares?q=costanera&localidadId=" + localidad, 200), "$.totalElementos"))
                .isEqualTo(1);

        // errores
        get_("/lugares?bbox=1,2,3", 400);
        post_("/lugares", "{\"nombre\":\"X\",\"geometria\":{\"type\":\"Circle\",\"coordinates\":[]}}", 400);
        post_("/lugares", "{\"nombre\":\"X\",\"coordenadas\":{\"lat\":-95,\"lng\":0}}", 400);
        put_("/lugares/" + idPadre, "{\"nombre\":\"Parque\",\"padreId\":%d}".formatted(idHijo), 422);

        // modificar
        String modificado = put_("/lugares/" + idHijo, """
                {"nombre":"Anfiteatro municipal","padreId":%d,"coordenadas":{"lat":-31.631,"lng":-60.701}}
                """.formatted(idPadre), 200);
        assertThat((String) leer(modificado, "$.nombre")).isEqualTo("Anfiteatro municipal");
        assertThat((Object) leer(modificado, "$.localidadId")).isNull();

        // eliminar: el padre tiene hijos (409); un lugar usado por un establecimiento (409); uno libre (204)
        delete_("/lugares/" + idPadre, 409);
        crearEstablecimiento(crearOferente(), idHijo);
        delete_("/lugares/" + idHijo, 409);
        long libre = crearLugar(-31.6, -60.7, null);
        delete_("/lugares/" + libre, 204);
        get_("/lugares/" + libre, 404);
    }
}
