package ar.sf.sicat.core.dominio.especializacion.salon;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SalonRepository extends JpaRepository<Salon, Long> {

    List<Salon> findByOfertaIdOrderByNombre(Long idOferta);
}
