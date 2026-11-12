package ar.sf.sicat.publica;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Levanta la API Pública con el usuario de solo lectura. Las migraciones las aplica el test
 * como sicat_admin_rw (en producción lo hace la API Admin). Se omite si no hay Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ApiPublicaTest {

    static final PostgreSQLContainer<?> POSTGIS = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:17-3.5").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("sicat")
            .withEnv("SICAT_ADMIN_DB_PASSWORD", "admin-test")
            .withEnv("SICAT_PUBLIC_DB_PASSWORD", "public-test")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("../../infra/postgres/init/20_roles.sh").toAbsolutePath()),
                    "/docker-entrypoint-initdb.d/20_roles.sh");

    @BeforeAll
    static void migrar() {
        POSTGIS.start();
        Flyway.configure()
                .dataSource(POSTGIS.getJdbcUrl(), "sicat_admin_rw", "admin-test")
                .locations("classpath:db/migration")
                .defaultSchema("sicat")
                .schemas("sicat", "seguridad", "auditoria", "publico")
                .placeholders(java.util.Map.of("publicUser", "sicat_public_ro"))
                .load()
                .migrate();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGIS::getJdbcUrl);
        r.add("spring.datasource.username", () -> "sicat_public_ro");
        r.add("spring.datasource.password", () -> "public-test");
    }

    @Autowired
    MockMvc mvc;

    @Test
    void healthResponde() throws Exception {
        mvc.perform(get("/api/public/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("OK"));
    }

    @Test
    void rechazaEscrituras() throws Exception {
        mvc.perform(post("/api/public/v1/establecimientos"))
                .andExpect(status().isMethodNotAllowed());
    }
}
