package ar.sf.sicat.core.dominio.oferente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OferenteRepository extends JpaRepository<Oferente, Long>, JpaSpecificationExecutor<Oferente> {
}
