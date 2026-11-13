package ar.sf.sicat.core.dominio.especializacion.circuito;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CircuitoParadaRepository extends JpaRepository<CircuitoParada, CircuitoParada.Id> {

    List<CircuitoParada> findByCircuitoIdOrderByIdOrden(Long idOferta);
}
