package ar.sf.sicat.publica.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Escucha el canal {@code sicat_cambios} de PostgreSQL (V10) y vacía la caché cuando cambia algo en
 * {@code sicat}: un cambio hecho en el panel se ve al instante en el sitio público (ADR 0014).
 * Usa una conexión propia, fuera del pool. Si se corta, reintenta y vacía la caché al reconectar
 * (pudo perderse algún aviso). El vencimiento de 5 minutos de la caché queda como respaldo.
 */
@Component
public class AvisoDeCambios implements SmartLifecycle {

    public static final String CANAL = "sicat_cambios";
    private static final Logger log = LoggerFactory.getLogger(AvisoDeCambios.class);
    private static final Duration ESPERA = Duration.ofSeconds(5);

    private final CacheManager caches;
    private final String url;
    private final String usuario;
    private final String password;
    private final AtomicLong vaciados = new AtomicLong();

    private volatile boolean activo;
    private Thread hilo;

    public AvisoDeCambios(CacheManager caches,
                          @Value("${spring.datasource.url}") String url,
                          @Value("${spring.datasource.username}") String usuario,
                          @Value("${spring.datasource.password}") String password) {
        this.caches = caches;
        this.url = url;
        this.usuario = usuario;
        this.password = password;
    }

    @Override
    public void start() {
        activo = true;
        hilo = Thread.ofPlatform().daemon().name("aviso-de-cambios").start(this::escuchar);
    }

    @Override
    public void stop() {
        activo = false;
        if (hilo != null) {
            hilo.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return activo;
    }

    /** Veces que se vació la caché (para tests y diagnóstico). */
    public long vaciados() {
        return vaciados.get();
    }

    private void escuchar() {
        while (activo) {
            try (Connection con = DriverManager.getConnection(url, usuario, password)) {
                try (Statement st = con.createStatement()) {
                    st.execute("LISTEN " + CANAL);
                }
                vaciar("conexión");
                PGConnection pg = con.unwrap(PGConnection.class);
                while (activo) {
                    PGNotification[] avisos = pg.getNotifications((int) ESPERA.toMillis());
                    if (avisos != null && avisos.length > 0) {
                        vaciar(avisos[0].getParameter() + (avisos.length > 1 ? " y " + (avisos.length - 1) + " más" : ""));
                    }
                }
            } catch (SQLException e) {
                if (activo) {
                    log.warn("Se cortó la escucha de {}: {}. Reintento en {} s", CANAL, e.getMessage(), ESPERA.toSeconds());
                    dormir();
                }
            }
        }
    }

    private void vaciar(String motivo) {
        caches.getCacheNames().forEach(n -> {
            var c = caches.getCache(n);
            if (c != null) {
                c.clear();
            }
        });
        vaciados.incrementAndGet();
        log.debug("Caché vaciada ({})", motivo);
    }

    private void dormir() {
        try {
            Thread.sleep(ESPERA);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
