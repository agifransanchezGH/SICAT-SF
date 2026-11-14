package ar.sf.sicat.admin.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;

import ar.sf.sicat.admin.PruebaIntegracion;
import ar.sf.sicat.core.comun.Coordenadas;
import ar.sf.sicat.core.comun.Geo;
import ar.sf.sicat.core.dominio.clasificacion.Caracteristica;
import ar.sf.sicat.core.dominio.clasificacion.CaracteristicaRepository;
import ar.sf.sicat.core.dominio.clasificacion.Categoria;
import ar.sf.sicat.core.dominio.clasificacion.CategoriaRepository;
import ar.sf.sicat.core.dominio.establecimiento.Distincion;
import ar.sf.sicat.core.dominio.establecimiento.DistincionRepository;
import ar.sf.sicat.core.dominio.establecimiento.Establecimiento;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoDistincion;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoDistincionRepository;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoRepository;
import ar.sf.sicat.core.dominio.lugar.Lugar;
import ar.sf.sicat.core.dominio.lugar.LugarRepository;
import ar.sf.sicat.core.dominio.oferente.Oferente;
import ar.sf.sicat.core.dominio.oferente.OferenteRepository;
import ar.sf.sicat.core.dominio.oferente.TipoOferente;
import ar.sf.sicat.core.dominio.oferente.TipoOferenteRepository;

class AuditoriaTest extends PruebaIntegracion {

    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;
    @Autowired TipoOferenteRepository tiposOferente;
    @Autowired OferenteRepository oferentes;
    @Autowired LugarRepository lugares;
    @Autowired EstablecimientoRepository establecimientos;
    @Autowired DistincionRepository distinciones;
    @Autowired EstablecimientoDistincionRepository establecimientoDistinciones;
    @Autowired CategoriaRepository categorias;
    @Autowired CaracteristicaRepository caracteristicas;

    /** Registros de auditoría de una tabla y clave, del más antiguo al más reciente. */
    private List<Map<String, Object>> registros(String tabla, String clave) {
        return jdbc.queryForList("""
                SELECT accion, id_usuario, origen, datos_anteriores::text AS antes, datos_nuevos::text AS despues
                FROM auditoria.auditoria WHERE tabla_afectada = ? AND clave_registro = ? ORDER BY id_auditoria""",
                tabla, clave);
    }

    private static Object leer(Object json, String ruta) {
        return JsonPath.read((String) json, ruta);
    }

    private static String unico(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String crearUsuario(String bearer, String email, String rol) throws Exception {
        String respuesta = mvc.perform(post(API + "/usuarios")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreCompleto":"Persona","email":"%s","password":"clave-segura-1","rol":"%s"}
                                """.formatted(email, rol)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return String.valueOf(((Number) JsonPath.read(respuesta, "$.id")).longValue());
    }

    private long idAdmin() throws Exception {
        return ((Number) JsonPath.read(login(ADMIN_EMAIL, ADMIN_PASSWORD), "$.usuario.id")).longValue();
    }

    @Test
    void altaPorApiRegistraUsuarioOrigenYSinHashDeContrasena() throws Exception {
        String email = unico("auditado") + "@sicat.test";
        String id = crearUsuario(bearerAdmin(), email, "CONSULTA");

        List<Map<String, Object>> r = registros("seguridad.usuario", id);
        assertThat(r).hasSize(1);
        assertThat(r.get(0).get("accion")).isEqualTo("ALTA");
        assertThat(((Number) r.get(0).get("id_usuario")).longValue()).isEqualTo(idAdmin());
        assertThat(r.get(0).get("origen")).isEqualTo("APP");
        assertThat(r.get(0).get("antes")).isNull();
        assertThat(leer(r.get(0).get("despues"), "$.email")).isEqualTo(email);
        assertThat(leer(r.get(0).get("despues"), "$.hashPassword")).isEqualTo("(modificada)");
        assertThat((String) r.get(0).get("despues")).doesNotContain("$2a$");
    }

    @Test
    void modificacionGuardaSoloLosCamposQueCambiaron() throws Exception {
        String bearer = bearerAdmin();
        String email = unico("modif") + "@sicat.test";
        String id = crearUsuario(bearer, email, "CONSULTA");
        mvc.perform(put(API + "/usuarios/" + id)
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreCompleto":"Nombre nuevo","email":"%s","password":"otra-clave-22","rol":"CONSULTA"}
                                """.formatted(email)))
                .andExpect(status().isOk());

        Map<String, Object> m = registros("seguridad.usuario", id).get(1);
        assertThat(m.get("accion")).isEqualTo("MODIFICACION");
        assertThat(leer(m.get("antes"), "$.nombreCompleto")).isEqualTo("Persona");
        assertThat(leer(m.get("despues"), "$.nombreCompleto")).isEqualTo("Nombre nuevo");
        assertThat(leer(m.get("despues"), "$.hashPassword")).isEqualTo("(modificada)");
        assertThat((String) m.get("despues")).doesNotContain("email").doesNotContain("rol");
    }

