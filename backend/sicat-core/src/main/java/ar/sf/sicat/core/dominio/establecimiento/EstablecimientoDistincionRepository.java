package ar.sf.sicat.core.dominio.establecimiento;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EstablecimientoDistincionRepository extends JpaRepository<EstablecimientoDistincion, EstablecimientoDistincion.Id> {

    List<EstablecimientoDistincion> findByEstablecimientoId(Long idEstablecimiento);
}
