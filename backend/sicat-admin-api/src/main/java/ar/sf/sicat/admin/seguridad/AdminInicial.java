package ar.sf.sicat.admin.seguridad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.sf.sicat.admin.seguridad.dto.UsuarioCrearSolicitud;
import ar.sf.sicat.core.seguridad.UsuarioRepository;

/**
 * Si la tabla de usuarios está vacía, crea el primer ADMIN con
 * SICAT_ADMIN_INICIAL_EMAIL / SICAT_ADMIN_INICIAL_PASSWORD (ADR 0005).
 * Si ya hay usuarios, no hace nada: cambiar esas variables no modifica al admin existente.
 */
@Component
public class AdminInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    private final UsuarioRepository usuarios;
    private final UsuarioService servicio;
    private final String email;
    private final String password;
    private final String nombre;

    public AdminInicial(UsuarioRepository usuarios, UsuarioService servicio,
                        @Value("${sicat.admin-inicial.email:}") String email,
                        @Value("${sicat.admin-inicial.password:}") String password,
                        @Value("${sicat.admin-inicial.nombre:Administrador}") String nombre) {
        this.usuarios = usuarios;
        this.servicio = servicio;
        this.email = email;
        this.password = password;
        this.nombre = nombre;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0) {
            return;
        }
        if (email.isBlank() || password.isBlank()) {
            log.warn("No hay usuarios y no se definió SICAT_ADMIN_INICIAL_EMAIL/PASSWORD: nadie podrá iniciar sesión");
            return;
        }
        if (password.length() < 8) {
            throw new IllegalStateException("SICAT_ADMIN_INICIAL_PASSWORD debe tener al menos 8 caracteres");
        }
        servicio.crear(new UsuarioCrearSolicitud(nombre, email, password, RolSistema.ADMIN));
        log.info("Usuario ADMIN inicial creado: {}", UsuarioService.normalizar(email));
    }
}
