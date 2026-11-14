package ar.sf.sicat.core.dominio.establecimiento;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EstablecimientoCaracteristicaRepository extends JpaRepository<EstablecimientoCaracteristica, EstablecimientoCaracteristica.Id> {

    List<EstablecimientoCaracteristica> findByEstablecimientoId(Long idEstablecimiento);
}
