package ar.sf.sicat.core.vistapublica;

import java.util.List;

public interface DistincionPublicaRepository extends VistaRepository<DistincionPublica, ClaveCompuesta.Distincion> {

    List<DistincionPublica> findByIdEstablecimiento(Long idEstablecimiento);
}
