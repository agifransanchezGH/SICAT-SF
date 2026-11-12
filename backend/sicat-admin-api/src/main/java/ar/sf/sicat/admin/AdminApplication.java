package ar.sf.sicat.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * API de administración (lectura/escritura). Única aplicación que ejecuta Flyway.
 * Escanea las entidades de negocio, seguridad y auditoría; no las vistas públicas.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EntityScan(basePackages = {
        "ar.sf.sicat.core.dominio",
        "ar.sf.sicat.core.seguridad",
        "ar.sf.sicat.core.auditoria"})
@EnableJpaRepositories(basePackages = {
        "ar.sf.sicat.core.dominio",
        "ar.sf.sicat.core.seguridad",
        "ar.sf.sicat.core.auditoria"})
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
