package ar.sf.sicat.admin.publicacion;

import static ar.sf.sicat.admin.BaseDeDatosPrueba.POSTGIS;
import static ar.sf.sicat.admin.BaseDeDatosPrueba.PUBLIC_PASS;
import static ar.sf.sicat.admin.BaseDeDatosPrueba.PUBLIC_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import ar.sf.sicat.admin.PruebaAbm;

class PublicacionTest extends PruebaAbm {

    /** Establecimiento que cumple todo: lugar con coordenadas y una oferta activa. */
    private record Completo(long oferente, long lugar, long establecimiento, long categoria, long oferta) {
    }

    private Completo completo() throws Exception {
        long oferente = crearOferente();
        long lugar = crearLugar(-31.63, -60.70, null);
        long establecimiento = crearEstablecimiento(oferente, lugar);
        long categoria = crearCategoria(null);
        long oferta = crearOferta(establecimiento, categoria);
        return new Completo(oferente, lugar, establecimiento, categoria, oferta);
    }

    private boolean esPublico(long idEstablecimiento) throws Exception {
        try (Connection ro = DriverManager.getConnection(POSTGIS.getJdbcUrl(), PUBLIC_USER, PUBLIC_PASS);
             PreparedStatement ps = ro.prepareStatement(
                     "SELECT 1 FROM publico.v_establecimiento_publico WHERE id_establecimiento = ?")) {
            ps.setLong(1, idEstablecimiento);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Test
    void publicarYDespublicarControlaLaVistaPublica() throws Exception {
        Completo c = completo();
        String estado = get_("/establecimientos/" + c.establecimiento() + "/publicacion", 200);
        assertThat((Boolean) leer(estado, "$.puedePublicarse")).isTrue();
        assertThat((String) leer(estado, "$.estado")).isEqualTo("BORRADOR");
        assertThat(esPublico(c.establecimiento())).isFalse();

        // despublicar algo que nunca se publicó
        post_("/establecimientos/" + c.establecimiento() + "/despublicar", "", 422);

        String publicado = post_("/establecimientos/" + c.establecimiento() + "/publicar", "", 200);
        assertThat((String) leer(publicado, "$.estadoPublicacion")).isEqualTo("PUBLICADO");
        assertThat(esPublico(c.establecimiento())).isTrue();
        post_("/establecimientos/" + c.establecimiento() + "/publicar", "", 200);

        String oculto = post_("/establecimientos/" + c.establecimiento() + "/despublicar", "", 200);
        assertThat((String) leer(oculto, "$.estadoPublicacion")).isEqualTo("OCULTO");
        assertThat(esPublico(c.establecimiento())).isFalse();

        String historial = get_("/auditoria/establecimiento/" + c.establecimiento(), 200);
        assertThat((String) leer(historial, "$[0].datosNuevos.estadoPublicacion")).isEqualTo("OCULTO");
        assertThat((String) leer(historial, "$[1].datosNuevos.estadoPublicacion")).isEqualTo("PUBLICADO");
    }

    @Test
    void noSePublicaSinLosRequisitos() throws Exception {
        long oferente = crearOferente();
        long sinNada = crearEstablecimiento(oferente, null);
        String estado = get_("/establecimientos/" + sinNada + "/publicacion", 200);
        assertThat((Boolean) leer(estado, "$.puedePublicarse")).isFalse();
        assertThat((List<String>) leer(estado, "$.faltantes")).hasSize(2);

        String error = post_("/establecimientos/" + sinNada + "/publicar", "", 422);
        assertThat((List<String>) leer(error, "$.errores"))
                .containsExactly("Falta asignar un lugar", "Falta al menos una oferta activa con categoría activa");

        long lugarSinCoordenadas = id(post_("/lugares", "{\"nombre\":\"Sin punto\"}", 201));
        long conLugarSinPunto = crearEstablecimiento(oferente, lugarSinCoordenadas);
        crearOferta(conLugarSinPunto, crearCategoria(null));
        String error2 = post_("/establecimientos/" + conLugarSinPunto + "/publicar", "", 422);
        assertThat((List<String>) leer(error2, "$.errores")).singleElement().asString().contains("no tiene coordenadas");

        // oferta con categoría inactiva no cuenta
        Completo c = completo();
        delete_("/categorias/" + c.categoria(), 204);
        post_("/establecimientos/" + c.establecimiento() + "/publicar", "", 422);
    }

    @Test
    void unPublicadoNoPuedeQuedarIncompleto() throws Exception {
        Completo c = completo();
        post_("/establecimientos/" + c.establecimiento() + "/publicar", "", 200);

        // quitarle el lugar
        String error = put_("/establecimientos/" + c.establecimiento(),
                "{\"oferenteId\":%d,\"nombre\":\"Sin lugar\"}".formatted(c.oferente()), 422);
        assertThat((List<String>) leer(error, "$.errores")).singleElement().asString().contains("Falta asignar un lugar");
        // quitarle las coordenadas al lugar
        put_("/lugares/" + c.lugar(), "{\"nombre\":\"Lugar sin punto\"}", 422);
        // dar de baja su única oferta (por DELETE o por PUT)
        delete_("/ofertas/" + c.oferta(), 422);
        put_("/ofertas/" + c.oferta(), "{\"categoriaId\":%d,\"activo\":false}".formatted(c.categoria()), 422);
        // dar de baja su categoría o su oferente
        delete_("/categorias/" + c.categoria(), 422);
        delete_("/oferentes/" + c.oferente(), 422);

        // nada cambió: sigue publicado y visible
        String detalle = get_("/establecimientos/" + c.establecimiento(), 200);
        assertThat((String) leer(detalle, "$.estadoPublicacion")).isEqualTo("PUBLICADO");
        assertThat((Integer) leer(detalle, "$.lugar.id")).isEqualTo((int) c.lugar());
        assertThat(esPublico(c.establecimiento())).isTrue();

        // con otra oferta válida sí se puede dar de baja la primera
        crearOferta(c.establecimiento(), crearCategoria(null));
        delete_("/ofertas/" + c.oferta(), 204);

        // despublicado, los cambios se aceptan
        post_("/establecimientos/" + c.establecimiento() + "/despublicar", "", 200);
        put_("/establecimientos/" + c.establecimiento(),
                "{\"oferenteId\":%d,\"nombre\":\"Sin lugar\"}".formatted(c.oferente()), 200);
    }

    @Test
    void laBajaDeUnPublicadoLoPasaAOculto() throws Exception {
        Completo c = completo();
        post_("/establecimientos/" + c.establecimiento() + "/publicar", "", 200);
        delete_("/establecimientos/" + c.establecimiento(), 204);
        String detalle = get_("/establecimientos/" + c.establecimiento(), 200);
        assertThat((String) leer(detalle, "$.estadoPublicacion")).isEqualTo("OCULTO");
        assertThat((Boolean) leer(detalle, "$.activo")).isFalse();

        // reactivarlo no lo vuelve a publicar
        Completo d = completo();
        post_("/establecimientos/" + d.establecimiento() + "/publicar", "", 200);
        put_("/establecimientos/" + d.establecimiento(), "{\"oferenteId\":%d,\"lugarId\":%d,\"nombre\":\"X\",\"activo\":false}"
                .formatted(d.oferente(), d.lugar()), 200);
        String reactivado = put_("/establecimientos/" + d.establecimiento(),
                "{\"oferenteId\":%d,\"lugarId\":%d,\"nombre\":\"X\",\"activo\":true}".formatted(d.oferente(), d.lugar()), 200);
        assertThat((String) leer(reactivado, "$.estadoPublicacion")).isEqualTo("OCULTO");
    }

    @Test
    void permisos() throws Exception {
        Completo c = completo();
        String consulta = bearerDe("CONSULTA");
        String cargador = bearerDe("CARGADOR");
        pedir(get(API + "/establecimientos/" + c.establecimiento() + "/publicacion"), consulta, 200);
        pedir(post(API + "/establecimientos/" + c.establecimiento() + "/publicar"), consulta, 403);
        pedir(post(API + "/establecimientos/" + c.establecimiento() + "/publicar"), cargador, 200);
        pedir(post(API + "/establecimientos/" + c.establecimiento() + "/despublicar"), cargador, 200);
        post_("/establecimientos/999999/publicar", "", 404);
    }
}
