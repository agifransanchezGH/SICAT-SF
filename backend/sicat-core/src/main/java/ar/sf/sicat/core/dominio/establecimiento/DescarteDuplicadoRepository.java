package ar.sf.sicat.core.dominio.establecimiento;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DescarteDuplicadoRepository extends JpaRepository<DescarteDuplicado, Long> {

    boolean existsByClaveAndIntegrantes(String clave, String integrantes);
}
