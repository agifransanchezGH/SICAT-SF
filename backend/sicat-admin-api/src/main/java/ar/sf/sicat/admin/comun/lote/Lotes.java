package ar.sf.sicat.admin.comun.lote;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.support.TransactionTemplate;

import ar.sf.sicat.core.comun.ConflictoException;
import ar.sf.sicat.core.comun.DatoInvalidoException;
import ar.sf.sicat.core.comun.EntidadNoEncontradaException;
import ar.sf.sicat.core.comun.ReglaNegocioException;

/**
 * Procesa una acción elemento por elemento, cada uno en su propia transacción: si uno falla (regla de negocio,
 * dato inválido, conflicto o inexistente) se revierte solo ese y los demás siguen. El resultado detalla cada id.
 * Los cambios van por JPA, así que la auditoría los registra igual que una acción individual.
 */
public final class Lotes {

    /** Acción sobre un elemento; lanza las excepciones de dominio para rechazarlo. */
    @FunctionalInterface
    public interface Accion {
        void aplicar(Long id);
    }

    private Lotes() {
    }

    public static ResultadoLoteDto procesar(TransactionTemplate transacciones, List<Long> ids,
                                            Map<Long, String> nombres, Accion accion) {
        List<ResultadoLoteDto.Item> resultados = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(ids)) {
            String nombre = nombres.get(id);
            if (nombre == null) {
                resultados.add(new ResultadoLoteDto.Item(id, null, false, List.of("No existe")));
                continue;
            }
            try {
                transacciones.executeWithoutResult(estado -> accion.aplicar(id));
                resultados.add(new ResultadoLoteDto.Item(id, nombre, true, List.of()));
            } catch (ReglaNegocioException e) {
                List<String> errores = e.getErrores().isEmpty() ? List.of(e.getMessage()) : e.getErrores();
                resultados.add(new ResultadoLoteDto.Item(id, nombre, false, errores));
            } catch (DatoInvalidoException | ConflictoException | EntidadNoEncontradaException e) {
                resultados.add(new ResultadoLoteDto.Item(id, nombre, false, List.of(e.getMessage())));
            }
        }
        return ResultadoLoteDto.de(resultados);
    }
}
