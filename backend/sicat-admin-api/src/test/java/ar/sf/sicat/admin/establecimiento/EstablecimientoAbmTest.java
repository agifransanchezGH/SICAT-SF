package ar.sf.sicat.admin.establecimiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;

import org.junit.jupiter.api.Test;

import ar.sf.sicat.admin.PruebaAbm;

class EstablecimientoAbmTest extends PruebaAbm {

    @Test
    void altaModificacionYBaja() throws Exception {
        long oferente = crearOferente();
        long lugar = crearLugar(-31.63, -60.70, null);
        String creado = post_("/establecimientos", """
                {"oferenteId":%d,"lugarId":%d,"nombre":"Hotel Costanera","descripcion":"Frente al río",
                 "anioInauguracion":1998,"destacado":true}
                """.formatted(oferente, lugar), 201);
        long id = id(creado);
        assertThat((String) leer(creado, "$.estadoPublicacion")).isEqualTo("BORRADOR");
        assertThat((String) leer(creado, "$.lugar.direccion")).isEqualTo("San Martín 2000");
        assertThat((Boolean) leer(creado, "$.destacado")).isTrue();

        post_("/establecimientos", "{\"oferenteId\":%d,\"nombre\":\"X\",\"anioInauguracion\":1200}".formatted(oferente), 400);
        post_("/establecimientos", "{\"oferenteId\":999999,\"nombre\":\"X\"}", 404);

        String modificado = put_("/establecimientos/" + id, """
                {"oferenteId":%d,"nombre":"Hotel Costanera Sur","estadoFuncionamiento":"Cerrado temporalmente",
                 "motivoEstado":"Refacción","fechaReapertura":"2027-03-01"}
                """.formatted(oferente), 200);
        assertThat((String) leer(modificado, "$.nombre")).isEqualTo("Hotel Costanera Sur");
        assertThat((Object) leer(modificado, "$.lugar")).isNull();
        assertThat((String) leer(modificado, "$.fechaReapertura")).isEqualTo("2027-03-01");
        assertThat(((Number) leer(get_("/establecimientos/" + id, 200), "$.oferente.id")).longValue())
                .isEqualTo(oferente);

        pedir(delete(API + "/establecimientos/" + id), bearerDe("CARGADOR"), 403);
        delete_("/establecimientos/" + id, 204);
        assertThat((Integer) leer(get_("/establecimientos?oferenteId=" + oferente, 200), "$.totalElementos")).isZero();
        assertThat((Integer) leer(get_("/establecimientos?oferenteId=" + oferente + "&incluirInactivos=true", 200),
                "$.totalElementos")).isEqualTo(1);
        get_("/establecimientos/999999", 404);
    }

    @Test
    void filtrosDelListado() throws Exception {
        long oferente = crearOferente();
        long localidad = crearCatalogo("localidad", "\"provincia\":\"Santa Fe\"");
        long enCentro = crearEstablecimiento(oferente, crearLugar(-31.63, -60.70, localidad));
        long lejos = crearEstablecimiento(oferente, crearLugar(-32.95, -60.65, null));
        long gastronomia = crearCategoria(null);
        long parrilla = crearCategoria(gastronomia);
        long alojamiento = crearCategoria(null);
        crearOferta(enCentro, parrilla);
        long ofertaLejos = crearOferta(lejos, alojamiento);
        put_("/ofertas/" + ofertaLejos + "/categorias", "{\"ids\":[%d]}".formatted(parrilla), 200);

        String porOferente = "/establecimientos?oferenteId=" + oferente;
        assertThat(ids(get_(porOferente + "&categoria=" + gastronomia, 200))).containsExactlyInAnyOrder(enCentro, lejos);
        assertThat(ids(get_(porOferente + "&categoria=" + alojamiento, 200))).containsExactly(lejos);
        assertThat(ids(get_(porOferente + "&localidadId=" + localidad, 200))).containsExactly(enCentro);
        assertThat(ids(get_(porOferente + "&bbox=-60.75,-31.68,-60.65,-31.58", 200))).containsExactly(enCentro);
        assertThat(ids(get_(porOferente + "&estado=BORRADOR", 200))).hasSize(2);
        assertThat(ids(get_(porOferente + "&estado=PUBLICADO", 200))).isEmpty();

        String lista = get_(porOferente + "&sort=nombre,desc&size=1", 200);
        assertThat((Integer) leer(lista, "$.totalPaginas")).isEqualTo(2);
        String fila = get_(porOferente + "&localidadId=" + localidad, 200);
        assertThat((List<String>) leer(fila, "$.contenido[0].categorias")).hasSize(1);
        assertThat((Double) leer(fila, "$.contenido[0].coordenadas.lng")).isEqualTo(-60.70);

        get_("/establecimientos?sort=noExiste", 400);
        get_("/establecimientos?estado=OTRO", 400);
    }

    private static List<Long> ids(String json) {
        return ((List<Number>) leer(json, "$.contenido[*].id")).stream().map(Number::longValue).toList();
    }

