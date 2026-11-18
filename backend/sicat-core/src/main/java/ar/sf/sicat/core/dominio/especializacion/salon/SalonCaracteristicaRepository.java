package ar.sf.sicat.core.dominio.especializacion.salon;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SalonCaracteristicaRepository extends JpaRepository<SalonCaracteristica, SalonCaracteristica.Id> {

    List<SalonCaracteristica> findBySalonId(Long idSalon);

    List<SalonCaracteristica> findByCaracteristicaId(Long idCaracteristica);

    /** Cantidad de salones por característica: filas [id_caracteristica, cantidad]. */
    @Query("SELECT sc.caracteristica.id, COUNT(sc) FROM SalonCaracteristica sc GROUP BY sc.caracteristica.id")
    List<Object[]> contarPorCaracteristica();
}
