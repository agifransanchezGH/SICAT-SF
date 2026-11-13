package ar.sf.sicat.admin.oferta;

import static ar.sf.sicat.admin.comun.Entidades.buscar;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.sf.sicat.admin.oferta.dto.HorarioDto;
import ar.sf.sicat.admin.oferta.dto.HorarioSolicitud;
import ar.sf.sicat.core.comun.ReglaNegocioException;
import ar.sf.sicat.core.dominio.especializacion.visitaguiada.VisitaGuiada;
import ar.sf.sicat.core.dominio.especializacion.visitaguiada.VisitaGuiadaRepository;
import ar.sf.sicat.core.dominio.horario.Horario;
import ar.sf.sicat.core.dominio.horario.HorarioRepository;
import ar.sf.sicat.core.dominio.oferta.Oferta;
import ar.sf.sicat.core.dominio.oferta.OfertaRepository;

/** Horarios generales de una oferta y horarios de una visita guiada puntual. */
@Service
public class HorarioService {

    private final HorarioRepository horarios;
    private final OfertaRepository ofertas;
    private final VisitaGuiadaRepository visitas;

    public HorarioService(HorarioRepository horarios, OfertaRepository ofertas, VisitaGuiadaRepository visitas) {
        this.horarios = horarios;
        this.ofertas = ofertas;
        this.visitas = visitas;
    }

    @Transactional(readOnly = true)
    public List<HorarioDto> deOferta(Long idOferta) {
        buscar(ofertas, idOferta, "Oferta");
        return dtos(horarios.findByOfertaIdAndVisitaIsNullOrderByDiaSemanaAscHoraDesdeAsc(idOferta));
    }

    @Transactional(readOnly = true)
    public List<HorarioDto> deVisita(Long idVisita) {
        buscar(visitas, idVisita, "Visita guiada");
        return dtos(horarios.findByVisitaIdOrderByDiaSemanaAscHoraDesdeAsc(idVisita));
    }

    @Transactional
    public List<HorarioDto> reemplazarDeOferta(Long idOferta, List<HorarioSolicitud> lista) {
        Oferta oferta = buscar(ofertas, idOferta, "Oferta");
        reemplazar(horarios.findByOfertaIdAndVisitaIsNullOrderByDiaSemanaAscHoraDesdeAsc(idOferta), lista, oferta, null);
        return deOferta(idOferta);
    }

    @Transactional
    public List<HorarioDto> reemplazarDeVisita(Long idVisita, List<HorarioSolicitud> lista) {
        VisitaGuiada visita = buscar(visitas, idVisita, "Visita guiada");
        reemplazar(horarios.findByVisitaIdOrderByDiaSemanaAscHoraDesdeAsc(idVisita), lista, visita.getOferta(), visita);
        return deVisita(idVisita);
    }

    /** Elimina los horarios de una visita (antes de eliminar la visita). */
    @Transactional
    public void eliminarDeVisita(Long idVisita) {
        horarios.deleteAll(horarios.findByVisitaIdOrderByDiaSemanaAscHoraDesdeAsc(idVisita));
        horarios.flush();
    }

    private void reemplazar(List<Horario> actuales, List<HorarioSolicitud> lista, Oferta oferta, VisitaGuiada visita) {
        Map<Long, Horario> porId = actuales.stream().collect(Collectors.toMap(Horario::getId, Function.identity()));
        Set<Long> conservados = new HashSet<>();
        for (HorarioSolicitud s : lista) {
            if (s.desde().equals(s.hasta())) {
                throw new ReglaNegocioException("La franja del día " + s.diaSemana() + " empieza y termina a la misma hora");
            }
            Horario h;
            if (s.id() != null) {
                h = porId.get(s.id());
                if (h == null || !conservados.add(s.id())) {
                    throw new ReglaNegocioException("El horario " + s.id() + " no pertenece a esta lista o está repetido");
                }
            } else {
                h = new Horario();
                h.setOferta(oferta);
                h.setVisita(visita);
            }
            h.setDiaSemana(s.diaSemana());
            h.setHoraDesde(s.desde());
            h.setHoraHasta(s.hasta());
            if (h.getId() == null) {
                horarios.save(h);
            }
        }
        actuales.stream().filter(h -> !conservados.contains(h.getId())).forEach(horarios::delete);
        horarios.flush();
    }

    static List<HorarioDto> dtos(List<Horario> lista) {
        return lista.stream()
                .map(h -> new HorarioDto(h.getId(), h.getDiaSemana(), h.getHoraDesde(), h.getHoraHasta()))
                .toList();
    }
}