    @Test
    void contactosYReferentes() throws Exception {
        long id = crearEstablecimiento(crearOferente(), null);
        long instagram = crearCatalogo("tipo-canal", null);

        String contactos = put_("/establecimientos/" + id + "/contactos",
                "[{\"tipoCanalId\":%d,\"valor\":\"@hotel\",\"esPublico\":true,\"orden\":5}]".formatted(instagram), 200);
        assertThat((Integer) leer(contactos, "$[0].orden")).isEqualTo(5);
        assertThat((List<Object>) leer(get_("/establecimientos/" + id + "/contactos", 200), "$")).hasSize(1);

        String referentes = put_("/establecimientos/" + id + "/referentes", """
                [{"nombre":"Ana Gómez","cargo":"Gerente","correo":"ana@hotel.test"},
                 {"nombre":"Luis Pérez","telefono":"342 555"}]
                """, 200);
        assertThat((List<Object>) leer(referentes, "$")).hasSize(2);
        long ana = ((Number) leer(referentes, "$[0].id")).longValue();
        String reemplazados = put_("/establecimientos/" + id + "/referentes",
                "[{\"id\":%d,\"nombre\":\"Ana Gómez\",\"cargo\":\"Dueña\",\"activo\":false}]".formatted(ana), 200);
        assertThat((List<Object>) leer(reemplazados, "$")).hasSize(1);
        assertThat((String) leer(reemplazados, "$[0].cargo")).isEqualTo("Dueña");
        assertThat((Boolean) leer(get_("/establecimientos/" + id + "/referentes", 200), "$[0].activo")).isFalse();
        put_("/establecimientos/" + id + "/referentes", "[{\"nombre\":\"X\",\"correo\":\"no-es-mail\"}]", 400);
    }

    @Test
    void distincionesYCaracteristicas() throws Exception {
        long id = crearEstablecimiento(crearOferente(), null);
        long premio = crearCatalogo("distincion", null);
        long otroPremio = crearCatalogo("distincion", null);

        String distinciones = put_("/establecimientos/" + id + "/distinciones", """
                [{"distincionId":%d,"anioOtorgamiento":2024,"urlVerificacion":"https://premios.test/1"},
                 {"distincionId":%d}]
                """.formatted(premio, otroPremio), 200);
        assertThat((List<Object>) leer(distinciones, "$")).hasSize(2);
        put_("/establecimientos/" + id + "/distinciones",
                "[{\"distincionId\":%d,\"anioOtorgamiento\":2025}]".formatted(premio), 200);
        String leidas = get_("/establecimientos/" + id + "/distinciones", 200);
        assertThat((List<Object>) leer(leidas, "$")).hasSize(1);
        assertThat((Integer) leer(leidas, "$[0].anioOtorgamiento")).isEqualTo(2025);
        put_("/establecimientos/" + id + "/distinciones",
                "[{\"distincionId\":%d},{\"distincionId\":%d}]".formatted(premio, premio), 422);
        put_("/establecimientos/" + id + "/distinciones",
                "[{\"distincionId\":%d,\"urlVerificacion\":\"no es url\"}]".formatted(premio), 400);

        long habitaciones = crearCatalogo("caracteristica", "\"grupo\":\"Capacidad\",\"admiteCantidad\":true");
        long wifi = crearCatalogo("caracteristica", "\"grupo\":\"Servicios\"");
        String caracteristicas = put_("/establecimientos/" + id + "/caracteristicas",
                "[{\"caracteristicaId\":%d,\"cantidad\":40},{\"caracteristicaId\":%d}]".formatted(habitaciones, wifi), 200);
        assertThat((List<Object>) leer(caracteristicas, "$")).hasSize(2);
        assertThat((Integer) leer(caracteristicas, "$[0].cantidad")).isEqualTo(40);
        put_("/establecimientos/" + id + "/caracteristicas",
                "[{\"caracteristicaId\":%d,\"cantidad\":2}]".formatted(wifi), 422);
        put_("/establecimientos/" + id + "/caracteristicas",
                "[{\"caracteristicaId\":%d,\"cantidad\":45}]".formatted(habitaciones), 200);
        String finales = get_("/establecimientos/" + id + "/caracteristicas", 200);
        assertThat((List<Object>) leer(finales, "$")).hasSize(1);
        assertThat((Integer) leer(finales, "$[0].cantidad")).isEqualTo(45);
    }

    @Test
    void consultaLeeLosSubrecursosYElCambioQuedaAuditado() throws Exception {
        long id = crearEstablecimiento(crearOferente(), null);
        String consulta = bearerDe("CONSULTA");
        for (String sub : List.of("", "/contactos", "/referentes", "/distinciones", "/caracteristicas", "/ofertas")) {
            pedir(get(API + "/establecimientos/" + id + sub), consulta, 200);
        }
        String historial = get_("/auditoria/establecimiento/" + id, 200);
        assertThat((String) leer(historial, "$[0].accion")).isEqualTo("ALTA");
        assertThat((String) leer(historial, "$[0].usuario.email")).isEqualTo(ADMIN_EMAIL);
    }
}
