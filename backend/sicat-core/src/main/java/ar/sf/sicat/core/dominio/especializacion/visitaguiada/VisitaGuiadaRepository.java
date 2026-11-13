package ar.sf.sicat.core.dominio.especializacion.visitaguiada;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitaGuiadaRepository extends JpaRepository<VisitaGuiada, Long> {

    List<VisitaGuiada> findByOfertaIdOrderById(Long idOferta);
}
