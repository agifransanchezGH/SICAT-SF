package ar.sf.sicat.admin.lugar;

import static ar.sf.sicat.admin.comun.Entidades.buscar;
import static ar.sf.sicat.admin.comun.Entidades.buscarOpcional;
import static ar.sf.sicat.admin.comun.Entidades.texto;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.sf.sicat.admin.lugar.dto.LugarDto;
import ar.sf.sicat.admin.lugar.dto.LugarSolicitud;
import ar.sf.sicat.admin.publicacion.PublicacionService;
import ar.sf.sicat.core.comun.Bbox;
import ar.sf.sicat.core.comun.ConflictoException;
import ar.sf.sicat.core.comun.Geo;
import ar.sf.sicat.core.comun.GeoJson;
import ar.sf.sicat.core.comun.PaginaDto;
import ar.sf.sicat.core.comun.ReglaNegocioException;
import ar.sf.sicat.core.dominio.especializacion.circuito.CircuitoRepository;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoRepository;
import ar.sf.sicat.core.dominio.lugar.LocalidadRepository;
import ar.sf.sicat.core.dominio.lugar.Lugar;
import ar.sf.sicat.core.dominio.lugar.LugarRepository;
import ar.sf.sicat.core.dominio.lugar.TipoLugarRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class LugarService {

    private final LugarRepository lugares;
    private final TipoLugarRepository tipos;
    private final LocalidadRepository localidades;
    private final EstablecimientoRepository establecimientos;
    private final CircuitoRepository circuitos;
    private final PublicacionService publicacion;

    public LugarService(LugarRepository lugares, TipoLugarRepository tipos, LocalidadRepository localidades,
                        EstablecimientoRepository establecimientos, CircuitoRepository circuitos,
                        PublicacionService publicacion) {
        this.lugares = lugares;
        this.tipos = tipos;
        this.localidades = localidades;
        this.establecimientos = establecimientos;
        this.circuitos = circuitos;
        this.publicacion = publicacion;
    }

    @Transactional(readOnly = true)
    public PaginaDto<LugarDto> listar(String q, Long tipoLugarId, Long padreId, Long localidadId, String bbox,
                                      Pageable pageable) {
        Bbox caja = Bbox.parse(bbox);
        List<Long> enCaja = caja == null ? null
                : lugares.idsEnBbox(caja.minLng(), caja.minLat(), caja.maxLng(), caja.maxLat());
        Specification<Lugar> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String patron = "%" + q.trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(cb.lower(root.get("calle")), patron)));
            }
            if (tipoLugarId != null) {
                p.add(cb.equal(root.get("tipoLugar").get("id"), tipoLugarId));
            }
            if (padreId != null) {
                p.add(cb.equal(root.get("lugarPadre").get("id"), padreId));
            }
            if (localidadId != null) {
                p.add(cb.equal(root.get("localidad").get("id"), localidadId));
            }
            if (enCaja != null) {
                p.add(enCaja.isEmpty() ? cb.disjunction() : root.get("id").in(enCaja));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PaginaDto.de(lugares.findAll(spec, pageable), l -> dto(l, false));
    }

    @Transactional(readOnly = true)
    public LugarDto detalle(Long id) {
        return dto(buscar(lugares, id, "Lugar"), true);
    }

    @Transactional
    public LugarDto crear(LugarSolicitud s) {
        Lugar l = new Lugar();
        aplicar(l, s);
        return dto(lugares.save(l), true);
    }

    @Transactional
    public LugarDto modificar(Long id, LugarSolicitud s) {
        Lugar l = buscar(lugares, id, "Lugar");
        aplicar(l, s);
        publicacion.verificarPublicados(establecimientos.findByLugarId(id));
        return dto(l, true);
    }

    /** Lugar no tiene baja lógica: se elimina solo si nadie lo usa. */
    @Transactional
    public void eliminar(Long id) {
        Lugar l = buscar(lugares, id, "Lugar");
        if (lugares.existsByLugarPadreId(id)) {
            throw new ConflictoException("El lugar contiene otros lugares");
        }
        if (establecimientos.existsByLugarId(id)) {
            throw new ConflictoException("El lugar está asignado a uno o más establecimientos");
        }
        if (circuitos.existsByLugarPartidaId(id)) {
            throw new ConflictoException("El lugar es punto de partida de un circuito");
        }
        lugares.delete(l);
    }

    private void aplicar(Lugar l, LugarSolicitud s) {
        Lugar padre = buscarOpcional(lugares, s.padreId(), "Lugar padre");
        for (Lugar p = padre; p != null; p = p.getLugarPadre()) {
            if (l.getId() != null && p.getId().equals(l.getId())) {
                throw new ReglaNegocioException("Un lugar no puede quedar dentro de sí mismo o de un lugar que contiene");
            }
        }
        l.setNombre(s.nombre().trim());
        l.setLugarPadre(padre);
        l.setTipoLugar(buscarOpcional(tipos, s.tipoLugarId(), "Tipo de lugar"));
        l.setLocalidad(buscarOpcional(localidades, s.localidadId(), "Localidad"));
        l.setCalle(texto(s.calle()));
        l.setAltura(s.altura());
        l.setKm(s.km());
        l.setCalleCruce(texto(s.calleCruce()));
        l.setComplemento(texto(s.complemento()));
        l.setPunto(Geo.punto(s.coordenadas()));
        l.setGeometria(GeoJson.aGeometria(s.geometria()));
        l.setEstadoGeo(texto(s.estadoGeo()));
    }

    LugarDto dto(Lugar l, boolean conHijos) {
        List<LugarDto.LugarRef> hijos = !conHijos || l.getId() == null ? null
                : lugares.findByLugarPadreIdOrderByNombre(l.getId()).stream()
                        .map(h -> new LugarDto.LugarRef(h.getId(), h.getNombre())).toList();
        return new LugarDto(l.getId(), l.getNombre(),
                l.getLugarPadre() == null ? null : l.getLugarPadre().getId(),
                l.getLugarPadre() == null ? null : l.getLugarPadre().getNombre(),
                l.getTipoLugar() == null ? null : l.getTipoLugar().getId(),
                l.getTipoLugar() == null ? null : l.getTipoLugar().getNombre(),
                l.getLocalidad() == null ? null : l.getLocalidad().getId(),
                l.getLocalidad() == null ? null : l.getLocalidad().getNombre(),
                l.getCalle(), l.getAltura(), l.getKm(), l.getCalleCruce(), l.getComplemento(),
                Geo.coordenadas(l.getPunto()), GeoJson.aGeoJson(l.getGeometria()), l.getEstadoGeo(), hijos);
    }
}
