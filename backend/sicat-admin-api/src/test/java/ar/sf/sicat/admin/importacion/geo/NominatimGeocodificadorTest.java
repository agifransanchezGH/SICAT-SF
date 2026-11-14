package ar.sf.sicat.admin.importacion.geo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

/** Cliente de Nominatim contra un servidor HTTP local que simula sus respuestas. */
class NominatimGeocodificadorTest {

    private HttpServer servidor;
    private final List<String> consultas = new ArrayList<>();

    private NominatimGeocodificador iniciar(Function<String, String> respuesta) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/search", ex -> {
            String query = URLDecoder.decode(ex.getRequestURI().getRawQuery(), StandardCharsets.UTF_8);
            consultas.add(query);
            assertThat(ex.getRequestHeaders().getFirst("User-Agent")).startsWith("SICAT-SF");
            byte[] cuerpo = respuesta.apply(query).getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, cuerpo.length);
            try (OutputStream out = ex.getResponseBody()) {
                out.write(cuerpo);
            }
        });
        servidor.start();
        return new NominatimGeocodificador("http://127.0.0.1:" + servidor.getAddress().getPort(),
                "SICAT-SF/test", 0);
    }

    @AfterEach
    void detener() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private static String resultado(double lat, double lon, String tipo) {
        return "[{\"lat\":\"%s\",\"lon\":\"%s\",\"addresstype\":\"%s\"}]".formatted(lat, lon, tipo);
    }

    @Test
    void primeroBuscaPorCalleYNumeroExpandido() throws IOException {
        NominatimGeocodificador g = iniciar(q -> q.contains("street=2068 Bulevar Gálvez") ? resultado(-31.63, -60.70, "building") : "[]");
        var r = g.buscar(new Geocodificador.Consulta("Bv. Gálvez 2068", "Bv. Gálvez", 2068, "Santa Fe", "Santa Fe"));
        assertThat(r).hasValueSatisfying(x -> {
            assertThat(x.aproximado()).isFalse();
            assertThat(x.coordenadas().lat()).isEqualTo(-31.63);
        });
        assertThat(consultas).hasSize(1);
        assertThat(consultas.get(0)).contains("city=Santa Fe").contains("bounded=1");
    }

    @Test
    void siSoloEncuentraLaCalleEsAproximadoYSigueIntentando() throws IOException {
        NominatimGeocodificador g = iniciar(q -> resultado(-31.64, -60.68, "road"));
        var r = g.buscar(new Geocodificador.Consulta("Gral. Paz 6308", "Gral. Paz", 6308, "Santa Fe", "Santa Fe"));
        assertThat(r).hasValueSatisfying(x -> assertThat(x.aproximado()).isTrue());
        // estructurada, libre expandida y libre original
        assertThat(consultas).hasSize(3);
        assertThat(consultas.get(1)).contains("q=General Paz 6308, Santa Fe");
        assertThat(consultas.get(2)).contains("q=Gral. Paz 6308, Santa Fe");
    }

    @Test
    void sinNumeroUnLugarConNombreNoEsAproximado() throws IOException {
        NominatimGeocodificador g = iniciar(q -> resultado(-31.646, -60.697, "shop"));
        var r = g.buscar(new Geocodificador.Consulta("Puerto Plaza", null, null, "Santa Fe", "Santa Fe"));
        assertThat(r).hasValueSatisfying(x -> assertThat(x.aproximado()).isFalse());
        assertThat(consultas).hasSize(1);
    }

    @Test
    void noEncontrado() throws IOException {
        NominatimGeocodificador g = iniciar(q -> "[]");
        assertThat(g.buscar(new Geocodificador.Consulta("No posee oficina física", null, null, "Santa Fe", "Santa Fe")))
                .isEmpty();
    }

    @Test
    void limpiaYExpandeDirecciones() {
        assertThat(DireccionGeo.limpiar("Bv. Gálvez 1620 (Local 3)")).isEqualTo("Bulevar Gálvez 1620");
        assertThat(DireccionGeo.limpiar("Rivadavia 3396 Local 004")).isEqualTo("Rivadavia 3396");
        assertThat(DireccionGeo.limpiar("Av. Alte. Brown 5101 (Costanera Oeste)")).isEqualTo("Avenida Almirante Brown 5101");
        assertThat(DireccionGeo.limpiar("Ruta Prov. N° 1- KM 3,4")).isEqualTo("Ruta Prov. 1- KM 3,4");
        assertThat(DireccionGeo.expandir("Pje. Pasteur 3912")).isEqualTo("Pasaje Pasteur 3912");
        assertThat(DireccionGeo.expandir("Costanera")).isEqualTo("Costanera");
    }
}
