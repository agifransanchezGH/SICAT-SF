package ar.sf.sicat.admin.seguridad;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import ar.sf.sicat.admin.PruebaIntegracion;

class AuthControllerTest extends PruebaIntegracion {

    @Test
    void loginDevuelveTokensYUsuario() throws Exception {
        mvc.perform(post(API + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ADMIN@sicat.test\",\"password\":\"%s\"}".formatted(ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuario.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.usuario.rol").value("ADMIN"))
                .andExpect(jsonPath("$.usuario.ultimoAcceso").isNotEmpty());
    }

    @Test
    void loginConContrasenaIncorrectaDa401Problem() throws Exception {
        mvc.perform(post(API + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"incorrecta\"}".formatted(ADMIN_EMAIL)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("No autenticado"));
    }

    @Test
    void loginInvalidoDa400() throws Exception {
        mvc.perform(post(API + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-es-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void meDevuelveElUsuarioDelToken() throws Exception {
        mvc.perform(get(API + "/auth/me").header("Authorization", bearerAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void meSinTokenDa401() throws Exception {
        mvc.perform(get(API + "/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRenuevaLaSesion() throws Exception {
        String refresh = JsonPath.read(login(ADMIN_EMAIL, ADMIN_PASSWORD), "$.refreshToken");
        mvc.perform(post(API + "/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refresh)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.usuario.email").value(ADMIN_EMAIL));
    }

    @Test
    void refreshNoAceptaUnAccessToken() throws Exception {
        String access = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        mvc.perform(post(API + "/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(access)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unRefreshTokenNoSirveParaLlamarALaApi() throws Exception {
        String refresh = JsonPath.read(login(ADMIN_EMAIL, ADMIN_PASSWORD), "$.refreshToken");
        mvc.perform(get(API + "/auth/me").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }
}
