package ar.sf.sicat.core.dominio.especializacion.salon;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SalonCaracteristicaRepository extends JpaRepository<SalonCaracteristica, SalonCaracteristica.Id> {

    List<SalonCaracteristica> findBySalonId(Long idSalon);
}
