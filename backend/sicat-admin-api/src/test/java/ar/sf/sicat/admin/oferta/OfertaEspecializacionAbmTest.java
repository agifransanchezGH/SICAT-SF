package ar.sf.sicat.admin.oferta;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import ar.sf.sicat.admin.PruebaAbm;

class OfertaEspecializacionAbmTest extends PruebaAbm {

    @org.springframework.beans.factory.annotation.Autowired
    org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void ofertasYCategoriasAdicionales() throws Exception {
        long establecimiento = crearEstablecimiento(crearOferente(), null);
        long hotel = crearCategoria(null);
        long eventos = crearCategoria(null);
        long gastronomia = crearCategoria(null);

        String creada = post_("/establecimientos/" + establecimiento + "/ofertas", """
                {"categoriaId":%d,"acceso":"Con reserva","publicoAdmitido":"Todo público","frecuencia":"Todo el año",
                 "fechaRelevamiento":"2026-10-01"}
                """.formatted(hotel), 201);
        long oferta = id(creada);
        assertThat((List<Object>) leer(creada, "$.horarios")).isEmpty();
        assertThat((Object) leer(creada, "$.alojamiento")).isNull();
        post_("/establecimientos/" + establecimiento + "/ofertas", "{\"categoriaId\":999999}", 404);
        post_("/establecimientos/" + establecimiento + "/ofertas",
                "{\"categoriaId\":%d,\"fechaRelevamiento\":\"2999-01-01\"}".formatted(hotel), 400);

        String adicionales = put_("/ofertas/" + oferta + "/categorias", "{\"ids\":[%d,%d]}".formatted(eventos, gastronomia), 200);
        assertThat((List<Object>) leer(adicionales, "$")).hasSize(2);
        put_("/ofertas/" + oferta + "/categorias", "{\"ids\":[%d]}".formatted(hotel), 422);
        assertThat((List<Object>) leer(get_("/ofertas/" + oferta + "/categorias", 200), "$")).hasSize(2);

        // cambiar la principal a una que era adicional la saca de las adicionales
        String modificada = put_("/ofertas/" + oferta, "{\"categoriaId\":%d}".formatted(eventos), 200);
        assertThat(((Number) leer(modificada, "$.categoriaId")).longValue()).isEqualTo(eventos);
        assertThat((List<Integer>) leer(modificada, "$.categoriasAdicionales[*].id")).containsExactly((int) gastronomia);

        String lista = get_("/establecimientos/" + establecimiento + "/ofertas", 200);
        assertThat((List<Object>) leer(lista, "$")).hasSize(1);
        assertThat((Object) leer(lista, "$[0].horarios")).isNull();

        delete_("/ofertas/" + oferta, 204);
        assertThat((List<Object>) leer(get_("/establecimientos/" + establecimiento + "/ofertas", 200), "$")).isEmpty();
        assertThat((List<Object>) leer(get_("/establecimientos/" + establecimiento + "/ofertas?incluirInactivas=true", 200), "$"))
                .hasSize(1);
        get_("/ofertas/999999", 404);
    }

    @Test
    void horariosDeLaOferta() throws Exception {
        long oferta = crearOferta(crearEstablecimiento(crearOferente(), null), crearCategoria(null));
        String horarios = put_("/ofertas/" + oferta + "/horarios", """
                [{"diaSemana":5,"desde":"20:00","hasta":"02:00"},
                 {"diaSemana":1,"desde":"09:00","hasta":"13:00"}]
                """, 200);
        assertThat((List<Integer>) leer(horarios, "$[*].diaSemana")).containsExactly(1, 5);
        assertThat((String) leer(horarios, "$[1].hasta")).isEqualTo("02:00");
        long lunes = ((Number) leer(horarios, "$[0].id")).longValue();

        String cambiados = put_("/ofertas/" + oferta + "/horarios",
                "[{\"id\":%d,\"diaSemana\":1,\"desde\":\"10:00\",\"hasta\":\"14:00\"}]".formatted(lunes), 200);
        assertThat((List<Object>) leer(cambiados, "$")).hasSize(1);
        assertThat((String) leer(get_("/ofertas/" + oferta + "/horarios", 200), "$[0].desde")).isEqualTo("10:00");

        // en la base queda la hora tal cual, sin corrimiento por zona horaria
        assertThat(jdbc.queryForObject("SELECT hora_desde::text FROM sicat.horario WHERE id_horario = ?",
                String.class, lunes)).isEqualTo("10:00:00");

        put_("/ofertas/" + oferta + "/horarios", "[{\"diaSemana\":8,\"desde\":\"10:00\",\"hasta\":\"14:00\"}]", 400);
        put_("/ofertas/" + oferta + "/horarios", "[{\"diaSemana\":2,\"desde\":\"10:00\",\"hasta\":\"10:00\"}]", 422);
    }

