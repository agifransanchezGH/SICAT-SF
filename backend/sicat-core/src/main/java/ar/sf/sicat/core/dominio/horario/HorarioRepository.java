package ar.sf.sicat.core.dominio.horario;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HorarioRepository extends JpaRepository<Horario, Long> {

    /** Horarios generales de la oferta (sin visita). */
    List<Horario> findByOfertaIdAndVisitaIsNullOrderByDiaSemanaAscHoraDesdeAsc(Long idOferta);

    List<Horario> findByVisitaIdOrderByDiaSemanaAscHoraDesdeAsc(Long idVisita);
}
