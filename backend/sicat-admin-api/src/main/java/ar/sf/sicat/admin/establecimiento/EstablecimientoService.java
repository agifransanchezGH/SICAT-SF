package ar.sf.sicat.admin.establecimiento;

import static ar.sf.sicat.admin.comun.Entidades.buscar;
import static ar.sf.sicat.admin.comun.Entidades.buscarOpcional;
import static ar.sf.sicat.admin.comun.Entidades.texto;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.sf.sicat.admin.catalogo.CategoriaService;
import ar.sf.sicat.admin.establecimiento.dto.EstablecimientoDto;
import ar.sf.sicat.admin.establecimiento.dto.EstablecimientoResumenDto;
import ar.sf.sicat.admin.establecimiento.dto.EstablecimientoSolicitud;
import ar.sf.sicat.core.comun.Bbox;
import ar.sf.sicat.core.comun.Geo;
import ar.sf.sicat.core.comun.PaginaDto;
import ar.sf.sicat.core.dominio.clasificacion.Categoria;
import ar.sf.sicat.core.dominio.establecimiento.EstadoPublicacion;
import ar.sf.sicat.core.dominio.establecimiento.Establecimiento;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoRepository;
import ar.sf.sicat.core.dominio.lugar.Lugar;
import ar.sf.sicat.core.dominio.lugar.LugarRepository;
import ar.sf.sicat.core.dominio.oferente.OferenteRepository;
import ar.sf.sicat.core.dominio.oferta.Oferta;
import ar.sf.sicat.core.dominio.oferta.OfertaRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Service
public class EstablecimientoService {

    private final EstablecimientoRepository establecimientos;
    private final OferenteRepository oferentes;
    private final LugarRepository lugares;
    private final OfertaRepository ofertas;
    private final CategoriaService categorias;

    public EstablecimientoService(EstablecimientoRepository establecimientos, OferenteRepository oferentes,
                                  LugarRepository lugares, OfertaRepository ofertas, CategoriaService categorias) {
        this.establecimientos = establecimientos;
        this.oferentes = oferentes;
        this.lugares = lugares;
        this.ofertas = ofertas;
        this.categorias = categorias;
    }

