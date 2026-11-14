package ar.sf.sicat.publica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.jayway.jsonpath.JsonPath;

/** Un cambio en sicat (como los que hace el panel) vacía la caché de la API Pública al confirmarse (ADR 0014). */
class AvisoDeCambiosTest extends PruebaPublica {

    private static final String FICHA = "/api/public/v1/establecimientos/100";

    @Test
    void unCambioEnLaBaseSeVeSinEsperarElVencimientoDeLaCache() throws Exception {
        JdbcTemplate admin = new JdbcTemplate(new DriverManagerDataSource(POSTGIS.getJdbcUrl(), "sicat_admin_rw", "admin-test"));
        String original = nombre();
        try {
            // Queda en caché con el nombre original.
            assertThat(nombre()).isEqualTo(original);
            admin.update("UPDATE sicat.establecimiento SET nombre = 'Hotel Ñandú (cambiado)' WHERE id_establecimiento = 100");
            assertThat(esperarNombre("Hotel Ñandú (cambiado)")).isTrue();
        } finally {
            admin.update("UPDATE sicat.establecimiento SET nombre = ? WHERE id_establecimiento = 100", original);
            assertThat(esperarNombre(original)).isTrue();
        }
    }

    private String nombre() throws Exception {
        return JsonPath.read(mvc.perform(get(FICHA)).andReturn().getResponse().getContentAsString(), "$.nombre");
    }

    /** El aviso llega en forma asíncrona: espera hasta 10 s (sin el aviso, la caché tardaría 5 minutos). */
    private boolean esperarNombre(String esperado) throws Exception {
        for (int i = 0; i < 100; i++) {
            if (esperado.equals(nombre())) {
                return true;
            }
            Thread.sleep(100);
        }
        return false;
    }
}