    @Test
    void elLoginNoGeneraAuditoria() throws Exception {
        String clave = String.valueOf(idAdmin());
        int antes = registros("seguridad.usuario", clave).size();
        login(ADMIN_EMAIL, ADMIN_PASSWORD);
        login(ADMIN_EMAIL, ADMIN_PASSWORD);
        assertThat(registros("seguridad.usuario", clave)).hasSize(antes);
    }

    @Test
    void cambiosSinUsuarioQuedanComoSistemaConAsociacionesYGeografia() {
        Long[] ids = tx.execute(s -> {
            TipoOferente tipo = new TipoOferente();
            tipo.setNombre(unico("Tipo"));
            tiposOferente.save(tipo);
            Oferente o = new Oferente();
            o.setTipoOferente(tipo);
            o.setNombre("Oferente original");
            oferentes.save(o);
            Lugar l = new Lugar();
            l.setNombre("Plaza");
            l.setPunto(Geo.punto(new Coordenadas(-31.63, -60.7)));
            lugares.save(l);
            return new Long[] {tipo.getId(), o.getId(), l.getId()};
        });
        tx.executeWithoutResult(s -> oferentes.findById(ids[1]).orElseThrow().setNombre("Oferente renombrado"));
        tx.executeWithoutResult(s -> oferentes.deleteById(ids[1]));

        List<Map<String, Object>> r = registros("sicat.oferente", ids[1].toString());
        assertThat(r).extracting(m -> m.get("accion")).containsExactly("ALTA", "MODIFICACION", "ELIMINACION");
        assertThat(r).allSatisfy(m -> {
            assertThat(m.get("origen")).isEqualTo("SISTEMA");
            assertThat(m.get("id_usuario")).isNull();
        });
        assertThat(((Number) leer(r.get(0).get("despues"), "$.tipoOferente")).longValue()).isEqualTo(ids[0]);
        assertThat(leer(r.get(1).get("antes"), "$.nombre")).isEqualTo("Oferente original");
        assertThat((String) r.get(1).get("despues")).doesNotContain("tipoOferente");
        assertThat(leer(r.get(2).get("antes"), "$.nombre")).isEqualTo("Oferente renombrado");
        assertThat(r.get(2).get("despues")).isNull();

        String punto = (String) leer(registros("sicat.lugar", ids[2].toString()).get(0).get("despues"), "$.punto");
        assertThat(punto).startsWith("POINT (-60.7 -31.63");
    }

    @Test
    void siLaTransaccionSeRevierteNoQuedaAuditoria() {
        String nombre = unico("Revertido");
        tx.executeWithoutResult(s -> {
            TipoOferente tipo = new TipoOferente();
            tipo.setNombre(nombre);
            tiposOferente.saveAndFlush(tipo);
            s.setRollbackOnly();
        });
        Integer cantidad = jdbc.queryForObject(
                "SELECT count(*) FROM auditoria.auditoria WHERE datos_nuevos->>'nombre' = ?", Integer.class, nombre);
        assertThat(cantidad).isZero();
    }