    @Test
    void alojamiento() throws Exception {
        long oferta = crearOferta(crearEstablecimiento(crearOferente(), null), crearCategoria(null));
        get_("/ofertas/" + oferta + "/alojamiento", 404);
        put_("/ofertas/" + oferta + "/alojamiento",
                "{\"estrellas\":4,\"cantHabitaciones\":60,\"totalPlazas\":140,\"habAccesibles\":3}", 200);
        String reemplazado = put_("/ofertas/" + oferta + "/alojamiento", "{\"estrellas\":5,\"cantHabitaciones\":62}", 200);
        assertThat((Object) leer(reemplazado, "$.totalPlazas")).isNull();
        assertThat((Integer) leer(get_("/ofertas/" + oferta + "/alojamiento", 200), "$.estrellas")).isEqualTo(5);
        assertThat((Integer) leer(get_("/ofertas/" + oferta, 200), "$.alojamiento.cantHabitaciones")).isEqualTo(62);
        put_("/ofertas/" + oferta + "/alojamiento", "{\"estrellas\":6}", 400);
    }

    @Test
    void circuitoYParadas() throws Exception {
        long oferente = crearOferente();
        long oferta = crearOferta(crearEstablecimiento(oferente, null), crearCategoria(null));
        long partida = crearLugar(-31.65, -60.71, null);
        long museo = crearEstablecimiento(oferente, null);
        long iglesia = crearEstablecimiento(oferente, null);

        get_("/ofertas/" + oferta + "/circuito", 404);
        get_("/ofertas/" + oferta + "/circuito/paradas", 404);
        String circuito = put_("/ofertas/" + oferta + "/circuito", """
                {"lugarPartidaId":%d,"duracionMin":90,
                 "trazado":{"type":"LineString","coordinates":[[-60.71,-31.65],[-60.70,-31.64],[-60.70,-31.63]]}}
                """.formatted(partida), 200);
        assertThat((String) leer(circuito, "$.trazado.type")).isEqualTo("LineString");
        assertThat((List<Object>) leer(circuito, "$.trazado.coordinates")).hasSize(3);
        assertThat((Integer) leer(get_("/ofertas/" + oferta + "/circuito", 200), "$.duracionMin")).isEqualTo(90);
        put_("/ofertas/" + oferta + "/circuito",
                "{\"trazado\":{\"type\":\"Point\",\"coordinates\":[-60.7,-31.6]}}", 400);

        String paradas = put_("/ofertas/" + oferta + "/circuito/paradas",
                "[{\"establecimientoId\":%d},{\"establecimientoId\":%d}]".formatted(museo, iglesia), 200);
        assertThat((List<Integer>) leer(paradas, "$[*].orden")).containsExactly(1, 2);
        String invertidas = put_("/ofertas/" + oferta + "/circuito/paradas",
                "[{\"establecimientoId\":%d}]".formatted(iglesia), 200);
        assertThat((List<Object>) leer(invertidas, "$")).hasSize(1);
        assertThat(((Number) leer(get_("/ofertas/" + oferta + "/circuito/paradas", 200), "$[0].establecimientoId"))
                .longValue()).isEqualTo(iglesia);

        // el lugar de partida ya no se puede eliminar
        delete_("/lugares/" + partida, 409);
    }

