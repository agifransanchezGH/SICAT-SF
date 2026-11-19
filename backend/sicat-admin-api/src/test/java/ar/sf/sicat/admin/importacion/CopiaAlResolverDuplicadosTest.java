package ar.sf.sicat.admin.importacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import ar.sf.sicat.admin.PruebaAbm;

/** "Quedarme con este" copiando al que queda los contactos y características que le faltan, sin duplicarlos. */
class CopiaAlResolverDuplicadosTest extends PruebaAbm {

    private record Caso(String clave, long queda, long otro, long tercero, long caracteristicaNueva, String grupo) {
    }

    private long tipoCanal(String nombre) throws Exception {
        return id(post_("/catalogos/tipo-canal", "{\"nombre\":\"%s\"}".formatted(unico(nombre)), 201));
    }

    private long caracteristica() throws Exception {
        return id(post_("/catalogos/caracteristica",
                "{\"nombre\":\"%s\",\"grupo\":\"Servicios\"}".formatted(unico("Wifi")), 201));
    }

    private void contactos(long est, String json) throws Exception {
        put_("/establecimientos/" + est + "/contactos", json, 200);
    }

    /**
     * Queda: teléfono "342 456-7890" e Instagram "@museo", característica compartida.
     * Otro: el mismo teléfono y el mismo Instagram con otro formato, una web nueva y una característica nueva.
     * Tercero: la misma web con https, y la misma característica nueva.
     * Lo que falta, sin repetir: 1 contacto (la web) y 1 característica.
     */
    private Caso caso() throws Exception {
        String nombre = unico("Museo Copia");
        long oferente = crearOferente();
        long[] ids = new long[3];
        for (int i = 0; i < 3; i++) {
            ids[i] = id(post_("/establecimientos", "{\"oferenteId\":%d,\"nombre\":\"%s\"}".formatted(oferente, nombre), 201));
        }
        long telefono = tipoCanal("Teléfono");
        long instagram = tipoCanal("Instagram");
        long web = tipoCanal("Web");
        contactos(ids[0], """
                [{"tipoCanalId":%d,"valor":"342 456-7890","esPublico":true},{"tipoCanalId":%d,"valor":"@museo","esPublico":true}]
                """.formatted(telefono, instagram));
        contactos(ids[1], """
                [{"tipoCanalId":%d,"valor":"3424567890"},{"tipoCanalId":%d,"valor":"https://www.instagram.com/museo/"},
                 {"tipoCanalId":%d,"valor":"museo.gob.ar","esPublico":true,"uso":"Reservas"}]
                """.formatted(telefono, instagram, web));
        contactos(ids[2], "[{\"tipoCanalId\":%d,\"valor\":\"https://museo.gob.ar\"}]".formatted(web));

        long compartida = caracteristica();
        long nueva = caracteristica();
        put_("/establecimientos/" + ids[0] + "/caracteristicas", "[{\"caracteristicaId\":%d}]".formatted(compartida), 200);
        put_("/establecimientos/" + ids[1] + "/caracteristicas",
                "[{\"caracteristicaId\":%d},{\"caracteristicaId\":%d}]".formatted(compartida, nueva), 200);
        put_("/establecimientos/" + ids[2] + "/caracteristicas", "[{\"caracteristicaId\":%d}]".formatted(nueva), 200);

        String grupo = "{\"clave\":\"%s\",\"integrantes\":[%d,%d,%d]}"
                .formatted(nombre.toLowerCase(), ids[0], ids[1], ids[2]);
        return new Caso(nombre.toLowerCase(), ids[0], ids[1], ids[2], nueva, grupo);
    }

    @Test
    void laVistaPreviaMuestraSoloLoQueFaltaSinRepetir() throws Exception {
        Caso c = caso();
        String previa = post_("/establecimientos/duplicados/resolucion/previa",
                "{\"conservarId\":%d,\"grupo\":%s}".formatted(c.queda(), c.grupo()), 200);
        assertThat((Integer) leer(previa, "$.contactos")).isEqualTo(1);
        assertThat((Integer) leer(previa, "$.caracteristicas")).isEqualTo(1);
        assertThat((List<String>) leer(previa, "$.origenes[0].contactos[*].valor")).containsExactly("museo.gob.ar");
        assertThat((List<Object>) leer(previa, "$.origenes[1].contactos")).isEmpty();
        assertThat((List<Object>) leer(previa, "$.origenes[1].caracteristicas")).isEmpty();
        // la vista previa no cambia nada
        assertThat((List<Object>) leer(get_("/establecimientos/" + c.queda() + "/contactos", 200), "$")).hasSize(2);
    }

    @Test
    void copiaLoQueFaltaYDaDeBajaALosDemasEnUnaTransaccion() throws Exception {
        Caso c = caso();
        String r = post_("/establecimientos/duplicados/resolucion",
                "{\"conservarId\":%d,\"grupo\":%s,\"copiarContactos\":true,\"copiarCaracteristicas\":true}"
                        .formatted(c.queda(), c.grupo()), 200);
        assertThat((Integer) leer(r, "$.contactosCopiados")).isEqualTo(1);
        assertThat((Integer) leer(r, "$.caracteristicasCopiadas")).isEqualTo(1);

        String contactos = get_("/establecimientos/" + c.queda() + "/contactos", 200);
        assertThat((List<String>) leer(contactos, "$[*].valor")).containsExactly("342 456-7890", "@museo", "museo.gob.ar");
        assertThat((String) leer(contactos, "$[2].uso")).isEqualTo("Reservas");
        assertThat((Boolean) leer(contactos, "$[2].esPublico")).isTrue();
        assertThat((List<Integer>) leer(get_("/establecimientos/" + c.queda() + "/caracteristicas", 200), "$[*].caracteristicaId"))
                .contains((int) c.caracteristicaNueva()).hasSize(2);

        // se copia, no se mueve: el dado de baja conserva sus datos
        assertThat((Boolean) leer(get_("/establecimientos/" + c.otro(), 200), "$.activo")).isFalse();
        assertThat((List<Object>) leer(get_("/establecimientos/" + c.otro() + "/contactos", 200), "$")).hasSize(3);
        // queda auditado
        assertThat((List<Object>) leer(get_("/auditoria?tabla=canal_contacto&accion=ALTA", 200), "$.contenido")).isNotEmpty();
    }

    @Test
    void sinCopiaSoloDaDeBajaYSiElGrupoCambioNoTocaNada() throws Exception {
        Caso c = caso();
        // grupo con un integrante de menos: 409 y no se copia ni se da de baja nada
        String incompleto = "{\"clave\":\"%s\",\"integrantes\":[%d,%d]}".formatted(c.clave(), c.queda(), c.otro());
        post_("/establecimientos/duplicados/resolucion",
                "{\"conservarId\":%d,\"grupo\":%s,\"copiarContactos\":true}".formatted(c.queda(), incompleto), 409);
        assertThat((List<Object>) leer(get_("/establecimientos/" + c.queda() + "/contactos", 200), "$")).hasSize(2);
        assertThat((Boolean) leer(get_("/establecimientos/" + c.otro(), 200), "$.activo")).isTrue();

        String r = post_("/establecimientos/duplicados/resolucion",
                "{\"conservarId\":%d,\"grupo\":%s}".formatted(c.queda(), c.grupo()), 200);
        assertThat((Integer) leer(r, "$.contactosCopiados")).isZero();
        assertThat((List<Object>) leer(get_("/establecimientos/" + c.queda() + "/contactos", 200), "$")).hasSize(2);
    }
}
