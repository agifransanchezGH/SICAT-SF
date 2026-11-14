package ar.sf.sicat.core.vistapublica;

import java.util.Collection;
import java.util.List;

public interface AlojamientoPublicoRepository extends VistaRepository<AlojamientoPublico, Long> {

    List<AlojamientoPublico> findByIdOfertaIn(Collection<Long> idsOferta);
}
