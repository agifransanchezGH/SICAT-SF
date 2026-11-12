package ar.sf.sicat.publica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * API Pública (solo lectura). Solo conoce las entidades de las vistas del esquema {@code publico}.
 * Nunca depende de sicat-admin-api ni de las entidades de negocio (verificado con ArchUnit).
 */
@SpringBootApplication
@EntityScan(basePackages = "ar.sf.sicat.core.vistapublica")
@EnableJpaRepositories(basePackages = "ar.sf.sicat.core.vistapublica")
public class PublicApplication {

    public static void main(String[] args) {
        SpringApplication.run(PublicApplication.class, args);
    }
}
