package ar.sf.sicat.core.vistapublica;

import java.util.List;

public interface HorarioPublicoRepository extends VistaRepository<HorarioPublico, Long> {

    List<HorarioPublico> findByIdOfertaInOrderByDiaSemanaAscHoraDesdeAsc(List<Long> idsOferta);
}
