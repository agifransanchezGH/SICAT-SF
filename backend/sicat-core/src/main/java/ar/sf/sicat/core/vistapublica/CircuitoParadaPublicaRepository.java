package ar.sf.sicat.core.vistapublica;

import java.util.Collection;
import java.util.List;

public interface CircuitoParadaPublicaRepository extends VistaRepository<CircuitoParadaPublica, ClaveCompuesta.CircuitoParada> {

    List<CircuitoParadaPublica> findByIdOfertaOrderByOrden(Long idOferta);

    List<CircuitoParadaPublica> findByIdOfertaInOrderByIdOfertaAscOrdenAsc(Collection<Long> idsOferta);
}
