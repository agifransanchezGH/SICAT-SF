package ar.sf.sicat.admin.publicacion;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import ar.sf.sicat.admin.comun.lote.Lotes;
import ar.sf.sicat.admin.comun.lote.ResultadoLoteDto;
import ar.sf.sicat.core.dominio.establecimiento.Establecimiento;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoRepository;

/**
 * Publicación en lote. Cada establecimiento pasa por {@link PublicacionService#publicar} en su propia transacción:
 * mismos requisitos (ADR 0008) y misma auditoría que el botón individual, y uno rechazado no frena a los demás.
 */
@Service
public class PublicacionLoteService {

    private final PublicacionService publicacion;
    private final EstablecimientoRepository establecimientos;
    private final TransactionTemplate transacciones;

    public PublicacionLoteService(PublicacionService publicacion, EstablecimientoRepository establecimientos,
                                  TransactionTemplate transacciones) {
        this.publicacion = publicacion;
        this.establecimientos = establecimientos;
        this.transacciones = transacciones;
    }

    public ResultadoLoteDto publicar(List<Long> ids) {
        Map<Long, String> nombres = establecimientos.findAllById(ids).stream()
                .collect(Collectors.toMap(Establecimiento::getId, Establecimiento::getNombre));
        return Lotes.procesar(transacciones, ids, nombres, publicacion::publicar);
    }
}
