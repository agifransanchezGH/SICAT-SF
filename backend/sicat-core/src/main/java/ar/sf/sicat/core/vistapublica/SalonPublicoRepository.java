package ar.sf.sicat.core.vistapublica;

import java.util.List;

public interface SalonPublicoRepository extends VistaRepository<SalonPublico, Long> {

    List<SalonPublico> findByIdOfertaIn(List<Long> idsOferta);
}
