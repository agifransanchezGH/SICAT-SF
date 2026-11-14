package ar.sf.sicat.admin.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ar.sf.sicat.admin.PruebaAbm;

/** ADR 0015: características de uso interno y notas internas del establecimiento. */
class DatosInternosTest extends PruebaAbm {

    @Test
    void caracteristicaInternaSeMarcaYSeDesmarca() throws Exception {
        String nombre = unico("Booking");
        String creada = post_("/catalogos/caracteristica",
                "{\"nombre\":\"%s\",\"grupo\":\"Plataformas de venta\",\"esPublica\":false}".formatted(nombre), 201);
        assertThat((Boolean) leer(creada, "$.esPublica")).isFalse();

        // Sin esPublica en el PUT, se conserva.
        long id = id(creada);
        String modificada = put_("/catalogos/caracteristica/" + id,
                "{\"nombre\":\"%s\",\"grupo\":\"Plataformas de venta\"}".formatted(nombre), 200);
        assertThat((Boolean) leer(modificada, "$.esPublica")).isFalse();

        String publica = put_("/catalogos/caracteristica/" + id,
                "{\"nombre\":\"%s\",\"grupo\":\"Plataformas de venta\",\"esPublica\":true}".formatted(nombre), 200);
        assertThat((Boolean) leer(publica, "$.esPublica")).isTrue();

        // Por defecto, pública.
        assertThat((Boolean) leer(post_("/catalogos/caracteristica",
                "{\"nombre\":\"%s\",\"grupo\":\"Servicios\"}".formatted(unico("Wi-Fi")), 201), "$.esPublica")).isTrue();
    }

    @Test
    void notasInternasDelEstablecimiento() throws Exception {
        long oferente = crearOferente();
        String creado = post_("/establecimientos", """
                {"oferenteId":%d,"nombre":"%s","descripcion":"Frente al río","notasInternas":"Cantidad de personal: 12"}
                """.formatted(oferente, unico("Hotel")), 201);
        long id = id(creado);
        assertThat((String) leer(get_("/establecimientos/" + id, 200), "$.notasInternas")).isEqualTo("Cantidad de personal: 12");

        String sinNotas = put_("/establecimientos/" + id, """
                {"oferenteId":%d,"nombre":"Hotel","descripcion":"Frente al río","notasInternas":"  "}
                """.formatted(oferente), 200);
        assertThat((Object) leer(sinNotas, "$.notasInternas")).isNull();
    }
}