    @Test
    void salonesYSusCaracteristicas() throws Exception {
        long oferta = crearOferta(crearEstablecimiento(crearOferente(), null), crearCategoria(null));
        String salon = post_("/ofertas/" + oferta + "/salones",
                "{\"nombre\":\"Salón Río\",\"capAuditorio\":300,\"capBanquete\":180,\"m2\":420.5}", 201);
        long id = id(salon);
        post_("/ofertas/" + oferta + "/salones", "{\"nombre\":\"X\",\"capAuditorio\":-1}", 400);

        String modificado = put_("/salones/" + id, "{\"nombre\":\"Salón Río Grande\",\"capCoctel\":250}", 200);
        assertThat((Object) leer(modificado, "$.capAuditorio")).isNull();
        assertThat((Integer) leer(modificado, "$.capCoctel")).isEqualTo(250);

        long proyector = crearCatalogo("caracteristica", "\"grupo\":\"Equipamiento\",\"admiteCantidad\":true");
        String caracteristicas = put_("/salones/" + id + "/caracteristicas",
                "[{\"caracteristicaId\":%d,\"cantidad\":2}]".formatted(proyector), 200);
        assertThat((Integer) leer(caracteristicas, "$[0].cantidad")).isEqualTo(2);
        assertThat((List<Object>) leer(get_("/salones/" + id + "/caracteristicas", 200), "$")).hasSize(1);

        delete_("/salones/" + id, 204);
        assertThat((List<Object>) leer(get_("/ofertas/" + oferta + "/salones", 200), "$")).isEmpty();
        assertThat((List<Object>) leer(get_("/ofertas/" + oferta + "/salones?incluirInactivos=true", 200), "$")).hasSize(1);
        put_("/salones/999999", "{\"nombre\":\"X\"}", 404);
    }

    @Test
    void visitasGuiadasConHorarios() throws Exception {
        long oferta = crearOferta(crearEstablecimiento(crearOferente(), null), crearCategoria(null));
        String visita = post_("/ofertas/" + oferta + "/visitas-guiadas",
                "{\"modalidad\":\"Grupal\",\"requiereReserva\":true}", 201);
        long id = id(visita);
        assertThat((Boolean) leer(visita, "$.habilitada")).isTrue();

        put_("/visitas-guiadas/" + id + "/horarios", "[{\"diaSemana\":6,\"desde\":\"10:00\",\"hasta\":\"11:30\"}]", 200);
        put_("/ofertas/" + oferta + "/horarios", "[{\"diaSemana\":6,\"desde\":\"09:00\",\"hasta\":\"18:00\"}]", 200);
        assertThat((List<Object>) leer(get_("/visitas-guiadas/" + id + "/horarios", 200), "$")).hasSize(1);
        assertThat((List<Object>) leer(get_("/ofertas/" + oferta + "/horarios", 200), "$")).hasSize(1);

        String modificada = put_("/visitas-guiadas/" + id, "{\"modalidad\":\"Individual\",\"habilitada\":false}", 200);
        assertThat((Boolean) leer(modificada, "$.habilitada")).isFalse();
        assertThat((Boolean) leer(modificada, "$.requiereReserva")).isFalse();
        assertThat((List<Object>) leer(get_("/ofertas/" + oferta + "/visitas-guiadas", 200), "$[0].horarios")).hasSize(1);

        String detalle = get_("/ofertas/" + oferta, 200);
        assertThat((List<Object>) leer(detalle, "$.visitasGuiadas")).hasSize(1);
        assertThat((List<Object>) leer(detalle, "$.horarios")).hasSize(1);

        delete_("/visitas-guiadas/" + id, 204);
        assertThat((List<Object>) leer(get_("/ofertas/" + oferta + "/visitas-guiadas", 200), "$")).isEmpty();
        assertThat((List<Object>) leer(get_("/ofertas/" + oferta + "/horarios", 200), "$")).hasSize(1);
        get_("/visitas-guiadas/" + id + "/horarios", 404);
    }
}
