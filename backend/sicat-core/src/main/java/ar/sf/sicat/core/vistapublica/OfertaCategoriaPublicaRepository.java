package ar.sf.sicat.core.vistapublica;

import java.util.List;

public interface OfertaCategoriaPublicaRepository extends VistaRepository<OfertaCategoriaPublica, ClaveCompuesta.OfertaCategoria> {

    List<OfertaCategoriaPublica> findByIdOfertaIn(List<Long> idsOferta);
}
