package ar.sf.sicat.core.dominio.establecimiento;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EstablecimientoCaracteristicaRepository extends JpaRepository<EstablecimientoCaracteristica, EstablecimientoCaracteristica.Id> {

    List<EstablecimientoCaracteristica> findByEstablecimientoId(Long idEstablecimiento);

    List<EstablecimientoCaracteristica> findByCaracteristicaId(Long idCaracteristica);

    /** Características de varios establecimientos con su catálogo, en una consulta. */
    @Query("SELECT ec FROM EstablecimientoCaracteristica ec JOIN FETCH ec.caracteristica"
            + " WHERE ec.establecimiento.id IN :ids")
    List<EstablecimientoCaracteristica> deEstablecimientos(
            @org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);

    /** Cantidad de establecimientos por característica: filas [id_caracteristica, cantidad]. */
    @Query("SELECT ec.caracteristica.id, COUNT(ec) FROM EstablecimientoCaracteristica ec GROUP BY ec.caracteristica.id")
    List<Object[]> contarPorCaracteristica();
}
