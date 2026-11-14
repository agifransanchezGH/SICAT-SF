package ar.sf.sicat.admin.importacion;

import static ar.sf.sicat.admin.importacion.Planillas.f;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import ar.sf.sicat.admin.PruebaAbm;

class ImportacionTest extends PruebaAbm {

    @Autowired
    JdbcTemplate jdbc;

    private String importar(String perfil, String accion, MockMultipartFile archivo, int estado) throws Exception {
        return pedir(multipart(API + "/importacion/" + perfil + "/" + accion).file(archivo), admin(), estado);
    }

    /** Sufijo para que los nombres no choquen con los de otros tests (la base es compartida). */
    private static String s() {
        return UUID.randomUUID().toString().substring(0, 6);
    }

    @Test
    void gastronomiaValidarYEjecutar() throws Exception {
        String x = s();
        MockMultipartFile planilla = Planillas.nueva().hoja("donde-comer",
                f("categoria", "titulo", "subtitulo", "direccion", "tel", "whatsapp", "instagram", "facebook", "web", "comentario", "Link de Reserva"),
                f("Restaurantes", "ARIADNA " + x, "Pastas, Cocina tradicional", "4 de Enero 3000", "342 4811603", "3425260204",
                        "https://www.instagram.com/ariadna_sf/", "-", "www.ariadna.test", "Liso santafesino", "https://reservas.test/ariadna"),
                f("Restaurantes", "ARIADNA " + x, "Pastas", "Bv. Gálvez 1500", null, null, null, null, null, null, null),
                f("Restaurantes", "ariadna " + x, "Pastas", "4 DE ENERO 3000", null, null, null, null, null, null, null),
                f("Bares", null, null, "Sin nombre 123", null, null, null, null, null, null, null),
                f("Cafés", "Café Ñandú " + x, null, null, "(0342) 4555555 / 4555556", null, "@cafenandu", null, null, null, null))
                .archivo();

        String validado = importar("gastronomia", "validar", planilla, 200);
        assertThat((Integer) leer(validado, "$.total")).isEqualTo(5);
        assertThat((Integer) leer(validado, "$.nuevos")).isEqualTo(3);
        assertThat((Integer) leer(validado, "$.duplicadosEnArchivo")).isEqualTo(1);
        assertThat((Integer) leer(validado, "$.conErrores")).isEqualTo(1);
        assertThat((Boolean) leer(validado, "$.ejecutado")).isFalse();
        assertThat((List<String>) leer(validado, "$.items[3].errores")).containsExactly("Falta el nombre");
        assertThat((Integer) leer(get_("/establecimientos?q=ARIADNA " + x, 200), "$.totalElementos")).isZero();

        String ejecutado = importar("gastronomia", "ejecutar", planilla, 200);
        assertThat((Integer) leer(ejecutado, "$.creados")).isEqualTo(3);
        long id = ((Number) leer(ejecutado, "$.items[0].establecimientoCreadoId")).longValue();

        String detalle = get_("/establecimientos/" + id, 200);
        assertThat((String) leer(detalle, "$.estadoPublicacion")).isEqualTo("BORRADOR");
        assertThat((String) leer(detalle, "$.oferente.nombre")).isEqualTo("ARIADNA " + x);
        assertThat((String) leer(detalle, "$.descripcion")).isEqualTo("Liso santafesino");
        assertThat((String) leer(detalle, "$.ofertas[0].categoria")).isEqualTo("Restaurantes");
        Map<String, Object> lugar = jdbc.queryForMap("""
                SELECT l.calle, l.altura, l.estado_geo, loc.nombre AS localidad FROM sicat.lugar l
                JOIN sicat.establecimiento e ON e.id_lugar = l.id_lugar
                LEFT JOIN sicat.localidad loc ON loc.id_localidad = l.id_localidad
                WHERE e.id_establecimiento = ?""", id);
        assertThat(lugar).containsEntry("calle", "4 de Enero").containsEntry("altura", 3000)
                .containsEntry("estado_geo", "PENDIENTE").containsEntry("localidad", "Santa Fe");

        String contactos = get_("/establecimientos/" + id + "/contactos", 200);
        assertThat((List<String>) leer(contactos, "$[*].tipoCanal"))
                .containsExactly("Teléfono", "WhatsApp", "Web", "Instagram", "Web");
        assertThat((List<String>) leer(contactos, "$[*].valor")).contains("ariadna_sf", "3425260204");
        assertThat((List<Boolean>) leer(contactos, "$[*].esPublico")).containsOnly(true);
        assertThat((String) leer(contactos, "$[4].uso")).isEqualTo("Reservas");
        assertThat((List<String>) leer(get_("/establecimientos/" + id + "/caracteristicas", 200), "$[*].nombre"))
                .containsExactly("Cocina tradicional", "Pastas");

        // la categoría quedó dentro del árbol Gastronomía y la auditoría marca el origen
        assertThat((List<String>) leer(get_("/categorias", 200), "$[?(@.nombre == 'Gastronomía')].hijos[*].nombre"))
                .contains("Restaurantes", "Cafés");
        assertThat((String) leer(get_("/auditoria/establecimiento/" + id, 200), "$[0].origen")).isEqualTo("MIGRACION");

        // segunda vez: todo duplicado, nada nuevo
        String otraVez = importar("gastronomia", "ejecutar", planilla, 200);
        assertThat((Integer) leer(otraVez, "$.creados")).isZero();
        assertThat((Integer) leer(otraVez, "$.duplicadosEnBase")).isEqualTo(3);
        assertThat(((Number) leer(otraVez, "$.items[0].establecimientoExistenteId")).longValue()).isEqualTo(id);
    }

