package ar.sf.sicat.core.vistapublica;

import java.util.List;

public interface VisitaGuiadaPublicaRepository extends VistaRepository<VisitaGuiadaPublica, Long> {

    List<VisitaGuiadaPublica> findByIdOfertaIn(List<Long> idsOferta);
}
