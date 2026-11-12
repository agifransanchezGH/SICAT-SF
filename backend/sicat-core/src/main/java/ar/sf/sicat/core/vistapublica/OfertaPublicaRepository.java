package ar.sf.sicat.core.vistapublica;

import java.util.List;

public interface OfertaPublicaRepository extends VistaRepository<OfertaPublica, Long> {

    List<OfertaPublica> findByIdEstablecimiento(Long idEstablecimiento);
}
