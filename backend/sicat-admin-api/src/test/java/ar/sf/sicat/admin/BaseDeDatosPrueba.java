package ar.sf.sicat.admin;

import java.nio.file.Path;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * PostGIS real con los mismos roles que docker-compose (infra/postgres/init/20_roles.sh).
 * Se comparte entre tests de la misma JVM.
 */
public final class BaseDeDatosPrueba {

    public static final String ADMIN_USER = "sicat_admin_rw";
    public static final String ADMIN_PASS = "admin-test";
    public static final String PUBLIC_USER = "sicat_public_ro";
    public static final String PUBLIC_PASS = "public-test";

    public static final PostgreSQLContainer<?> POSTGIS = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:17-3.5").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("sicat")
            .withEnv("SICAT_ADMIN_DB_USER", ADMIN_USER)
            .withEnv("SICAT_ADMIN_DB_PASSWORD", ADMIN_PASS)
            .withEnv("SICAT_PUBLIC_DB_USER", PUBLIC_USER)
            .withEnv("SICAT_PUBLIC_DB_PASSWORD", PUBLIC_PASS)
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("../../infra/postgres/init/20_roles.sh").toAbsolutePath()),
                    "/docker-entrypoint-initdb.d/20_roles.sh");

    private BaseDeDatosPrueba() {
    }
}
