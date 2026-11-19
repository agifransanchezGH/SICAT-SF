package ar.sf.sicat.admin.publicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ar.sf.sicat.admin.PruebaAbm;

/** POST /establecimientos/lote/publicar y /lugares/lote/verificar: una transacción por elemento y resultado por id. */
class AccionesEnLoteTest extends PruebaAbm {

    private long completo() throws Exception {
        long e = crearEstablecimiento(crearOferente(), crearLugar(-31.63, -60.70, null));
        crearOferta(e, crearCategoria(null));
        return e;
    }

    private String estado(long id) throws Exception {
        return leer(get_("/establecimientos/" + id, 200), "$.estadoPublicacion");
    }

    private long lugar(String estadoGeo, boolean conCoordenadas) throws Exception {
        String coordenadas = conCoordenadas ? ",\"coordenadas\":{\"lat\":-31.6,\"lng\":-60.7}" : "";
        return id(post_("/lugares", "{\"nombre\":\"%s\",\"estadoGeo\":\"%s\"%s}"
                .formatted(unico("Lugar"), estadoGeo, coordenadas), 201));
    }

    @Test
    void publicarEnLoteRechazaUnoPorUnoSinRevertirALosDemas() throws Exception {
        long a = completo();
        long b = completo();
        long sinLugar = crearEstablecimiento(crearOferente(), null);
        long inexistente = 999_999_999L;

        String r = post_("/establecimientos/lote/publicar",
                "{\"ids\":[%d,%d,%d,%d,%d]}".formatted(a, sinLugar, b, inexistente, a), 200);

        assertThat((Integer) leer(r, "$.procesados")).isEqualTo(4); // el repetido se procesa una vez
        assertThat((Integer) leer(r, "$.exitosos")).isEqualTo(2);
        assertThat((Integer) leer(r, "$.fallidos")).isEqualTo(2);
        assertThat((List<Integer>) leer(r, "$.resultados[*].id"))
                .containsExactly((int) a, (int) sinLugar, (int) b, (int) inexistente);
        assertThat((List<String>) leer(r, "$.resultados[1].errores"))
                .containsExactly("Falta asignar un lugar", "Falta al menos una oferta activa con categoría activa");
        assertThat((List<String>) leer(r, "$.resultados[3].errores")).containsExactly("No existe");

        assertThat(estado(a)).isEqualTo("PUBLICADO");
        assertThat(estado(b)).isEqualTo("PUBLICADO");
        assertThat(estado(sinLugar)).isEqualTo("BORRADOR");
        // queda auditado como la publicación individual
        String historial = get_("/auditoria/establecimiento/" + b, 200);
        assertThat((String) leer(historial, "$[0].datosNuevos.estadoPublicacion")).isEqualTo("PUBLICADO");
    }

    @Test
    void verificarEnLoteSoloConfirmaAutomaticasConCoordenadas() throws Exception {
        long automatico = lugar("AUTOMATICO", true);
        long aproximado = lugar("APROXIMADO", true);
        long sinPunto = lugar("NO_ENCONTRADO", false);
        long yaVerificado = lugar("VERIFICADO", true);

        String r = post_("/lugares/lote/verificar",
                "{\"ids\":[%d,%d,%d,%d]}".formatted(automatico, aproximado, sinPunto, yaVerificado), 200);

        assertThat((Integer) leer(r, "$.exitosos")).isEqualTo(2);
        assertThat((List<Boolean>) leer(r, "$.resultados[*].ok")).containsExactly(true, false, false, true);
        assertThat((String) leer(r, "$.resultados[1].errores[0]")).contains("APROXIMADO");
        assertThat((String) leer(r, "$.resultados[2].errores[0]")).contains("No tiene coordenadas");

        assertThat((String) leer(get_("/lugares/" + automatico, 200), "$.estadoGeo")).isEqualTo("VERIFICADO");
        assertThat((String) leer(get_("/lugares/" + aproximado, 200), "$.estadoGeo")).isEqualTo("APROXIMADO");
        assertThat((String) leer(get_("/auditoria/lugar/" + automatico, 200), "$[0].datosNuevos.estadoGeo"))
                .isEqualTo("VERIFICADO");
    }

    @Test
    void validacionesYPermisos() throws Exception {
        post_("/establecimientos/lote/publicar", "{\"ids\":[]}", 400);
        post_("/lugares/lote/verificar", "{}", 400);
        String muchos = "{\"ids\":[" + "1,".repeat(500) + "1]}";
        post_("/establecimientos/lote/publicar", muchos, 400);

        String consulta = bearerDe("CONSULTA");
        for (String ruta : List.of("/establecimientos/lote/publicar", "/lugares/lote/verificar")) {
            pedir(post(API + ruta).contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[1]}"), consulta, 403);
        }
        pedir(post(API + "/establecimientos/lote/publicar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\":[%d]}".formatted(completo())), bearerDe("CARGADOR"), 200);
    }

    @Test
    void elEstadoDePublicacionTraeElCodigoDeCadaRequisito() throws Exception {
        long sinLugar = crearEstablecimiento(crearOferente(), null);
        String estado = get_("/establecimientos/" + sinLugar + "/publicacion", 200);
        assertThat((List<String>) leer(estado, "$.requisitos[*].codigo")).containsExactly("LUGAR", "OFERTA_ACTIVA");
        assertThat((String) leer(estado, "$.requisitos[0].mensaje")).isEqualTo("Falta asignar un lugar");
    }
}