    @Test
    void alojamientoConCaracteristicasYReferente() throws Exception {
        String x = s();
        MockMultipartFile planilla = Planillas.nueva()
                .hoja("Ficha Tecnica de Hoteles",
                        f("Marca temporal", "Fecha del relevamiento in situ", "Nombre del alojamiento", "Año de inauguración", "Dirección",
                                "Tel. fijo", "Tel. Móvil", "Correo electrónico", "Nombre del referente del alojamiento",
                                "Tel. del referente \n(Info de uso interno)", "Seleccione el tipo de alojamiento que corresponda",
                                "Cantidad de habitaciones ", "Total de cantidad de plazas ", "Seleccione los servicios que ofrece/tiene",
                                "Seleccione lo que tiene", "Arribo de transporte público (colectivos) hasta el alojamiento (200 mts. máximo)",
                                "¿Tiene alguna automatización en su empresa? "),
                        f("46043.37", "45904", "HOTEL PRUEBA " + x, "2021", "San Luis 2862", "-", "3.425260204E9", "hotel@prueba.test",
                                "Adrián Pérez", "342 000", "Hotel 3*", "21", "68", "Desayuno, Pet Friendly, Información turística en formatos (PDF, QR)",
                                "TV, WiFi", "Sí*", "Sí"))
                .hoja("Ficha Tecnica de Delegaciones",
                        f("NOMBRE DEL ALOJAMINETO", "DIRECCION", "TIPO DE ALOJAMIENTO que corresponda", "CANTIDAD DE HABITACIONES", "SERVICIOS GENERALES"),
                        f("DELEGACION " + x, "Obispo Principe 350", "Delegaciones", "3", "Servicio medico. Admite contingentes. Gym."))
                .hoja("Hoja que no corresponde", f("A"), f("1"))
                .archivo();

        String r = importar("alojamiento", "ejecutar", planilla, 200);
        assertThat((Integer) leer(r, "$.creados")).isEqualTo(2);
        assertThat((List<String>) leer(r, "$.hojasIgnoradas")).containsExactly("Hoja que no corresponde");
        long hotel = ((Number) leer(r, "$.items[0].establecimientoCreadoId")).longValue();

        String detalle = get_("/establecimientos/" + hotel, 200);
        assertThat((Integer) leer(detalle, "$.anioInauguracion")).isEqualTo(2021);
        long oferta = ((Number) leer(detalle, "$.ofertas[0].id")).longValue();
        String alojamiento = get_("/ofertas/" + oferta + "/alojamiento", 200);
        assertThat((Integer) leer(alojamiento, "$.estrellas")).isEqualTo(3);
        assertThat((Integer) leer(alojamiento, "$.cantHabitaciones")).isEqualTo(21);
        assertThat((Integer) leer(alojamiento, "$.totalPlazas")).isEqualTo(68);
        assertThat((String) leer(get_("/ofertas/" + oferta, 200), "$.fechaRelevamiento")).isEqualTo("2025-09-04");

        List<String> caracteristicas = leer(get_("/establecimientos/" + hotel + "/caracteristicas", 200), "$[*].nombre");
        assertThat(caracteristicas).contains("Desayuno", "Pet Friendly", "Información turística en formatos (PDF, QR)",
                "TV", "WiFi", "Transporte público a menos de 200 m", "¿Tiene alguna automatización en su empresa?");

        String referentes = get_("/establecimientos/" + hotel + "/referentes", 200);
        assertThat((String) leer(referentes, "$[0].nombre")).isEqualTo("Adrián Pérez");
        assertThat((String) leer(referentes, "$[0].telefono")).isEqualTo("342 000");
        // el teléfono del referente no se cargó como contacto público
        assertThat((List<String>) leer(get_("/establecimientos/" + hotel + "/contactos", 200), "$[*].valor"))
                .containsExactly("3425260204", "hotel@prueba.test");

        long delegacion = ((Number) leer(r, "$.items[1].establecimientoCreadoId")).longValue();
        assertThat((String) leer(get_("/establecimientos/" + delegacion, 200), "$.ofertas[0].categoria"))
                .isEqualTo("Delegación y complejo social");
        assertThat((List<String>) leer(get_("/establecimientos/" + delegacion + "/caracteristicas", 200), "$[*].nombre"))
                .containsExactlyInAnyOrder("Servicio medico", "Admite contingentes", "Gym");
    }

