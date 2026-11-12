package ar.sf.sicat.core.vistapublica;

import java.util.List;

public interface ContactoPublicoRepository extends VistaRepository<ContactoPublico, ClaveCompuesta.Contacto> {

    List<ContactoPublico> findByIdEstablecimientoOrderByOrden(Long idEstablecimiento);
}
