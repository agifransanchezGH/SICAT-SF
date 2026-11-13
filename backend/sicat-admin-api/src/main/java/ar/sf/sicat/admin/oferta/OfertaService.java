package ar.sf.sicat.admin.oferta;

import static ar.sf.sicat.admin.comun.Entidades.buscar;
import static ar.sf.sicat.admin.comun.Entidades.texto;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.sf.sicat.admin.especializacion.EspecializacionService;
import ar.sf.sicat.admin.oferta.dto.OfertaDto;
import ar.sf.sicat.admin.oferta.dto.OfertaSolicitud;
import ar.sf.sicat.core.comun.ReglaNegocioException;
import ar.sf.sicat.core.dominio.clasificacion.Categoria;
import ar.sf.sicat.core.dominio.clasificacion.CategoriaRepository;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoRepository;
import ar.sf.sicat.core.dominio.oferta.Oferta;
import ar.sf.sicat.core.dominio.oferta.OfertaRepository;

@Service
public class OfertaService {

    private final OfertaRepository ofertas;
    private final EstablecimientoRepository establecimientos;
    private final CategoriaRepository categorias;
    private final HorarioService horarios;
    private final EspecializacionService especializaciones;

    public OfertaService(OfertaRepository ofertas, EstablecimientoRepository establecimientos,
                         CategoriaRepository categorias, HorarioService horarios,
                         EspecializacionService especializaciones) {
        this.ofertas = ofertas;
        this.establecimientos = establecimientos;
        this.categorias = categorias;
        this.horarios = horarios;
        this.especializaciones = especializaciones;
    }

    @Transactional(readOnly = true)
    public List<OfertaDto> deEstablecimiento(Long idEstablecimiento, boolean incluirInactivas) {
        buscar(establecimientos, idEstablecimiento, "Establecimiento");
        return ofertas.findByEstablecimientoIdOrderById(idEstablecimiento).stream()
                .filter(o -> incluirInactivas || Boolean.TRUE.equals(o.getActivo()))
                .map(o -> dto(o, false)).toList();
    }

    @Transactional(readOnly = true)
    public OfertaDto detalle(Long id) {
        return dto(buscar(ofertas, id, "Oferta"), true);
    }

    @Transactional
    public OfertaDto crear(Long idEstablecimiento, OfertaSolicitud s) {
        Oferta o = new Oferta();
        o.setEstablecimiento(buscar(establecimientos, idEstablecimiento, "Establecimiento"));
        aplicar(o, s);
        o.setActivo(true);
        return dto(ofertas.save(o), true);
    }

    @Transactional
    public OfertaDto modificar(Long id, OfertaSolicitud s) {
        Oferta o = buscar(ofertas, id, "Oferta");
        aplicar(o, s);
        if (s.activo() != null) {
            o.setActivo(s.activo());
        }
        return dto(o, true);
    }

    @Transactional
    public void darDeBaja(Long id) {
        buscar(ofertas, id, "Oferta").setActivo(false);
    }

    @Transactional(readOnly = true)
    public List<OfertaDto.CategoriaRef> categoriasAdicionales(Long id) {
        return refs(buscar(ofertas, id, "Oferta").getCategoriasAdicionales());
    }

    /** Reemplaza las categorías adicionales (modifica la colección en el lugar para que la auditoría guarde antes y después). */
    @Transactional
    public List<OfertaDto.CategoriaRef> reemplazarCategoriasAdicionales(Long id, List<Long> ids) {
        Oferta o = buscar(ofertas, id, "Oferta");
        Set<Categoria> nuevas = new HashSet<>();
        for (Long idCategoria : new LinkedHashSet<>(ids)) {
            if (idCategoria.equals(o.getCategoria().getId())) {
                throw new ReglaNegocioException("La categoría principal no puede repetirse como adicional");
            }
            nuevas.add(buscar(categorias, idCategoria, "Categoría"));
        }
        o.getCategoriasAdicionales().retainAll(nuevas);
        o.getCategoriasAdicionales().addAll(nuevas);
        return refs(o.getCategoriasAdicionales());
    }

    private void aplicar(Oferta o, OfertaSolicitud s) {
        Categoria principal = buscar(categorias, s.categoriaId(), "Categoría");
        o.setCategoria(principal);
        o.getCategoriasAdicionales().remove(principal);
        o.setAcceso(texto(s.acceso()));
        o.setPublicoAdmitido(texto(s.publicoAdmitido()));
        o.setFrecuencia(texto(s.frecuencia()));
        o.setFechaRelevamiento(s.fechaRelevamiento());
    }

    private OfertaDto dto(Oferta o, boolean detalle) {
        Long id = o.getId();
        return new OfertaDto(id, o.getEstablecimiento().getId(), o.getEstablecimiento().getNombre(),
                o.getCategoria().getId(), o.getCategoria().getNombre(), refs(o.getCategoriasAdicionales()),
                o.getAcceso(), o.getPublicoAdmitido(), o.getFrecuencia(), o.getFechaRelevamiento(),
                Boolean.TRUE.equals(o.getActivo()),
                detalle ? horarios.deOferta(id) : null,
                detalle ? especializaciones.alojamientoOpcional(id) : null,
                detalle ? especializaciones.circuitoOpcional(id) : null,
                detalle ? especializaciones.salones(id, true) : null,
                detalle ? especializaciones.visitas(id) : null);
    }

    private static List<OfertaDto.CategoriaRef> refs(Set<Categoria> lista) {
        return lista.stream()
                .map(c -> new OfertaDto.CategoriaRef(c.getId(), c.getNombre()))
                .sorted(Comparator.comparing(OfertaDto.CategoriaRef::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