    @Test
    void salonesAgrupaFilasDelMismoEstablecimiento() throws Exception {
        String x = s();
        String[] encabezado = f("Marca temporal", "CATEGORIZACIÓN\n", "Nombre de Sala/Salón:", "Dirección:\n(del lugar/salón/ sala)",
                "Teléfono:\n(Fijo - Wsp) ", "Responsable (s):", "Espacio", "Auditorio:", "Banquete:", "Cóctel:", "M2", "Wifi:", "Mesas:");
        MockMultipartFile planilla = Planillas.nueva().hoja("Respuestas de formulario 1", encabezado,
                f("45821.4", "Espacio privado particular", "EL ARENA " + x, "Pueyrredón 300", "0342-154358557", "Juan", "Salón Principal", "400", "600", "1000", "660", "Sí", "Sí"),
                f("45821.5", "Espacio privado particular", "EL ARENA " + x, "Pueyrredón 300", "0342-154358557", "Juan", "Salón VIP", "40", "60", null, "80", "Sí", "No"))
                .archivo();

        String r = importar("salones", "ejecutar", planilla, 200);
        assertThat((Integer) leer(r, "$.total")).isEqualTo(1);
        long id = ((Number) leer(r, "$.items[0].establecimientoCreadoId")).longValue();
        long oferta = ((Number) leer(get_("/establecimientos/" + id, 200), "$.ofertas[0].id")).longValue();
        String salones = get_("/ofertas/" + oferta + "/salones", 200);
        assertThat((List<String>) leer(salones, "$[*].nombre")).containsExactlyInAnyOrder("Salón Principal", "Salón VIP");
        assertThat((List<String>) leer(get_("/establecimientos/" + id + "/caracteristicas", 200), "$[*].nombre"))
                .extracting(String::toLowerCase).containsExactlyInAnyOrder("wifi", "mesas");
        assertThat((String) leer(get_("/establecimientos/" + id + "/referentes", 200), "$[0].nombre")).isEqualTo("Juan");
    }

