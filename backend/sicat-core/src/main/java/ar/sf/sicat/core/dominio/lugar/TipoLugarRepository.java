package ar.sf.sicat.core.dominio.lugar;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoLugarRepository extends JpaRepository<TipoLugar, Long> {

    Optional<TipoLugar> findByNombreIgnoreCase(String nombre);

    List<TipoLugar> findByActivoTrue();
}
