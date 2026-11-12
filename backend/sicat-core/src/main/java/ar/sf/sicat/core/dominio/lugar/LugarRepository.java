package ar.sf.sicat.core.dominio.lugar;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LugarRepository extends JpaRepository<Lugar, Long>, JpaSpecificationExecutor<Lugar> {

    List<Lugar> findByLugarPadreId(Long idLugarPadre);
}
