package ar.sf.sicat.admin.lugar;

import static ar.sf.sicat.admin.comun.Entidades.buscar;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import ar.sf.sicat.admin.comun.lote.Lotes;
import ar.sf.sicat.admin.comun.lote.ResultadoLoteDto;
import ar.sf.sicat.core.comun.ReglaNegocioException;
import ar.sf.sicat.core.dominio.lugar.Lugar;
import ar.sf.sicat.core.dominio.lugar.LugarRepository;

/**
 * Confirmación de ubicaciones en lote: solo las AUTOMATICO con coordenadas (OpenStreetMap encontró la dirección con
 * su número). Las APROXIMADO, NO_ENCONTRADO o PENDIENTE hay que mirarlas una por una en el mapa. Cada lugar en su
 * propia transacción, por JPA (queda auditado).
 */
@Service
public class VerificacionLoteService {

    private final LugarRepository lugares;
    private final TransactionTemplate transacciones;

    public VerificacionLoteService(LugarRepository lugares, TransactionTemplate transacciones) {
        this.lugares = lugares;
        this.transacciones = transacciones;
    }

    public ResultadoLoteDto verificar(List<Long> ids) {
        Map<Long, String> nombres = lugares.findAllById(ids).stream()
                .collect(Collectors.toMap(Lugar::getId, Lugar::getNombre));
        return Lotes.procesar(transacciones, ids, nombres, this::verificarUno);
    }

    private void verificarUno(Long id) {
        Lugar l = buscar(lugares, id, "Lugar");
        if (l.getPunto() == null) {
            throw new ReglaNegocioException("No tiene coordenadas: ubicalo en el mapa");
        }
        if ("VERIFICADO".equals(l.getEstadoGeo())) {
            return; // ya estaba: no es un error
        }
        if (!"AUTOMATICO".equals(l.getEstadoGeo())) {
            throw new ReglaNegocioException("Solo se confirman en lote las ubicaciones automáticas; esta es "
                    + (l.getEstadoGeo() == null ? "manual" : l.getEstadoGeo()) + ": revisala en el mapa");
        }
        l.setEstadoGeo("VERIFICADO");
    }
}
