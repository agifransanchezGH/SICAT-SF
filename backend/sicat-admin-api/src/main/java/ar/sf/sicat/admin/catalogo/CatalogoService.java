package ar.sf.sicat.admin.catalogo;

import static ar.sf.sicat.admin.comun.Entidades.texto;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.sf.sicat.admin.catalogo.dto.CatalogoItemDto;
import ar.sf.sicat.admin.catalogo.dto.CatalogoItemSolicitud;
import ar.sf.sicat.core.comun.ConflictoException;
import ar.sf.sicat.core.comun.DatoInvalidoException;
import ar.sf.sicat.core.comun.EntidadNoEncontradaException;
import ar.sf.sicat.core.dominio.clasificacion.Caracteristica;
import ar.sf.sicat.core.dominio.clasificacion.CaracteristicaRepository;
import ar.sf.sicat.core.dominio.contacto.TipoCanal;
import ar.sf.sicat.core.dominio.contacto.TipoCanalRepository;
import ar.sf.sicat.core.dominio.establecimiento.Distincion;
import ar.sf.sicat.core.dominio.establecimiento.DistincionRepository;
import ar.sf.sicat.core.dominio.lugar.Localidad;
import ar.sf.sicat.core.dominio.lugar.LocalidadRepository;
import ar.sf.sicat.core.dominio.lugar.TipoLugar;
import ar.sf.sicat.core.dominio.lugar.TipoLugarRepository;
import ar.sf.sicat.core.dominio.oferente.TipoOferente;
import ar.sf.sicat.core.dominio.oferente.TipoOferenteRepository;

/**
 * ABM de los catálogos simples. Cada catálogo se describe con un {@link Adaptador}: cómo se crea,
 * cómo se aplica la solicitud, qué es un duplicado y cómo se expone.
 */
@Service
public class CatalogoService {

    private static final Comparator<CatalogoItemDto> POR_NOMBRE =
            Comparator.comparing(CatalogoItemDto::nombre, String.CASE_INSENSITIVE_ORDER);

    private final Map<TipoCatalogo, Adaptador<?>> adaptadores = new EnumMap<>(TipoCatalogo.class);

    public CatalogoService(TipoOferenteRepository tiposOferente, TipoCanalRepository tiposCanal,
                           TipoLugarRepository tiposLugar, LocalidadRepository localidades,
                           DistincionRepository distinciones, CaracteristicaRepository caracteristicas) {

        adaptadores.put(TipoCatalogo.TIPO_OFERENTE, new Adaptador<>("Tipo de oferente", tiposOferente,
                TipoOferente::new, TipoOferente::getId,
                (e, s) -> e.setNombre(s.nombre().trim()),
                s -> tiposOferente.findByNombreIgnoreCase(s.nombre().trim()).map(TipoOferente::getId),
                e -> CatalogoItemDto.simple(e.getId(), e.getNombre(), e.getActivo()),
                TipoOferente::setActivo, s -> { }, POR_NOMBRE));

        adaptadores.put(TipoCatalogo.TIPO_CANAL, new Adaptador<>("Tipo de canal", tiposCanal,
                TipoCanal::new, TipoCanal::getId,
                (e, s) -> e.setNombre(s.nombre().trim()),
                s -> tiposCanal.findByNombreIgnoreCase(s.nombre().trim()).map(TipoCanal::getId),
                e -> CatalogoItemDto.simple(e.getId(), e.getNombre(), e.getActivo()),
                TipoCanal::setActivo, s -> { }, POR_NOMBRE));

        adaptadores.put(TipoCatalogo.TIPO_LUGAR, new Adaptador<>("Tipo de lugar", tiposLugar,
                TipoLugar::new, TipoLugar::getId,
                (e, s) -> e.setNombre(s.nombre().trim()),
                s -> tiposLugar.findByNombreIgnoreCase(s.nombre().trim()).map(TipoLugar::getId),
                e -> CatalogoItemDto.simple(e.getId(), e.getNombre(), e.getActivo()),
                TipoLugar::setActivo, s -> { }, POR_NOMBRE));

        adaptadores.put(TipoCatalogo.LOCALIDAD, new Adaptador<>("Localidad", localidades,
                Localidad::new, Localidad::getId,
                (e, s) -> {
                    e.setNombre(s.nombre().trim());
                    e.setProvincia(s.provincia().trim());
                },
                s -> localidades.findByNombreIgnoreCaseAndProvinciaIgnoreCase(s.nombre().trim(), s.provincia().trim())
                        .map(Localidad::getId),
                e -> new CatalogoItemDto(e.getId(), e.getNombre(), e.getActivo(), e.getProvincia(), null, null, null),
                Localidad::setActivo,
                s -> requerido(s.provincia(), "provincia", "localidad"),
                POR_NOMBRE));

        adaptadores.put(TipoCatalogo.DISTINCION, new Adaptador<>("Distinción", distinciones,
                Distincion::new, Distincion::getId,
                (e, s) -> {
                    e.setNombre(s.nombre().trim());
                    e.setOrganismoOtorgante(texto(s.organismoOtorgante()));
                },
                s -> distinciones.findByNombreIgnoreCase(s.nombre().trim()).map(Distincion::getId),
                e -> new CatalogoItemDto(e.getId(), e.getNombre(), e.getActivo(), null, e.getOrganismoOtorgante(),
                        null, null),
                Distincion::setActivo, s -> { }, POR_NOMBRE));

        adaptadores.put(TipoCatalogo.CARACTERISTICA, new Adaptador<>("Característica", caracteristicas,
                Caracteristica::new, Caracteristica::getId,
                (e, s) -> {
                    e.setNombre(s.nombre().trim());
                    e.setGrupo(s.grupo().trim());
                    if (s.admiteCantidad() != null) {
                        e.setAdmiteCantidad(s.admiteCantidad());
                    }
                },
                s -> caracteristicas.findByGrupoIgnoreCaseAndNombreIgnoreCase(s.grupo().trim(), s.nombre().trim())
                        .map(Caracteristica::getId),
                CatalogoService::caracteristica,
                Caracteristica::setActivo,
                s -> requerido(s.grupo(), "grupo", "caracteristica"),
                Comparator.comparing(CatalogoItemDto::grupo, String.CASE_INSENSITIVE_ORDER).thenComparing(POR_NOMBRE)));
    }

