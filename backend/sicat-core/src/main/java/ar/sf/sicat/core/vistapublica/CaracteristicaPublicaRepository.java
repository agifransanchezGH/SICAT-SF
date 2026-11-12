package ar.sf.sicat.core.vistapublica;

import java.util.List;

public interface CaracteristicaPublicaRepository extends VistaRepository<CaracteristicaPublica, ClaveCompuesta.Caracteristica> {

    List<CaracteristicaPublica> findByIdEstablecimiento(Long idEstablecimiento);
}
