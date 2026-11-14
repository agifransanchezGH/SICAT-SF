package ar.sf.sicat.admin.importacion.geo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import ar.sf.sicat.core.comun.DatoInvalidoException;
import ar.sf.sicat.core.comun.Geo;
import ar.sf.sicat.core.dominio.lugar.Lugar;
import ar.sf.sicat.core.dominio.lugar.LugarRepository;
import jakarta.annotation.PreDestroy;

/**
 * Geocodifica en segundo plano los lugares en los estados pedidos (por defecto PENDIENTE). Resultado:
 * <ul>
 *   <li>AUTOMATICO: se encontró la dirección; hay que verificarla en el mapa antes de publicar;</li>
 *   <li>APROXIMADO: se encontró la calle o la zona pero no el número; verificar primero estas;</li>
 *   <li>NO_ENCONTRADO: hay que ubicarlo a mano.</li>
 * </ul>
 * Nunca toca los lugares VERIFICADO ni los cargados a mano (sin estado). Solo corre un proceso a la vez.
 */
@Service
public class GeocodificacionService {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String AUTOMATICO = "AUTOMATICO";
    public static final String APROXIMADO = "APROXIMADO";
    public static final String NO_ENCONTRADO = "NO_ENCONTRADO";
    /** Estados que se pueden (re)procesar. */
    public static final Set<String> REPROCESABLES = Set.of(PENDIENTE, NO_ENCONTRADO, APROXIMADO, AUTOMATICO);

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
                                  @Value("${sicat.geocodificacion.pausa-ms:0}") long pausaMs) {
        this.lugares = lugares;
        this.geocodificador = geocodificador;
        this.tx = tx;
        this.pausaMs = pausaMs;
    }

    /** Arranca un proceso para hasta {@code limite} lugares en esos estados. 409 si ya hay uno en curso. */
    public EstadoGeocodificacion iniciar(int limite, Set<String> estados) {
        if (estados.isEmpty() || !REPROCESABLES.containsAll(estados)) {
            throw new DatoInvalidoException("estados debe ser una lista de " + REPROCESABLES);
        }
        if (!enCurso.compareAndSet(false, true)) {
            throw new ConflictoException("Ya hay una geocodificación en curso");
        }
        int total = (int) Math.min(limite, lugares.countByEstadoGeoIn(estados));
        ultimo = new EstadoGeocodificacion(true, total, 0, 0, 0, 0, null, OffsetDateTime.now(), null, null);
        ejecutor.submit(() -> correr(total, estados));
        return estado();
    }

    public EstadoGeocodificacion estado() {
        return ultimo.conPendientes(lugares.countByEstadoGeo(PENDIENTE));
    }

    private void correr(int total, Set<String> estados) {
        int procesados = 0;
        int encontrados = 0;
        int aproximados = 0;
        int noEncontrados = 0;
        long ultimoId = 0L;
        String error = null;
        try {
            while (procesados < total) {
                List<Lugar> lote = lugares.findByEstadoGeoInAndIdGreaterThanOrderById(estados, ultimoId,
                        PageRequest.of(0, Math.min(20, total - procesados)));
                if (lote.isEmpty()) {
                    break;
                }
                for (Lugar l : lote) {
                    ultimoId = l.getId();
                    String resultado = geocodificar(l.getId());
                    procesados++;
                    switch (resultado) {
                        case AUTOMATICO -> encontrados++;
                        case APROXIMADO -> aproximados++;
                        default -> noEncontrados++;
                    }
                    ultimo = new EstadoGeocodificacion(true, total, procesados, encontrados, aproximados,
                            noEncontrados, null, ultimo.inicio(), null, null);
                    if (pausaMs > 0) {
                        Thread.sleep(pausaMs);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            error = "Interrumpido";
        } catch (RuntimeException e) {
            log.warn("Geocodificación detenida", e);
            error = e.getMessage();
        } finally {
            ultimo = new EstadoGeocodificacion(false, total, procesados, encontrados, aproximados, noEncontrados,
                    null, ultimo.inicio(), OffsetDateTime.now(), error);
            enCurso.set(false);
        }
    }

    /** Busca y guarda un lugar en su propia transacción (lo ya procesado no se pierde si el proceso se corta). */
    private String geocodificar(Long idLugar) {
        return tx.execute(s -> {
            Lugar l = lugares.findById(idLugar).orElse(null);
            if (l == null || !REPROCESABLES.contains(l.getEstadoGeo())) {
                return NO_ENCONTRADO;
            }
            String localidad = l.getLocalidad() != null ? l.getLocalidad().getNombre() : "Santa Fe";
            String provincia = l.getLocalidad() != null ? l.getLocalidad().getProvincia() : "Santa Fe";
            Optional<Geocodificador.Resultado> r = geocodificador.buscar(
                    new Geocodificador.Consulta(l.getNombre(), l.getCalle(), l.getAltura(), localidad, provincia));
            if (r.isPresent()) {
                l.setPunto(Geo.punto(r.get().coordenadas()));
                l.setEstadoGeo(r.get().aproximado() ? APROXIMADO : AUTOMATICO);
            } else {
                l.setPunto(null);
                l.setEstadoGeo(NO_ENCONTRADO);
            }
            return l.getEstadoGeo();
        });
    }

    @PreDestroy
    void cerrar() {
        ejecutor.shutdownNow();
    }
}
