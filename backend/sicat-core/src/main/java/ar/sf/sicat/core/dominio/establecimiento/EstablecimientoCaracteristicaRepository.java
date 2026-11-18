package ar.sf.sicat.core.dominio.establecimiento;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EstablecimientoCaracteristicaRepository extends JpaRepository<EstablecimientoCaracteristica, EstablecimientoCaracteristica.Id> {

    List<EstablecimientoCaracteristica> findByEstablecimientoId(Long idEstablecimiento);

    List<EstablecimientoCaracteristica> findByCaracteristicaId(Long idCaracteristica);

    /** Cantidad de establecimientos por característica: filas [id_caracteristica, cantidad]. */
    @Query("SELECT ec.caracteristica.id, COUNT(ec) FROM EstablecimientoCaracteristica ec GROUP BY ec.caracteristica.id")
    List<Object[]> contarPorCaracteristica();
}
