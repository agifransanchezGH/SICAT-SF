package ar.sf.sicat.core.auditoria;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long>, JpaSpecificationExecutor<Auditoria> {

    List<Auditoria> findByTablaAfectadaAndClaveRegistroOrderByFechaHoraDesc(String tablaAfectada, String claveRegistro);
}
