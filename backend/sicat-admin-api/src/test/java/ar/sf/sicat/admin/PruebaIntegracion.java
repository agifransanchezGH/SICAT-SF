package ar.sf.sicat.admin;

import static ar.sf.sicat.admin.BaseDeDatosPrueba.ADMIN_PASS;
import static ar.sf.sicat.admin.BaseDeDatosPrueba.ADMIN_USER;
import static ar.sf.sicat.admin.BaseDeDatosPrueba.POSTGIS;
import static ar.sf.sicat.admin.BaseDeDatosPrueba.PUBLIC_USER;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;

/**
 * Base de los tests de integración de la API Admin: PostGIS real (Testcontainers),
 * migraciones Flyway y ADMIN inicial creado por AdminInicial. Todas las subclases comparten
 * el mismo contexto de Spring y la misma base. Se omiten si no hay Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
public abstract class PruebaIntegracion {

    protected static final String API = "/api/admin/v1";
    protected static final String ADMIN_EMAIL = "admin@sicat.test";
    protected static final String ADMIN_PASSWORD = "admin-inicial-123";

    @Autowired
    protected MockMvc mvc;

    @BeforeAll
    static void iniciarBase() {
        POSTGIS.start();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGIS::getJdbcUrl);
        r.add("spring.datasource.username", () -> ADMIN_USER);
        r.add("spring.datasource.password", () -> ADMIN_PASS);
        r.add("spring.flyway.placeholders.publicUser", () -> PUBLIC_USER);
        r.add("sicat.admin-inicial.email", () -> ADMIN_EMAIL);
        r.add("sicat.admin-inicial.password", () -> ADMIN_PASSWORD);
    }

    /** Hace login y devuelve el JSON de la respuesta. */
    protected String login(String email, String password) throws Exception {
        return mvc.perform(post(API + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    protected String accessToken(String email, String password) throws Exception {
        return JsonPath.read(login(email, password), "$.accessToken");
    }

    protected String bearerAdmin() throws Exception {
        return "Bearer " + accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
    }
}
