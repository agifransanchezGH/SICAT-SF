package ar.sf.sicat.core.dominio.establecimiento;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferenteRepository extends JpaRepository<Referente, Long> {

    List<Referente> findByEstablecimientoIdOrderById(Long idEstablecimiento);
}
