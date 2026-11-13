package ar.sf.sicat.core.dominio.oferente;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoOferenteRepository extends JpaRepository<TipoOferente, Long> {

    Optional<TipoOferente> findByNombreIgnoreCase(String nombre);

    List<TipoOferente> findByActivoTrue();
}
