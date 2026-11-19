package ar.sf.sicat.admin.panel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;

import ar.sf.sicat.admin.PruebaAbm;

/** GET /contadores: cada contador sube cuando aparece un pendiente de su tipo. La base es compartida: se miden diferencias. */
class ContadoresTest extends PruebaAbm {

    private record Contadores(long ubicaciones, long listos, long duplicados, long contactos) {
    }

    private Contadores leerContadores() throws Exception {
        String json = get_("/contadores", 200);
        return new Contadores(((Number) leer(json, "$.ubicacionesARevisar")).longValue(),
                ((Number) leer(json, "$.listosParaPublicar")).longValue(),
                ((Number) leer(json, "$.posiblesDuplicados")).longValue(),
                ((Number) leer(json, "$.contactosARevisar")).longValue());
    }

    @Test
    void cadaContadorSigueSuCriterio() throws Exception {
        Contadores antes = leerContadores();

        // 1 ubicación a revisar
        post_("/lugares", "{\"nombre\":\"%s\",\"estadoGeo\":\"PENDIENTE\"}".formatted(unico("Lugar")), 201);
        // 1 listo para publicar (lugar con coordenadas cargadas a mano y una oferta activa)
        long oferente = crearOferente();
        long listo = crearEstablecimiento(oferente, crearLugar(-31.63, -60.70, null));
        crearOferta(listo, crearCategoria(null));
        // 1 grupo de duplicados: dos activos con el mismo nombre (sin importar mayúsculas)
        String nombre = unico("Duplicado");
        for (String n : new String[] {nombre, nombre.toUpperCase()}) {
            post_("/establecimientos", "{\"oferenteId\":%d,\"nombre\":\"%s\"}".formatted(oferente, n), 201);
        }
        // 1 con contactos a revisar: Instagram que no es un usuario ni una URL
        long instagram = id(post_("/catalogos/tipo-canal", "{\"nombre\":\"%s\"}".formatted(unico("Instagram")), 201));
        long conContacto = crearEstablecimiento(oferente, null);
        put_("/establecimientos/" + conContacto + "/contactos",
                "[{\"tipoCanalId\":%d,\"valor\":\"mi local en instagram\"}]".formatted(instagram), 200);

        Contadores despues = leerContadores();
        assertThat(despues.ubicaciones()).isEqualTo(antes.ubicaciones() + 1);
        assertThat(despues.listos()).isEqualTo(antes.listos() + 1);
        assertThat(despues.duplicados()).isEqualTo(antes.duplicados() + 1);
        assertThat(despues.contactos()).isEqualTo(antes.contactos() + 1);

        // publicarlo lo saca de "listos"; darlo de baja saca al del contacto
        post_("/establecimientos/" + listo + "/publicar", "", 200);
        delete_("/establecimientos/" + conContacto, 204);
        Contadores fin = leerContadores();
        assertThat(fin.listos()).isEqualTo(antes.listos());
        assertThat(fin.contactos()).isEqualTo(antes.contactos());
    }

    @Test
    void cualquierUsuarioLosLeeYSinSesionNo() throws Exception {
        pedir(get(API + "/contadores"), bearerDe("CONSULTA"), 200);
        pedir(get(API + "/contadores"), null, 401);
    }
}
