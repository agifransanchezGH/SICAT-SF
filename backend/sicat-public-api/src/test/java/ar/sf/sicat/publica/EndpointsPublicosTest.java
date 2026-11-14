package ar.sf.sicat.publica;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * Endpoints de la API Pública sobre {@code datos-publicos.sql}. Publicados visibles: 100 (Hotel Ñandú),
 * 101 (Parrilla Don Café), 105 (Bodegón Santo Tomé), 106 (Recorrido Histórico). No visibles: 102 (borrador),
 * 103 (oculto), 104 (oferente inactivo).
 */
class EndpointsPublicosTest extends PruebaPublica {

    private static final String API = "/api/public/v1";

    @Test
    void listadoSoloPublicadosDestacadosPrimero() throws Exception {
        mvc.perform(get(API + "/establecimientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(4))
                .andExpect(jsonPath("$.contenido[0].nombre").value("Hotel Ñandú"))
                .andExpect(jsonPath("$.contenido[0].destacado").value(true))
                .andExpect(jsonPath("$.contenido[0].direccion").value("Av. Costanera 1500"))
                .andExpect(jsonPath("$.contenido[0].categorias", containsInAnyOrder("Hotel", "Salones de eventos")))
                .andExpect(jsonPath("$.contenido[*].id", not(hasItem(102))))
                .andExpect(jsonPath("$.contenido[*].id", not(hasItem(104))));
    }

    @Test
    void buscadorSinAcentosEnNombreYCategorias() throws Exception {
        mvc.perform(get(API + "/establecimientos").param("q", "nandu"))
                .andExpect(jsonPath("$.contenido[*].id", contains(100)));
        mvc.perform(get(API + "/establecimientos").param("q", "PARRILLA cafe"))
                .andExpect(jsonPath("$.contenido[*].id", contains(101)));
        mvc.perform(get(API + "/establecimientos").param("q", "gastronomía"))
                .andExpect(jsonPath("$.contenido[*].id", contains(105)));
        mvc.perform(get(API + "/establecimientos").param("q", "salones"))
                .andExpect(jsonPath("$.contenido[*].id", contains(100)));
        mvc.perform(get(API + "/establecimientos").param("q", "borrador"))
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void filtrosPorCategoriaLocalidadYZona() throws Exception {
        // Gastronomía incluye su subcategoría Parrilla
        mvc.perform(get(API + "/establecimientos").param("categoria", "100").param("sort", "nombre"))
                .andExpect(jsonPath("$.contenido[*].id", contains(105, 101)));
        // categoría adicional
        mvc.perform(get(API + "/establecimientos").param("categoria", "104"))
                .andExpect(jsonPath("$.contenido[*].id", contains(100)));
        mvc.perform(get(API + "/establecimientos").param("localidad", "101"))
                .andExpect(jsonPath("$.contenido[*].id", contains(105)));
        mvc.perform(get(API + "/establecimientos").param("bbox", "-60.71,-31.64,-60.69,-31.62"))
                .andExpect(jsonPath("$.contenido[*].id", contains(100)));
        mvc.perform(get(API + "/establecimientos").param("bbox", "1,2,3"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(API + "/establecimientos").param("sort", "noExiste"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fichaCompletaSinDatosInternos() throws Exception {
        mvc.perform(get(API + "/establecimientos/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Hotel Ñandú"))
                .andExpect(jsonPath("$.oferente").value("Hotelera del Litoral"))
                .andExpect(jsonPath("$.localidad").value("Santa Fe"))
                .andExpect(jsonPath("$.coordenadas.lat").value(-31.63))
                .andExpect(jsonPath("$.contactos.length()").value(2))
                .andExpect(jsonPath("$.contactos[*].valor", containsInAnyOrder("@hotelnandu", "342 111-1111")))
                .andExpect(jsonPath("$.contactos[?(@.valor == '@hotelnandu')].uso").value("Reservas"))
                .andExpect(jsonPath("$.caracteristicas[0].grupo").value("Capacidad"))
                .andExpect(jsonPath("$.caracteristicas[0].cantidad").value(40))
                .andExpect(jsonPath("$.distinciones[0].anio").value(2024))
                .andExpect(jsonPath("$.ofertas[0].categoria").value("Hotel"))
                .andExpect(jsonPath("$.ofertas[0].categoriasAdicionales[0]").value("Salones de eventos"))
                .andExpect(jsonPath("$.ofertas[0].horarios[0].desde").value("08:00"))
                .andExpect(jsonPath("$.ofertas[0].detalle.alojamiento.estrellas").value(4))
                .andExpect(jsonPath("$.ofertas[0].detalle.alojamiento.personalPermanente").doesNotExist())
                .andExpect(jsonPath("$.ofertas[0].detalle.salones.length()").value(1))
                .andExpect(jsonPath("$.ofertas[0].detalle.salones[0].estadoConservacion").doesNotExist());
    }

    @Test
    void fichaConEstadoYCircuito() throws Exception {
        mvc.perform(get(API + "/establecimientos/101"))
                .andExpect(jsonPath("$.estadoFuncionamiento").value("Cerrado temporalmente"))
                .andExpect(jsonPath("$.motivoEstado").value("Refacción"))
                .andExpect(jsonPath("$.fechaReapertura").value("2027-03-01"))
                .andExpect(jsonPath("$.ofertas.length()").value(1))
                .andExpect(jsonPath("$.ofertas[0].horarios[0].hasta").value("02:00"));

        mvc.perform(get(API + "/establecimientos/106"))
                .andExpect(jsonPath("$.ofertas[0].detalle.circuito.duracionMin").value(90))
                .andExpect(jsonPath("$.ofertas[0].detalle.circuito.trazado.type").value("LineString"))
                .andExpect(jsonPath("$.ofertas[0].detalle.circuito.paradas[*].establecimientoId", contains(100, 101)))
                .andExpect(jsonPath("$.ofertas[0].detalle.visitasGuiadas.length()").value(1))
                .andExpect(jsonPath("$.ofertas[0].detalle.visitasGuiadas[0].horarios[0].desde").value("10:00"));
    }

    @Test
    void fichaNoPublicadaDa404() throws Exception {
        for (int id : new int[] {102, 103, 104, 999}) {
            mvc.perform(get(API + "/establecimientos/" + id)).andExpect(status().isNotFound());
        }
    }

    @Test
    void cercanosOrdenadosPorDistancia() throws Exception {
        mvc.perform(get(API + "/establecimientos/cercanos")
                        .param("lat", "-31.631").param("lng", "-60.700").param("radioMetros", "3000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(100, 101, 106)))
                .andExpect(jsonPath("$[0].distanciaMetros").value(111));
        mvc.perform(get(API + "/establecimientos/cercanos")
                        .param("lat", "-31.631").param("lng", "-60.700").param("radioMetros", "15000").param("limite", "2"))
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get(API + "/establecimientos/cercanos").param("lat", "-95").param("lng", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(API + "/establecimientos/cercanos").param("lat", "-31").param("lng", "-60")
                        .param("radioMetros", "999999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void categoriasYLocalidades() throws Exception {
        mvc.perform(get(API + "/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", contains("Alojamiento", "Circuitos", "Gastronomía", "Salones de eventos")))
                .andExpect(jsonPath("$[2].hijos[0].nombre").value("Parrilla"));
        mvc.perform(get(API + "/localidades"))
                .andExpect(jsonPath("$[*].nombre", contains("Santa Fe", "Santo Tomé")));
    }

    @Test
    void mapaEnGeoJson() throws Exception {
        mvc.perform(get(API + "/mapa").param("categoria", "102"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.features.length()").value(1))
                .andExpect(jsonPath("$.features[0].geometry.type").value("Point"))
                .andExpect(jsonPath("$.features[0].geometry.coordinates[0]").value(-60.7))
                .andExpect(jsonPath("$.features[0].properties.nombre").value("Hotel Ñandú"));
        mvc.perform(get(API + "/mapa"))
                .andExpect(jsonPath("$.features.length()").value(4));
        mvc.perform(get(API + "/mapa/circuitos"))
                .andExpect(jsonPath("$.features.length()").value(1))
                .andExpect(jsonPath("$.features[0].geometry.type").value("LineString"))
                .andExpect(jsonPath("$.features[0].properties.establecimientoId").value(106))
                .andExpect(jsonPath("$.features[0].properties.paradas.length()").value(2));
    }
}
