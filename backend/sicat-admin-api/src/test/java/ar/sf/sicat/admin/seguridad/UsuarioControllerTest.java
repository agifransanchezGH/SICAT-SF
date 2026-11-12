package ar.sf.sicat.admin.seguridad;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import ar.sf.sicat.admin.PruebaIntegracion;

class UsuarioControllerTest extends PruebaIntegracion {

    private static String emailUnico(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 8) + "@sicat.test";
    }

    private Long crear(String bearer, String email, String rol) throws Exception {
        String json = mvc.perform(post(API + "/usuarios")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreCompleto":"Persona de prueba","email":"%s","password":"clave-segura-1","rol":"%s"}
                                """.formatted(email, rol)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.rol").value(rol))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.hashPassword").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    @Test
    void adminCreaUsuarioQuePuedeIniciarSesion() throws Exception {
        String email = emailUnico("cargador");
        crear(bearerAdmin(), email, "CARGADOR");
        mvc.perform(get(API + "/auth/me").header("Authorization", "Bearer " + accessToken(email, "clave-segura-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CARGADOR"));
    }

    @Test
    void emailRepetidoDa409() throws Exception {
        String bearer = bearerAdmin();
        String email = emailUnico("repetido");
        crear(bearer, email, "CONSULTA");
        mvc.perform(post(API + "/usuarios")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreCompleto":"Otra","email":"%s","password":"clave-segura-1","rol":"CONSULTA"}
                                """.formatted(email.toUpperCase())))
                .andExpect(status().isConflict());
    }

    @Test
    void rolOferenteNoSePuedeAsignarEnEtapa1() throws Exception {
        mvc.perform(post(API + "/usuarios")
                        .header("Authorization", bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreCompleto":"X","email":"%s","password":"clave-segura-1","rol":"OFERENTE"}
                                """.formatted(emailUnico("oferente"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarEsPaginado() throws Exception {
        mvc.perform(get(API + "/usuarios?page=0&size=5&sort=email,asc").header("Authorization", bearerAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isArray())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanio").value(5))
                .andExpect(jsonPath("$.totalElementos").isNumber());
    }

    @Test
    void soloAdminGestionaUsuarios() throws Exception {
        String email = emailUnico("consulta");
        crear(bearerAdmin(), email, "CONSULTA");
        String bearerConsulta = "Bearer " + accessToken(email, "clave-segura-1");
        mvc.perform(get(API + "/usuarios").header("Authorization", bearerConsulta))
                .andExpect(status().isForbidden());
    }

    @Test
    void modificarCambiaRolYContrasena() throws Exception {
        String bearer = bearerAdmin();
        String email = emailUnico("modificar");
        Long id = crear(bearer, email, "CONSULTA");
        mvc.perform(put(API + "/usuarios/" + id)
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreCompleto":"Nombre nuevo","email":"%s","password":"otra-clave-22","rol":"CARGADOR"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Nombre nuevo"))
                .andExpect(jsonPath("$.rol").value("CARGADOR"));
        accessToken(email, "otra-clave-22");
    }

    @Test
    void desactivarImpideIniciarSesion() throws Exception {
        String bearer = bearerAdmin();
        String email = emailUnico("inactivo");
        Long id = crear(bearer, email, "CONSULTA");
        mvc.perform(patch(API + "/usuarios/" + id + "/estado")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        mvc.perform(post(API + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"clave-segura-1\"}".formatted(email)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unAdminNoPuedeDesactivarseNiQuitarseElRol() throws Exception {
        String sesion = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String bearer = "Bearer " + JsonPath.read(sesion, "$.accessToken");
        Integer id = JsonPath.read(sesion, "$.usuario.id");
        mvc.perform(patch(API + "/usuarios/" + id + "/estado")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(put(API + "/usuarios/" + id)
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreCompleto":"Administrador","email":"%s","rol":"CONSULTA"}
                                """.formatted(ADMIN_EMAIL)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void usuarioInexistenteDa404() throws Exception {
        mvc.perform(patch(API + "/usuarios/999999/estado")
                        .header("Authorization", bearerAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":true}"))
                .andExpect(status().isNotFound());
    }
}