    @Test
    void claveCompuestaYColeccionesManyToMany() {
        Long[] ids = tx.execute(s -> {
            TipoOferente tipo = new TipoOferente();
            tipo.setNombre(unico("Tipo"));
            tiposOferente.save(tipo);
            Oferente o = new Oferente();
            o.setTipoOferente(tipo);
            o.setNombre("Oferente");
            oferentes.save(o);
            Establecimiento e = new Establecimiento();
            e.setOferente(o);
            e.setNombre("Hotel");
            establecimientos.save(e);
            Distincion d = new Distincion();
            d.setNombre(unico("Premio"));
            distinciones.save(d);
            EstablecimientoDistincion ed = new EstablecimientoDistincion();
            ed.setEstablecimiento(e);
            ed.setDistincion(d);
            ed.setAnioOtorgamiento((short) 2025);
            establecimientoDistinciones.save(ed);
            Caracteristica c = new Caracteristica();
            c.setGrupo("Servicios");
            c.setNombre(unico("Wi-Fi"));
            caracteristicas.save(c);
            Categoria cat = new Categoria();
            cat.setNombre("Hotel");
            categorias.save(cat);
            return new Long[] {e.getId(), d.getId(), c.getId(), cat.getId()};
        });
        tx.executeWithoutResult(s -> categorias.findById(ids[3]).orElseThrow()
                .getCaracteristicas().add(caracteristicas.findById(ids[2]).orElseThrow()));

        String clave = jdbc.queryForObject("""
                SELECT clave_registro FROM auditoria.auditoria
                WHERE tabla_afectada = 'sicat.establecimiento_distincion' AND accion = 'ALTA'
                  AND clave_registro::jsonb->>'idDistincion' = ?""", String.class, ids[1].toString());
        assertThat(((Number) leer(clave, "$.idEstablecimiento")).longValue()).isEqualTo(ids[0]);

        Map<String, Object> m = registros("sicat.categoria", ids[3].toString()).get(1);
        assertThat(m.get("accion")).isEqualTo("MODIFICACION");
        assertThat((List<?>) leer(m.get("antes"), "$.caracteristicas")).isEmpty();
        assertThat(((List<?>) leer(m.get("despues"), "$.caracteristicas"))).hasSize(1);
    }

    @Test
    void consultaPorFiltrosEHistorialSegunRol() throws Exception {
        String bearerAdmin = bearerAdmin();
        String idConsulta = crearUsuario(bearerAdmin, unico("consulta") + "@sicat.test", "CONSULTA");
        String emailCargador = unico("cargador") + "@sicat.test";
        crearUsuario(bearerAdmin, emailCargador, "CARGADOR");
        String emailConsulta = jdbc.queryForObject(
                "SELECT email FROM seguridad.usuario WHERE id_usuario = ?", String.class, Long.valueOf(idConsulta));
        String bearerCargador = "Bearer " + accessToken(emailCargador, "clave-segura-1");
        String bearerConsulta = "Bearer " + accessToken(emailConsulta, "clave-segura-1");

        mvc.perform(get(API + "/auditoria")
                        .param("tabla", "usuario").param("clave", idConsulta).param("accion", "ALTA")
                        .header("Authorization", bearerAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].tabla").value("seguridad.usuario"))
                .andExpect(jsonPath("$.contenido[0].usuario.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.contenido[0].datosNuevos.rol").isNumber());

        mvc.perform(get(API + "/auditoria").param("desde", "2000-01-01").param("hasta", "2000-01-31")
                        .header("Authorization", bearerAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));

        mvc.perform(get(API + "/auditoria").header("Authorization", bearerCargador))
                .andExpect(status().isForbidden());

        mvc.perform(get(API + "/auditoria/seguridad.usuario/" + idConsulta).header("Authorization", bearerCargador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accion").value("ALTA"));

        mvc.perform(get(API + "/auditoria/usuario/" + idConsulta).header("Authorization", bearerConsulta))
                .andExpect(status().isForbidden());
    }
}
