package ar.sf.sicat.core.dominio.contacto;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoCanalRepository extends JpaRepository<TipoCanal, Long> {

    Optional<TipoCanal> findByNombreIgnoreCase(String nombre);

    List<TipoCanal> findByActivoTrue();
}
