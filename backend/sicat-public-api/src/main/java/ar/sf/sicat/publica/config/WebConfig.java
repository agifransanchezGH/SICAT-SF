package ar.sf.sicat.publica.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/** CORS, caché (Caffeine, ver spring.cache en application.yml) y OpenAPI. */
@Configuration
@EnableCaching
public class WebConfig implements WebMvcConfigurer {

    private final String[] origenes;

    public WebConfig(@Value("${sicat.cors.origenes}") String origenes) {
        this.origenes = Arrays.stream(origenes.split(",")).map(String::trim).toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/public/**").allowedOrigins(origenes).allowedMethods("GET", "HEAD", "OPTIONS");
    }

    @Bean
    OpenAPI sicatPublicOpenApi() {
        return new OpenAPI().info(new Info()
                .title("SICAT SF · API Pública")
                .version("v1")
                .description("Consulta pública de la oferta turística de Santa Fe. Solo lectura."));
    }
}
