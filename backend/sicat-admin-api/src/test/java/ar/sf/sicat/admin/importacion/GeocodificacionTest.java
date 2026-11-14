package ar.sf.sicat.admin.importacion;

import static ar.sf.sicat.admin.importacion.Planillas.f;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ar.sf.sicat.admin.PruebaAbm;
import ar.sf.sicat.admin.importacion.geo.Geocodificador;
import ar.sf.sicat.admin.importacion.geo.Geocodificador.Resultado;
import ar.sf.sicat.core.comun.Coordenadas;

/** Geocodificación con un geocodificador simulado (no se consulta OpenStreetMap en los tests). */
class GeocodificacionTest extends PruebaAbm {

    @MockitoBean
    Geocodificador geocodificador;

    @Autowired
    JdbcTemplate jdbc;

    /** Respuestas simuladas según la dirección consultada. */
    private void simular(boolean encontrarElPerdido) {
        doAnswer(inv -> {
            Geocodificador.Consulta c = inv.getArgument(0);
            if (c.direccion().contains("San Martín 2068")) {
                assertThat(c.calle()).isEqualTo("San Martín");
                assertThat(c.altura()).isEqualTo(2068);
                return Optional.of(new Resultado(new Coordenadas(-31.6469, -60.7093), false));
            }
            if (c.direccion().contains("Gálvez")) {
                return Optional.of(new Resultado(new Coordenadas(-31.6393, -60.6846), true));
            }
            if (encontrarElPerdido && c.direccion().contains("Lugar que no existe")) {
                return Optional.of(new Resultado(new Coordenadas(-31.62, -60.70), false));
            }
            return Optional.empty();
        }).when(geocodificador).buscar(any());
    }

    private String correr(String parametros) throws Exception {
        post_("/importacion/geocodificar?limite=5000" + parametros, "", 202);
        String estado = null;
        for (int i = 0; i < 100; i++) {
            estado = get_("/importacion/geocodificacion", 200);
            if (!(Boolean) leer(estado, "$.enCurso")) {
                return estado;
            }
            Thread.sleep(100);
        }
        return estado;
    }

    @Test
    void geocodificaReintentaYExigeVerificarAntesDePublicar() throws Exception {
        String x = UUID.randomUUID().toString().substring(0, 6);
        simular(false);
        String r = pedir(multipart(API + "/importacion/museos/ejecutar").file(Planillas.nueva().hoja("Hoja 1",
                f("NOMBRE", "DIRECCIÓN", "ENTRADA", "VISITAS GUIADAS / TURNOS"),
                f("MUSEO ENCONTRADO " + x, "San Martín 2068", "GRATIS", "Con turno por mail"),
                f("MUSEO PERDIDO " + x, "Lugar que no existe", null, null),
                f("MUSEO EN BULEVAR " + x, "Bv. Gálvez 1620", null, null)).archivo()), admin(), 200);
        long encontrado = ((Number) leer(r, "$.items[0].establecimientoCreadoId")).longValue();
        long perdido = ((Number) leer(r, "$.items[1].establecimientoCreadoId")).longValue();
        long bulevar = ((Number) leer(r, "$.items[2].establecimientoCreadoId")).longValue();

        String estado = correr("");
        assertThat((Boolean) leer(estado, "$.enCurso")).isFalse();
        assertThat((Integer) leer(estado, "$.encontrados")).isGreaterThanOrEqualTo(1);
        assertThat((Integer) leer(estado, "$.aproximados")).isGreaterThanOrEqualTo(1);
        assertThat(((Number) leer(estado, "$.pendientes")).longValue()).isZero();
        assertThat(estadoGeo(encontrado)).isEqualTo("AUTOMATICO");
        assertThat(estadoGeo(perdido)).isEqualTo("NO_ENCONTRADO");
        assertThat(estadoGeo(bulevar)).isEqualTo("APROXIMADO");

        String detalle = get_("/establecimientos/" + encontrado, 200);
        assertThat((Double) leer(detalle, "$.lugar.coordenadas.lat")).isEqualTo(-31.6469);
        long oferta = ((Number) leer(detalle, "$.ofertas[0].id")).longValue();
        String ofertaDetalle = get_("/ofertas/" + oferta, 200);
        assertThat((String) leer(ofertaDetalle, "$.acceso")).isEqualTo("GRATIS");
        assertThat((Boolean) leer(ofertaDetalle, "$.visitasGuiadas[0].requiereReserva")).isTrue();

        // coordenadas automáticas o aproximadas: no se publica hasta verificarlas
        String error = post_("/establecimientos/" + encontrado + "/publicar", "", 422);
        assertThat((List<String>) leer(error, "$.errores")).singleElement().asString().contains("verificalas en el mapa");
        post_("/establecimientos/" + bulevar + "/publicar", "", 422);
        long lugar = ((Number) leer(detalle, "$.lugar.id")).longValue();
        put_("/lugares/" + lugar, """
                {"nombre":"San Martín 2068","coordenadas":{"lat":-31.6469,"lng":-60.7093},"estadoGeo":"VERIFICADO"}
                """, 200);
        post_("/establecimientos/" + encontrado + "/publicar", "", 200);

        // reintentar los no encontrados (ahora el geocodificador sí lo encuentra)
        simular(true);
        correr("&estados=NO_ENCONTRADO");
        assertThat(estadoGeo(perdido)).isEqualTo("AUTOMATICO");

        // reprocesar todo lo automático nunca toca lo verificado
        correr("&estados=AUTOMATICO,APROXIMADO");
        assertThat(estadoGeo(encontrado)).isEqualTo("VERIFICADO");

        post_("/importacion/geocodificar?estados=VERIFICADO", "", 400);
    }

    private String estadoGeo(long idEstablecimiento) {
        return jdbc.queryForObject("""
                SELECT l.estado_geo FROM sicat.lugar l JOIN sicat.establecimiento e ON e.id_lugar = l.id_lugar
                WHERE e.id_establecimiento = ?""", String.class, idEstablecimiento);
    }
}