    /**
     * Filtros: q (nombre), categoria (incluye sus subcategorías; principal o adicional de una oferta activa),
     * estado, oferenteId, localidadId (la del lugar), bbox.
     */
    @Transactional(readOnly = true)
    public PaginaDto<EstablecimientoResumenDto> listar(String q, Long categoria, EstadoPublicacion estado,
                                                       Long oferenteId, Long localidadId, String bbox,
                                                       boolean incluirInactivos, Pageable pageable) {
        Bbox caja = Bbox.parse(bbox);
        List<Long> enCaja = caja == null ? null
                : establecimientos.idsEnBbox(caja.minLng(), caja.minLat(), caja.maxLng(), caja.maxLat());
        Set<Long> ramaCategoria = categoria == null ? null : categorias.conDescendientes(categoria);

        Specification<Establecimiento> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                p.add(cb.like(cb.lower(root.get("nombre")), "%" + q.trim().toLowerCase() + "%"));
            }
            if (estado != null) {
                p.add(cb.equal(root.get("estadoPublicacion"), estado));
            }
            if (oferenteId != null) {
                p.add(cb.equal(root.get("oferente").get("id"), oferenteId));
            }
            if (localidadId != null) {
                p.add(cb.equal(root.join("lugar", JoinType.LEFT).get("localidad").get("id"), localidadId));
            }
            if (enCaja != null) {
                p.add(enCaja.isEmpty() ? cb.disjunction() : root.get("id").in(enCaja));
            }
            if (ramaCategoria != null) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<Oferta> o = sub.from(Oferta.class);
                Join<Oferta, Categoria> adicional = o.join("categoriasAdicionales", JoinType.LEFT);
                sub.select(o.get("establecimiento").get("id")).where(
                        cb.isTrue(o.get("activo")),
                        cb.or(o.get("categoria").get("id").in(ramaCategoria), adicional.get("id").in(ramaCategoria)));
                p.add(root.get("id").in(sub));
            }
            if (!incluirInactivos) {
                p.add(cb.isTrue(root.get("activo")));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return PaginaDto.de(establecimientos.findAll(spec, pageable), this::resumen);
    }

    @Transactional(readOnly = true)
    public EstablecimientoDto detalle(Long id) {
        return dto(buscar(establecimientos, id, "Establecimiento"));
    }

    /** Se crea en BORRADOR; se publica en el paso 6 (/publicar). */
    @Transactional
    public EstablecimientoDto crear(EstablecimientoSolicitud s) {
        Establecimiento e = new Establecimiento();
        aplicar(e, s);
        e.setEstadoPublicacion(EstadoPublicacion.BORRADOR);
        e.setActivo(true);
        return dto(establecimientos.save(e));
    }

    @Transactional
    public EstablecimientoDto modificar(Long id, EstablecimientoSolicitud s) {
        Establecimiento e = buscar(establecimientos, id, "Establecimiento");
        aplicar(e, s);
        if (s.activo() != null) {
            e.setActivo(s.activo());
        }
        return dto(e);
    }

    /** Baja lógica: deja de aparecer en listados y en el sitio público. */
    @Transactional
    public void darDeBaja(Long id) {
        buscar(establecimientos, id, "Establecimiento").setActivo(false);
    }

    private void aplicar(Establecimiento e, EstablecimientoSolicitud s) {
        e.setOferente(buscar(oferentes, s.oferenteId(), "Oferente"));
        e.setLugar(buscarOpcional(lugares, s.lugarId(), "Lugar"));
        e.setNombre(s.nombre().trim());
        e.setDescripcion(texto(s.descripcion()));
        e.setAnioInauguracion(s.anioInauguracion());
        e.setEstadoFuncionamiento(texto(s.estadoFuncionamiento()));
        e.setMotivoEstado(texto(s.motivoEstado()));
        e.setFechaReapertura(s.fechaReapertura());
        e.setDestacado(Boolean.TRUE.equals(s.destacado()));
    }

    private EstablecimientoResumenDto resumen(Establecimiento e) {
        Lugar l = e.getLugar();
        List<String> nombresCategorias = ofertas.findByEstablecimientoIdOrderById(e.getId()).stream()
                .filter(o -> Boolean.TRUE.equals(o.getActivo()))
                .flatMap(o -> Stream.concat(Stream.of(o.getCategoria()), o.getCategoriasAdicionales().stream()))
                .map(Categoria::getNombre)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream().toList();
        return new EstablecimientoResumenDto(e.getId(), e.getNombre(), e.getOferente().getId(),
                e.getOferente().getNombre(),
                l == null || l.getLocalidad() == null ? null : l.getLocalidad().getNombre(),
                l == null ? null : Geo.coordenadas(l.getPunto()),
                e.getEstadoPublicacion(), Boolean.TRUE.equals(e.getDestacado()), Boolean.TRUE.equals(e.getActivo()),
                nombresCategorias);
    }

    private EstablecimientoDto dto(Establecimiento e) {
        Lugar l = e.getLugar();
        EstablecimientoDto.LugarRef lugar = l == null ? null : new EstablecimientoDto.LugarRef(l.getId(),
                l.getNombre(), direccion(l), l.getLocalidad() == null ? null : l.getLocalidad().getNombre(),
                Geo.coordenadas(l.getPunto()));
        List<EstablecimientoDto.OfertaRef> listaOfertas = e.getId() == null ? List.of()
                : ofertas.findByEstablecimientoIdOrderById(e.getId()).stream()
                        .map(o -> new EstablecimientoDto.OfertaRef(o.getId(), o.getCategoria().getId(),
                                o.getCategoria().getNombre(), Boolean.TRUE.equals(o.getActivo())))
                        .toList();
        return new EstablecimientoDto(e.getId(), e.getNombre(), e.getDescripcion(), e.getAnioInauguracion(),
                e.getEstadoFuncionamiento(), e.getMotivoEstado(), e.getFechaReapertura(), e.getEstadoPublicacion(),
                Boolean.TRUE.equals(e.getDestacado()), Boolean.TRUE.equals(e.getActivo()),
                new EstablecimientoDto.Ref(e.getOferente().getId(), e.getOferente().getNombre()),
                lugar, listaOfertas);
    }

    /** "Calle 123", "Ruta km 5.5" o "Calle y Cruce", más el complemento. */
    static String direccion(Lugar l) {
        StringBuilder sb = new StringBuilder();
        if (l.getCalle() != null) {
            sb.append(l.getCalle());
            if (l.getAltura() != null) {
                sb.append(' ').append(l.getAltura());
            } else if (l.getKm() != null) {
                sb.append(" km ").append(l.getKm().stripTrailingZeros().toPlainString());
            } else if (l.getCalleCruce() != null) {
                sb.append(" y ").append(l.getCalleCruce());
            }
        }
        if (l.getComplemento() != null) {
            sb.append(sb.isEmpty() ? "" : ", ").append(l.getComplemento());
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
