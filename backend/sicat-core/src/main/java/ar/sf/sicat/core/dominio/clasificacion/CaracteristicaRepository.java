package ar.sf.sicat.core.dominio.clasificacion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CaracteristicaRepository extends JpaRepository<Caracteristica, Long> {

    Optional<Caracteristica> findByGrupoIgnoreCaseAndNombreIgnoreCase(String grupo, String nombre);

    List<Caracteristica> findByActivoTrue();
}
