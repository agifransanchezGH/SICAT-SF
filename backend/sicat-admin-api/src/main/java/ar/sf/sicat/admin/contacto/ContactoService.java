package ar.sf.sicat.admin.contacto;

import static ar.sf.sicat.admin.comun.Entidades.buscar;
import static ar.sf.sicat.admin.comun.Entidades.texto;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.sf.sicat.admin.contacto.dto.CanalContactoDto;
import ar.sf.sicat.admin.contacto.dto.CanalContactoSolicitud;
import ar.sf.sicat.core.comun.ReglaNegocioException;
import ar.sf.sicat.core.dominio.contacto.CanalContacto;
import ar.sf.sicat.core.dominio.contacto.CanalContactoRepository;
import ar.sf.sicat.core.dominio.contacto.TipoCanalRepository;
import ar.sf.sicat.core.dominio.establecimiento.Establecimiento;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoRepository;
import ar.sf.sicat.core.dominio.oferente.Oferente;
import ar.sf.sicat.core.dominio.oferente.OferenteRepository;

/** Canales de contacto de un establecimiento o de un oferente (exactamente uno de los dos). */
@Service
public class ContactoService {

    private final CanalContactoRepository canales;
    private final TipoCanalRepository tiposCanal;
    private final EstablecimientoRepository establecimientos;
    private final OferenteRepository oferentes;

    public ContactoService(CanalContactoRepository canales, TipoCanalRepository tiposCanal,
                           EstablecimientoRepository establecimientos, OferenteRepository oferentes) {
        this.canales = canales;
        this.tiposCanal = tiposCanal;
        this.establecimientos = establecimientos;
        this.oferentes = oferentes;
    }

    @Transactional(readOnly = true)
    public List<CanalContactoDto> deEstablecimiento(Long idEstablecimiento) {
        buscar(establecimientos, idEstablecimiento, "Establecimiento");
        return dtos(canales.findByEstablecimientoIdOrderByOrden(idEstablecimiento));
    }

    @Transactional(readOnly = true)
    public List<CanalContactoDto> deOferente(Long idOferente) {
        buscar(oferentes, idOferente, "Oferente");
        return dtos(canales.findByOferenteIdOrderByOrden(idOferente));
    }

    @Transactional
    public List<CanalContactoDto> reemplazarDeEstablecimiento(Long idEstablecimiento, List<CanalContactoSolicitud> lista) {
        Establecimiento e = buscar(establecimientos, idEstablecimiento, "Establecimiento");
        reemplazar(canales.findByEstablecimientoIdOrderByOrden(idEstablecimiento), lista, c -> c.setEstablecimiento(e));
        return deEstablecimiento(idEstablecimiento);
    }

    @Transactional
    public List<CanalContactoDto> reemplazarDeOferente(Long idOferente, List<CanalContactoSolicitud> lista) {
        Oferente o = buscar(oferentes, idOferente, "Oferente");
        reemplazar(canales.findByOferenteIdOrderByOrden(idOferente), lista, c -> c.setOferente(o));
        return deOferente(idOferente);
    }

    /** Modifica los que vienen con id, crea los nuevos y elimina los que no vienen (auditoría limpia). */
    private void reemplazar(List<CanalContacto> actuales, List<CanalContactoSolicitud> lista,
                            java.util.function.Consumer<CanalContacto> asignarDuenio) {
        Map<Long, CanalContacto> porId = actuales.stream()
                .collect(Collectors.toMap(CanalContacto::getId, Function.identity()));
        Set<Long> conservados = new HashSet<>();
        short posicion = 0;
        for (CanalContactoSolicitud s : lista) {
            posicion++;
            CanalContacto c;
            if (s.id() != null) {
                c = porId.get(s.id());
                if (c == null) {
                    throw new ReglaNegocioException("El canal " + s.id() + " no pertenece a este registro");
                }
                if (!conservados.add(s.id())) {
                    throw new ReglaNegocioException("El canal " + s.id() + " está repetido en la lista");
                }
            } else {
                c = new CanalContacto();
                asignarDuenio.accept(c);
            }
            c.setTipoCanal(buscar(tiposCanal, s.tipoCanalId(), "Tipo de canal"));
            c.setValor(s.valor().trim());
            c.setUso(texto(s.uso()));
            c.setEsPublico(Boolean.TRUE.equals(s.esPublico()));
            c.setOrden(s.orden() != null ? s.orden() : posicion);
            if (c.getId() == null) {
                canales.save(c);
            }
        }
        actuales.stream().filter(c -> !conservados.contains(c.getId())).forEach(canales::delete);
        canales.flush();
    }

    private static List<CanalContactoDto> dtos(List<CanalContacto> lista) {
        return lista.stream()
                .sorted(Comparator.comparing(CanalContacto::getOrden, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CanalContacto::getId))
                .map(c -> new CanalContactoDto(c.getId(), c.getTipoCanal().getId(), c.getTipoCanal().getNombre(),
                        c.getValor(), c.getUso(), Boolean.TRUE.equals(c.getEsPublico()), c.getOrden()))
                .toList();
    }
}
