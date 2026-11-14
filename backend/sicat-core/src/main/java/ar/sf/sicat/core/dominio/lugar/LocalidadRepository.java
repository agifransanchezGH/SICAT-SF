package ar.sf.sicat.core.dominio.lugar;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalidadRepository extends JpaRepository<Localidad, Long> {

    Optional<Localidad> findByNombreIgnoreCaseAndProvinciaIgnoreCase(String nombre, String provincia);

    List<Localidad> findByActivoTrue();
}
