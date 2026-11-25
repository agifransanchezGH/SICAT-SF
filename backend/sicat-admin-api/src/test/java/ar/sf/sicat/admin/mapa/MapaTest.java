package ar.sf.sicat.admin.mapa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import ar.sf.sicat.admin.PruebaAbm;

class MapaTest extends PruebaAbm {

    @Test
    void establecimientosComoGeoJson() throws Exception {
        long localidad = crearCatalogo("localidad", "\"provincia\":\"Santa Fe\"");
        long oferente = crearOferente();
        long conPunto = crearEstablecimiento(oferente, crearLugar(-31.63, -60.70, localidad));
        crearEstablecimiento(oferente, null);
        // categorías de sus ofertas activas: principal y adicional; la de una oferta dada de baja no cuenta
        long principal = crearCategoria(null);
        long adicional = crearCategoria(null);
        long deBaja = crearCategoria(null);
        long oferta = crearOferta(conPunto, principal);
        put_("/ofertas/" + oferta + "/categorias", "{\"ids\":[%d]}".formatted(adicional), 200);
        delete_("/ofertas/" + crearOferta(conPunto, deBaja), 204);

        String geo = get_("/mapa/establecimientos?localidadId=" + localidad, 200);
        assertThat((String) leer(geo, "$.type")).isEqualTo("FeatureCollection");
        assertThat((List<Integer>) leer(geo, "$.features[*].id")).containsExactly((int) conPunto);
        assertThat((List<Double>) leer(geo, "$.features[0].geometry.coordinates")).containsExactly(-60.70, -31.63);
        assertThat((String) leer(geo, "$.features[0].properties.estadoPublicacion")).isEqualTo("BORRADOR");
        assertThat((List<Integer>) leer(geo, "$.features[0].properties.categorias"))
                .containsExactlyInAnyOrder((int) principal, (int) adicional);

        assertThat((List<Object>) leer(get_("/mapa/establecimientos?localidadId=%d&estado=PUBLICADO".formatted(localidad), 200),
                "$.features")).isEmpty();
        assertThat((List<Object>) leer(get_("/mapa/establecimientos?localidadId=%d&bbox=10,10,11,11".formatted(localidad), 200),
                "$.features")).isEmpty();
    }

    @Test
    void cercanosOrdenadosPorDistancia() throws Exception {
        long oferente = crearOferente();
        // En medio del océano, lejos de los datos de otros tests.
        long cerca = crearEstablecimiento(oferente, crearLugar(-40.0, -40.0, null));
        long lejos = crearEstablecimiento(oferente, crearLugar(-40.0, -40.01, null));
        crearEstablecimiento(oferente, crearLugar(-41.0, -40.0, null));

        String geo = get_("/mapa/establecimientos/cercanos?lat=-40&lng=-40.0001&radioMetros=2000", 200);
        assertThat((List<Integer>) leer(geo, "$.features[*].id")).containsExactly((int) cerca, (int) lejos);
        assertThat((Integer) leer(geo, "$.features[0].properties.distanciaMetros")).isBetween(5, 15);
        assertThat((Integer) leer(geo, "$.features[1].properties.distanciaMetros")).isBetween(840, 860);

        get_("/mapa/establecimientos/cercanos?lat=-40&lng=-40&radioMetros=50000", 400);
        get_("/mapa/establecimientos/cercanos?lat=-95&lng=-40", 400);
    }

    @Test
    void circuitosConTrazadoYParadas() throws Exception {
        long oferente = crearOferente();
        long est = crearEstablecimiento(oferente, crearLugar(-31.63, -60.70, null));
        long parada = crearEstablecimiento(oferente, crearLugar(-31.64, -60.69, null));
        long oferta = crearOferta(est, crearCategoria(null));
        put_("/ofertas/%d/circuito".formatted(oferta), """
                {"duracionMin":90,"trazado":{"type":"LineString","coordinates":[[-60.70,-31.63],[-60.69,-31.64]]}}
                """, 200);
        put_("/ofertas/%d/circuito/paradas".formatted(oferta), "[{\"establecimientoId\":%d}]".formatted(parada), 200);

        String geo = get_("/mapa/circuitos", 200);
        String base = "$.features[?(@.id == %d)]".formatted(oferta);
        assertThat((List<String>) leer(geo, base + ".geometry.type")).containsExactly("LineString");
        assertThat((List<Integer>) leer(geo, base + ".properties.duracionMin")).containsExactly(90);
        assertThat((List<Integer>) leer(geo, base + ".properties.paradas[*].establecimientoId")).containsExactly((int) parada);
    }
}
