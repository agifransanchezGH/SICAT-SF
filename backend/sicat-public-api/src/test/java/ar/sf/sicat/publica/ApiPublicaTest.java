package ar.sf.sicat.publica;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class ApiPublicaTest extends PruebaPublica {

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
