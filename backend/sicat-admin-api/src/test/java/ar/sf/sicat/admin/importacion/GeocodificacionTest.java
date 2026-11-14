package ar.sf.sicat.admin.importacion;

import static ar.sf.sicat.admin.importacion.Planillas.f;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ar.sf.sicat.admin.PruebaAbm;
import ar.sf.sicat.admin.importacion.geo.Geocodificador;
import ar.sf.sicat.core.comun.Coordenadas;

/** Geocodificación con un geocodificador simulado (no se consulta OpenStreetMap en los tests). */
@TestPropertySource(properties = "sicat.geocodificacion.pausa-ms=0")
class GeocodificacionTest extends PruebaAbm {

    @MockitoBean
    Geocodificador geocodificador;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void geocodificaPendientesYExigeVerificarAntesDePublicar() throws Exception {
        String x = UUID.randomUUID().toString().substring(0, 6);
        when(geocodificador.buscar(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(geocodificador.buscar(contains("San Martín 2068"), eq("Santa Fe"), eq("Santa Fe")))
                .thenReturn(Optional.of(new Coordenadas(-31.6469, -60.7093)));

        String r = pedir(multipart(API + "/importacion/museos/ejecutar").file(Planillas.nueva().hoja("Hoja 1",
                f("NOMBRE", "DIRECCIÓN", "ENTRADA", "VISITAS GUIADAS / TURNOS"),
                f("MUSEO ENCONTRADO " + x, "San Martín 2068", "GRATIS", "Con turno por mail"),
                f("MUSEO PERDIDO " + x, "Lugar que no existe", null, null)).archivo()), admin(), 200);
        long encontrado = ((Number) leer(r, "$.items[0].establecimientoCreadoId")).longValue();
        long perdido = ((Number) leer(r, "$.items[1].establecimientoCreadoId")).longValue();

        String iniciado = post_("/importacion/geocodificar?limite=5000", "", 202);
        assertThat((Integer) leer(iniciado, "$.total")).isGreaterThanOrEqualTo(2);
        String estado = null;
        for (int i = 0; i < 100; i++) {
            estado = get_("/importacion/geocodificacion", 200);
            if (!(Boolean) leer(estado, "$.enCurso")) {
                break;
            }
            Thread.sleep(100);
        }
        assertThat((Boolean) leer(estado, "$.enCurso")).isFalse();
        assertThat((Integer) leer(estado, "$.encontrados")).isGreaterThanOrEqualTo(1);
        assertThat(((Number) leer(estado, "$.pendientes")).longValue()).isZero();

        assertThat(estadoGeo(encontrado)).isEqualTo("AUTOMATICO");
        assertThat(estadoGeo(perdido)).isEqualTo("NO_ENCONTRADO");
        String detalle = get_("/establecimientos/" + encontrado, 200);
        assertThat((Double) leer(detalle, "$.lugar.coordenadas.lat")).isEqualTo(-31.6469);

        // la visita guiada y el acceso se importaron
        long oferta = ((Number) leer(detalle, "$.ofertas[0].id")).longValue();
        String ofertaDetalle = get_("/ofertas/" + oferta, 200);
        assertThat((String) leer(ofertaDetalle, "$.acceso")).isEqualTo("GRATIS");
        assertThat((Boolean) leer(ofertaDetalle, "$.visitasGuiadas[0].requiereReserva")).isTrue();

        // coordenadas automáticas: no se publica hasta verificarlas
        String error = post_("/establecimientos/" + encontrado + "/publicar", "", 422);
        assertThat((List<String>) leer(error, "$.errores")).singleElement().asString().contains("verificalas en el mapa");
        long lugar = ((Number) leer(detalle, "$.lugar.id")).longValue();
        put_("/lugares/" + lugar, """
                {"nombre":"San Martín 2068","coordenadas":{"lat":-31.6469,"lng":-60.7093},"estadoGeo":"VERIFICADO"}
                """, 200);
        post_("/establecimientos/" + encontrado + "/publicar", "", 200);

        // sin pendientes, una nueva corrida no procesa nada
        String vacio = post_("/importacion/geocodificar", "", 202);
        assertThat((Integer) leer(vacio, "$.total")).isZero();
    }

    private String estadoGeo(long idEstablecimiento) {
        return jdbc.queryForObject("""
                SELECT l.estado_geo FROM sicat.lugar l JOIN sicat.establecimiento e ON e.id_lugar = l.id_lugar
                WHERE e.id_establecimiento = ?""", String.class, idEstablecimiento);
    }
}
