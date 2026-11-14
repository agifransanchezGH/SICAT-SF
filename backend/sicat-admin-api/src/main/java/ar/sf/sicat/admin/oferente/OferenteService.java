package ar.sf.sicat.admin.oferente;

import static ar.sf.sicat.admin.comun.Entidades.buscar;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.sf.sicat.admin.oferente.dto.OferenteDto;
import ar.sf.sicat.admin.oferente.dto.OferenteSolicitud;
import ar.sf.sicat.core.comun.PaginaDto;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoRepository;
import ar.sf.sicat.core.dominio.oferente.Oferente;
import ar.sf.sicat.core.dominio.oferente.OferenteRepository;
import ar.sf.sicat.core.dominio.oferente.TipoOferenteRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class OferenteService {

    private final OferenteRepository oferentes;
    private final TipoOferenteRepository tipos;
    private final EstablecimientoRepository establecimientos;

    public OferenteService(OferenteRepository oferentes, TipoOferenteRepository tipos,
                           EstablecimientoRepository establecimientos) {
        this.oferentes = oferentes;
        this.tipos = tipos;
        this.establecimientos = establecimientos;
    }

    @Transactional(readOnly = true)
    public PaginaDto<OferenteDto> listar(String q, Long tipoId, boolean incluirInactivos, Pageable pageable) {
        Specification<Oferente> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                p.add(cb.like(cb.lower(root.get("nombre")), "%" + q.trim().toLowerCase() + "%"));
            }
            if (tipoId != null) {
                p.add(cb.equal(root.get("tipoOferente").get("id"), tipoId));
            }
            if (!incluirInactivos) {
                p.add(cb.isTrue(root.get("activo")));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PaginaDto.de(oferentes.findAll(spec, pageable), o -> dto(o, false));
    }

    @Transactional(readOnly = true)
    public OferenteDto detalle(Long id) {
        return dto(buscar(oferentes, id, "Oferente"), true);
    }

    @Transactional
    public OferenteDto crear(OferenteSolicitud s) {
        Oferente o = new Oferente();
        aplicar(o, s);
        o.setActivo(true);
        return dto(oferentes.save(o), true);
    }

    @Transactional
    public OferenteDto modificar(Long id, OferenteSolicitud s) {
        Oferente o = buscar(oferentes, id, "Oferente");
        aplicar(o, s);
        if (s.activo() != null) {
            o.setActivo(s.activo());
        }
        return dto(o, true);
    }

    /** Baja lógica. Sus establecimientos no cambian (y dejan de ser públicos por el filtro de las vistas). */
    @Transactional
    public void darDeBaja(Long id) {
        buscar(oferentes, id, "Oferente").setActivo(false);
    }

    private void aplicar(Oferente o, OferenteSolicitud s) {
        o.setNombre(s.nombre().trim());
        o.setTipoOferente(buscar(tipos, s.tipoOferenteId(), "Tipo de oferente"));
    }

    private OferenteDto dto(Oferente o, boolean conEstablecimientos) {
        List<OferenteDto.EstablecimientoRef> lista = !conEstablecimientos || o.getId() == null ? null
                : establecimientos.findByOferenteIdOrderByNombre(o.getId()).stream()
                        .map(e -> new OferenteDto.EstablecimientoRef(e.getId(), e.getNombre(),
                                e.getEstadoPublicacion(), Boolean.TRUE.equals(e.getActivo())))
                        .toList();
        return new OferenteDto(o.getId(), o.getNombre(), o.getTipoOferente().getId(), o.getTipoOferente().getNombre(),
                Boolean.TRUE.equals(o.getActivo()), lista);
    }
}
