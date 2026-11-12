package ar.sf.sicat.admin;

import static ar.sf.sicat.admin.BaseDeDatosPrueba.ADMIN_PASS;
import static ar.sf.sicat.admin.BaseDeDatosPrueba.ADMIN_USER;
import static ar.sf.sicat.admin.BaseDeDatosPrueba.POSTGIS;
import static ar.sf.sicat.admin.BaseDeDatosPrueba.PUBLIC_PASS;
import static ar.sf.sicat.admin.BaseDeDatosPrueba.PUBLIC_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoRepository;
import ar.sf.sicat.core.seguridad.RolRepository;

/**
 * Levanta la API Admin contra PostGIS real:
 * Flyway aplica V1..Vn, Hibernate valida el mapeo (ddl-auto=validate) y se verifican los permisos
 * de sicat_public_ro. Se omite si no hay Docker.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class MigracionesYPermisosTest {

    @BeforeAll
    static void iniciar() {
        POSTGIS.start();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGIS::getJdbcUrl);
        r.add("spring.datasource.username", () -> ADMIN_USER);
        r.add("spring.datasource.password", () -> ADMIN_PASS);
        r.add("spring.flyway.placeholders.publicUser", () -> PUBLIC_USER);
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    RolRepository roles;

    @Autowired
    EstablecimientoRepository establecimientos;

    @Test
    void migracionesAplicadasYRolesIniciales() {
        assertThat(roles.findByNombre("ADMIN")).isPresent();
        assertThat(roles.count()).isEqualTo(4);
    }

    @Test
    void publicoVeSoloLoPublicadoYNoLeeElNegocio() throws SQLException {
        Long tipo = jdbc.queryForObject(
                "INSERT INTO sicat.tipo_oferente (nombre) VALUES ('Privado') RETURNING id_tipo_oferente", Long.class);
        Long oferente = jdbc.queryForObject(
                "INSERT INTO sicat.oferente (id_tipo_oferente, nombre) VALUES (?, 'Oferente') RETURNING id_oferente",
                Long.class, tipo);
        Long lugar = jdbc.queryForObject("""
                INSERT INTO sicat.lugar (nombre, punto)
                VALUES ('Costanera', public.ST_GeogFromText('SRID=4326;POINT(-60.70 -31.63)'))
                RETURNING id_lugar""", Long.class);
        jdbc.update("""
                INSERT INTO sicat.establecimiento (id_oferente, id_lugar, nombre, estado_publicacion)
                VALUES (?, ?, 'Publicado', 'PUBLICADO'), (?, ?, 'Borrador', 'BORRADOR')""",
                oferente, lugar, oferente, lugar);

        // La API Admin puede usar consultas geográficas
        assertThat(establecimientos.buscarCercanos(-31.63, -60.70, 100)).hasSize(2);

        try (Connection ro = DriverManager.getConnection(POSTGIS.getJdbcUrl(), PUBLIC_USER, PUBLIC_PASS);
             Statement st = ro.createStatement()) {

            try (ResultSet rs = st.executeQuery("SELECT nombre, lat, lng FROM publico.v_establecimiento_publico")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("nombre")).isEqualTo("Publicado");
                assertThat(rs.getDouble("lat")).isEqualTo(-31.63);
                assertThat(rs.next()).as("el borrador no es público").isFalse();
            }

            assertThatThrownBy(() -> st.executeQuery("SELECT * FROM sicat.oferente"))
                    .hasMessageContaining("permission denied");
            assertThatThrownBy(() -> st.executeQuery("SELECT * FROM seguridad.usuario"))
                    .hasMessageContaining("permission denied");
            assertThatThrownBy(() -> st.executeUpdate("DELETE FROM publico.v_categoria_publica"))
                    .hasMessageContaining("permission denied");
        }
    }
}