    @Test
    void elMismoEstablecimientoEnOtraPlanillaSumaLaCategoria() throws Exception {
        String nombre = "MUSEO COMPARTIDO " + s();
        String museo = importar("museos", "ejecutar", Planillas.nueva().hoja("Hoja 1",
                f("NOMBRE", "DIRECCIÓN"), f(nombre, "San Martín 1540")).archivo(), 200);
        long id = ((Number) leer(museo, "$.items[0].establecimientoCreadoId")).longValue();

        MockMultipartFile recreacion = Planillas.nueva().hoja("ATRACTIVOS TURÍSTICOS DESTACADO",
                f("NOMBRE", "DIRECCIÓN / UBICACIÓN"), f(nombre, "San Martín 1540")).archivo();
        String r = importar("recreacion", "ejecutar", recreacion, 200);
        assertThat((String) leer(r, "$.items[0].estado")).isEqualTo("DUPLICADO_EN_BASE");
        assertThat((List<String>) leer(r, "$.items[0].advertencias"))
                .anySatisfy(a -> assertThat(a).contains("se le agregó la categoría 'Atractivo turístico destacado'"));

        String detalle = get_("/ofertas/" + ((Number) leer(get_("/establecimientos/" + id, 200), "$.ofertas[0].id")).longValue(), 200);
        assertThat((String) leer(detalle, "$.categoria")).isEqualTo("Museo");
        assertThat((List<String>) leer(detalle, "$.categoriasAdicionales[*].nombre")).containsExactly("Atractivo turístico destacado");

        // una tercera vez no la vuelve a agregar
        String otra = importar("recreacion", "ejecutar", recreacion, 200);
        assertThat((List<String>) leer(otra, "$.items[0].advertencias")).noneMatch(a -> a.contains("se le agregó"));
    }

    @Test
    void reimportarNoDuplicaAunqueLaDireccionTengaCerosOFechas() throws Exception {
        String x = s();
        MockMultipartFile planilla = Planillas.nueva().hoja("CLUBES DEPORTIVOS",
                f("NOMBRE", "DIRECCIÓN"),
                f("COTO " + x, "Rivadavia 3396 Local 004"),
                f("BAHCO " + x, "4 de January 0801")).archivo();
        String primera = importar("servicios-turisticos", "ejecutar", planilla, 200);
        assertThat((Integer) leer(primera, "$.creados")).isEqualTo(2);
        assertThat((String) leer(primera, "$.items[1].direccion")).isEqualTo("4 de Enero 801");
        String segunda = importar("servicios-turisticos", "ejecutar", planilla, 200);
        assertThat((Integer) leer(segunda, "$.creados")).isZero();
        assertThat((Integer) leer(segunda, "$.duplicadosEnBase")).isEqualTo(2);
    }

    @Test
    void erroresDeArchivoYPermisos() throws Exception {
        MockMultipartFile vacio = new MockMultipartFile("archivo", "x.xlsx", "application/octet-stream", new byte[0]);
        MockMultipartFile noEsExcel = new MockMultipartFile("archivo", "x.xlsx", "application/octet-stream", "hola".getBytes());
        importar("gastronomia", "validar", vacio, 400);
        importar("gastronomia", "validar", noEsExcel, 400);
        importar("no-existe", "validar", Planillas.nueva().hoja("x", f("NOMBRE"), f("A")).archivo(), 404);
        // ninguna hoja del perfil
        importar("recreacion", "validar", Planillas.nueva().hoja("Otra cosa", f("NOMBRE"), f("A")).archivo(), 400);

        pedir(multipart(API + "/importacion/museos/validar").file(Planillas.nueva().hoja("Hoja 1", f("NOMBRE"), f("A")).archivo()),
                bearerDe("CARGADOR"), 403);
        assertThat((List<String>) leer(get_("/importacion/perfiles", 200), "$[*].archivo"))
                .contains("gastronomia", "alojamiento", "salones", "servicios-turisticos");
    }

    @Test
    void duplicadosEnLaBase() throws Exception {
        String nombre = "Duplicado " + s();
        long oferente = crearOferente();
        long lugar = crearLugar(-31.6, -60.7, null);
        long a = id(post_("/establecimientos", "{\"oferenteId\":%d,\"lugarId\":%d,\"nombre\":\"%s\"}".formatted(oferente, lugar, nombre), 201));
        long b = id(post_("/establecimientos", "{\"oferenteId\":%d,\"lugarId\":%d,\"nombre\":\"%s\"}"
                .formatted(oferente, lugar, nombre.toUpperCase()), 201));
        String grupos = get_("/establecimientos/duplicados", 200);
        List<Map<String, Object>> grupo = leer(grupos, "$[?(@.establecimientos[0].id == " + a + ")]");
        assertThat(grupo).hasSize(1);
        assertThat(grupo.get(0)).containsEntry("mismaDireccion", true);
        assertThat((List<Integer>) leer(grupos, "$[?(@.establecimientos[0].id == " + a + ")].establecimientos[*].id"))
                .containsExactly((int) a, (int) b);
        pedir(get(API + "/establecimientos/duplicados"), bearerDe("CONSULTA"), 403);
    }
}
