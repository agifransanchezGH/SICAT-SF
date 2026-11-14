package ar.sf.sicat.core.vistapublica;

import java.util.Collection;
import java.util.List;

public interface CircuitoPublicoRepository extends VistaRepository<CircuitoPublico, Long> {

    List<CircuitoPublico> findByIdOfertaIn(Collection<Long> idsOferta);
}