    public static CatalogoItemDto caracteristica(Caracteristica e) {
        return new CatalogoItemDto(e.getId(), e.getNombre(), e.getActivo(), null, null, e.getGrupo(),
                e.getAdmiteCantidad());
    }

    @Transactional(readOnly = true)
    public List<CatalogoItemDto> listar(TipoCatalogo tipo, boolean incluirInactivos) {
        return adaptador(tipo).listar(incluirInactivos);
    }

    @Transactional
    public CatalogoItemDto crear(TipoCatalogo tipo, CatalogoItemSolicitud s) {
        return adaptador(tipo).crear(s);
    }

    @Transactional
    public CatalogoItemDto modificar(TipoCatalogo tipo, Long id, CatalogoItemSolicitud s) {
        return adaptador(tipo).modificar(id, s);
    }

    @Transactional
    public void darDeBaja(TipoCatalogo tipo, Long id) {
        adaptador(tipo).darDeBaja(id);
    }

    private Adaptador<?> adaptador(TipoCatalogo tipo) {
        return adaptadores.get(tipo);
    }

    private static void requerido(String valor, String campo, String catalogo) {
        if (valor == null || valor.isBlank()) {
            throw new DatoInvalidoException("'" + campo + "' es obligatorio en " + catalogo);
        }
    }

    /** Cómo se maneja un catálogo concreto. */
    private record Adaptador<E>(
            String nombre,
            JpaRepository<E, Long> repo,
            Supplier<E> nuevo,
            Function<E, Long> id,
            BiConsumer<E, CatalogoItemSolicitud> aplicar,
            Function<CatalogoItemSolicitud, Optional<Long>> duplicado,
            Function<E, CatalogoItemDto> dto,
            BiConsumer<E, Boolean> activo,
            Consumer<CatalogoItemSolicitud> validar,
            Comparator<CatalogoItemDto> orden) {

        List<CatalogoItemDto> listar(boolean incluirInactivos) {
            return repo.findAll().stream()
                    .map(dto)
                    .filter(d -> incluirInactivos || Boolean.TRUE.equals(d.activo()))
                    .sorted(orden)
                    .toList();
        }

        CatalogoItemDto crear(CatalogoItemSolicitud s) {
            validar.accept(s);
            verificarDuplicado(s, null);
            E e = nuevo.get();
            aplicar.accept(e, s);
            activo.accept(e, true);
            return dto.apply(repo.save(e));
        }

        CatalogoItemDto modificar(Long idItem, CatalogoItemSolicitud s) {
            validar.accept(s);
            E e = buscar(idItem);
            verificarDuplicado(s, idItem);
            aplicar.accept(e, s);
            if (s.activo() != null) {
                activo.accept(e, s.activo());
            }
            return dto.apply(e);
        }

        void darDeBaja(Long idItem) {
            activo.accept(buscar(idItem), false);
        }

        private E buscar(Long idItem) {
            return repo.findById(idItem).orElseThrow(() -> new EntidadNoEncontradaException(nombre, idItem));
        }

        private void verificarDuplicado(CatalogoItemSolicitud s, Long idPropio) {
            duplicado.apply(s)
                    .filter(otro -> !otro.equals(idPropio))
                    .ifPresent(otro -> {
                        throw new ConflictoException(nombre + " ya existe (id " + otro + ")");
                    });
        }
    }
}
