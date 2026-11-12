package ar.sf.sicat.core.dominio.oferta;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {

    List<Oferta> findByEstablecimientoId(Long idEstablecimiento);
}
