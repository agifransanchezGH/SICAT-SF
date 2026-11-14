package ar.sf.sicat.publica;

import java.nio.file.Path;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Base de los tests de la API Pública: PostGIS real, migraciones aplicadas como sicat_admin_rw
 * (en producción lo hace la API Admin), datos de ejemplo de {@code datos-publicos.sql} y la API
 * conectada con el usuario de solo lectura. Se omite si no hay Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
public abstract class PruebaPublica {

    static final PostgreSQLContainer<?> POSTGIS = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:17-3.5").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("sicat")
            .withEnv("SICAT_ADMIN_DB_PASSWORD", "admin-test")
            .withEnv("SICAT_PUBLIC_DB_PASSWORD", "public-test")
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("../../infra/postgres/init/20_roles.sh").toAbsolutePath()),
                    "/docker-entrypoint-initdb.d/20_roles.sh");

    private static boolean preparada;

    @Autowired
    protected MockMvc mvc;

    @BeforeAll
    static synchronized void prepararBase() {
        if (preparada) {
            return;
        }
        POSTGIS.start();
        Flyway.configure()
                .dataSource(POSTGIS.getJdbcUrl(), "sicat_admin_rw", "admin-test")
                .locations("classpath:db/migration")
                .defaultSchema("sicat")
                .schemas("sicat", "seguridad", "auditoria", "publico")
                .placeholders(Map.of("publicUser", "sicat_public_ro"))
                .load()
                .migrate();
        new ResourceDatabasePopulator(new ClassPathResource("datos-publicos.sql"))
                .execute(new DriverManagerDataSource(POSTGIS.getJdbcUrl(), "sicat_admin_rw", "admin-test"));
        preparada = true;
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGIS::getJdbcUrl);
        r.add("spring.datasource.username", () -> "sicat_public_ro");
        r.add("spring.datasource.password", () -> "public-test");
    }
}
