package ar.sf.sicat.admin.oferente;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import ar.sf.sicat.admin.PruebaAbm;

/** GET /lugares con orden=duda y señal de calidad de cada ubicación. */
class ColaUbicacionesTest extends PruebaAbm {

    private long lugar(String nombre, String estadoGeo, Double lat, Double lng, Integer altura) throws Exception {
        String coordenadas = lat == null ? "" : ",\"coordenadas\":{\"lat\":%s,\"lng\":%s}".formatted(lat, lng);
        return id(post_("/lugares", "{\"nombre\":\"%s\",\"calle\":\"Calle\",\"altura\":%s,\"estadoGeo\":\"%s\"%s}"
                .formatted(nombre, altura, estadoGeo, coordenadas), 201));
    }

    @Test
    void primeroLoMasDudosoYCadaUnoConSuCalidad() throws Exception {
        String prefijo = unico("Cola").replace(' ', '-');
        // nombres en orden alfabético inverso al de duda, para que el orden no salga por casualidad
        lugar(prefijo + " a automatico", "AUTOMATICO", -31.6301, -60.7001, 100);
        lugar(prefijo + " b pendiente", "PENDIENTE", null, null, 100);
        lugar(prefijo + " c aproximado", "APROXIMADO", -31.6302, -60.7002, 100);
        lugar(prefijo + " d no encontrado", "NO_ENCONTRADO", null, null, 100);

        String r = get_("/lugares?orden=duda&q=" + prefijo, 200);
        assertThat((List<String>) leer(r, "$.contenido[*].estadoGeo"))
                .containsExactly("NO_ENCONTRADO", "APROXIMADO", "PENDIENTE", "AUTOMATICO");
        assertThat((List<String>) leer(r, "$.contenido[*].calidad.nivel")).containsExactly("BAJA", "MEDIA", "BAJA", "ALTA");
        assertThat((String) leer(r, "$.contenido[0].calidad.motivos[0]")).contains("no encontró");
        assertThat((Integer) leer(r, "$.totalElementos")).isEqualTo(4);
    }

    @Test
    void puntoCompartidoLejanoOSinAlturaBajanLaSenal() throws Exception {
        String prefijo = unico("Senal").replace(' ', '-');
        // dos automáticos en el mismo punto: OpenStreetMap ubicó la calle, no la puerta
        lugar(prefijo + " uno", "AUTOMATICO", -31.6111, -60.6999, 10);
        lugar(prefijo + " dos", "AUTOMATICO", -31.6111, -60.6999, 20);
        // automático en Rosario (a ~150 km)
        lugar(prefijo + " lejos", "AUTOMATICO", -32.9468, -60.6393, 30);
        // verificado
        lugar(prefijo + " verificado", "VERIFICADO", -31.62, -60.71, null);

        String r = get_("/lugares?q=" + prefijo + "&sort=nombre", 200);
        // orden por nombre: dos, lejos, uno, verificado
        assertThat((List<String>) leer(r, "$.contenido[*].calidad.nivel")).containsExactly("MEDIA", "BAJA", "MEDIA", "VERIFICADA");
        assertThat((String) leer(r, "$.contenido[0].calidad.motivos[1]")).isEqualTo("Mismo punto que 1 lugar más");
        assertThat((String) leer(r, "$.contenido[1].calidad.motivos[1]")).contains("km del centro de Santa Fe");

        long id = ((Number) leer(r, "$.contenido[2].id")).longValue();
        assertThat((String) leer(get_("/lugares/" + id, 200), "$.calidad.nivel")).isEqualTo("MEDIA");
    }

    @Test
    void ordenInvalidoEs400() throws Exception {
        get_("/lugares?orden=cualquiera", 400);
    }
}
