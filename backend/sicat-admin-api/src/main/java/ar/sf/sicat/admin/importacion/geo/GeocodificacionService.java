package ar.sf.sicat.admin.importacion.geo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import ar.sf.sicat.admin.importacion.dto.EstadoGeocodificacion;
import ar.sf.sicat.core.comun.ConflictoException;
import ar.sf.sicat.core.comun.Coordenadas;
import ar.sf.sicat.core.comun.Geo;
import ar.sf.sicat.core.dominio.lugar.Lugar;
import ar.sf.sicat.core.dominio.lugar.LugarRepository;
import jakarta.annotation.PreDestroy;

/**
 * Geocodifica en segundo plano los lugares importados (estado_geo = PENDIENTE), de a uno por segundo.
 * Resultado por lugar: AUTOMATICO (coordenadas encontradas, hay que verificarlas en el mapa antes de publicar)
 * o NO_ENCONTRADO (hay que ubicarlo a mano). Solo corre un proceso a la vez.
 */
@Service
public class GeocodificacionService {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String AUTOMATICO = "AUTOMATICO";
    public static final String NO_ENCONTRADO = "NO_ENCONTRADO";

    private static final Logger log = LoggerFactory.getLogger(GeocodificacionService.class);

    private final LugarRepository lugares;
    private final Geocodificador geocodificador;
    private final TransactionTemplate tx;
    private final long pausaMs;
    private final ExecutorService ejecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "geocodificacion");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean enCurso = new AtomicBoolean(false);
    private volatile EstadoGeocodificacion ultimo = EstadoGeocodificacion.inicial();

    public GeocodificacionService(LugarRepository lugares, Geocodificador geocodificador, TransactionTemplate tx,
                                  @Value("${sicat.geocodificacion.pausa-ms:1100}") long pausaMs) {
        this.lugares = lugares;
        this.geocodificador = geocodificador;
        this.tx = tx;
        this.pausaMs = pausaMs;
    }

    /** Arranca un proceso para hasta {@code limite} lugares pendientes. 409 si ya hay uno en curso. */
    public EstadoGeocodificacion iniciar(int limite) {
        if (!enCurso.compareAndSet(false, true)) {
            throw new ConflictoException("Ya hay una geocodificación en curso");
        }
        long pendientes = lugares.countByPuntoIsNullAndEstadoGeo(PENDIENTE);
        int total = (int) Math.min(limite, pendientes);
        ultimo = new EstadoGeocodificacion(true, total, 0, 0, 0, pendientes, OffsetDateTime.now(), null, null);
        ejecutor.submit(() -> correr(total));
        return ultimo;
    }

    public EstadoGeocodificacion estado() {
        EstadoGeocodificacion e = ultimo;
        return e.conPendientes(lugares.countByPuntoIsNullAndEstadoGeo(PENDIENTE));
    }

    private void correr(int total) {
        int procesados = 0;
        int encontrados = 0;
        int noEncontrados = 0;
        String error = null;
        try {
            while (procesados < total) {
                List<Lugar> lote = lugares.findByPuntoIsNullAndEstadoGeoOrderById(PENDIENTE,
                        PageRequest.of(0, Math.min(20, total - procesados)));
                if (lote.isEmpty()) {
                    break;
                }
                for (Lugar l : lote) {
                    boolean encontrado = geocodificar(l.getId());
                    procesados++;
                    if (encontrado) {
                        encontrados++;
                    } else {
                        noEncontrados++;
                    }
                    ultimo = new EstadoGeocodificacion(true, total, procesados, encontrados, noEncontrados,
                            null, ultimo.inicio(), null, null);
                    Thread.sleep(pausaMs);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            error = "Interrumpido";
        } catch (RuntimeException e) {
            log.warn("Geocodificación detenida", e);
            error = e.getMessage();
        } finally {
            ultimo = new EstadoGeocodificacion(false, total, procesados, encontrados, noEncontrados, null,
                    ultimo.inicio(), OffsetDateTime.now(), error);
            enCurso.set(false);
        }
    }

    /** Busca y guarda un lugar en su propia transacción (lo ya procesado no se pierde si el proceso se corta). */
    private boolean geocodificar(Long idLugar) {
        Boolean r = tx.execute(s -> {
            Lugar l = lugares.findById(idLugar).orElse(null);
            if (l == null || l.getPunto() != null) {
                return false;
            }
            String localidad = l.getLocalidad() != null ? l.getLocalidad().getNombre() : "Santa Fe";
            String provincia = l.getLocalidad() != null ? l.getLocalidad().getProvincia() : "Santa Fe";
            Optional<Coordenadas> c = geocodificador.buscar(l.getNombre(), localidad, provincia);
            if (c.isPresent()) {
                l.setPunto(Geo.punto(c.get()));
                l.setEstadoGeo(AUTOMATICO);
                return true;
            }
            l.setEstadoGeo(NO_ENCONTRADO);
            return false;
        });
        return Boolean.TRUE.equals(r);
    }

    @PreDestroy
    void cerrar() {
        ejecutor.shutdownNow();
    }
}
