package ar.sf.sicat.core.dominio.establecimiento;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DistincionRepository extends JpaRepository<Distincion, Long> {

    Optional<Distincion> findByNombreIgnoreCase(String nombre);

    List<Distincion> findByActivoTrue();
}
