package ar.sf.sicat.admin.catalogo;

import static ar.sf.sicat.admin.comun.Entidades.buscar;
import static ar.sf.sicat.admin.comun.Entidades.buscarOpcional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.sf.sicat.admin.catalogo.dto.CatalogoItemDto;
import ar.sf.sicat.admin.catalogo.dto.CategoriaNodoDto;
import ar.sf.sicat.admin.catalogo.dto.CategoriaSolicitud;
import ar.sf.sicat.admin.publicacion.PublicacionService;
import ar.sf.sicat.core.comun.ReglaNegocioException;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoRepository;
import ar.sf.sicat.core.dominio.clasificacion.Caracteristica;
import ar.sf.sicat.core.dominio.clasificacion.CaracteristicaRepository;
import ar.sf.sicat.core.dominio.clasificacion.Categoria;
import ar.sf.sicat.core.dominio.clasificacion.CategoriaRepository;

@Service
public class CategoriaService {

    private final CategoriaRepository categorias;
    private final CaracteristicaRepository caracteristicas;
    private final EstablecimientoRepository establecimientos;
    private final PublicacionService publicacion;

    public CategoriaService(CategoriaRepository categorias, CaracteristicaRepository caracteristicas,
                            EstablecimientoRepository establecimientos, PublicacionService publicacion) {
        this.categorias = categorias;
        this.caracteristicas = caracteristicas;
        this.establecimientos = establecimientos;
        this.publicacion = publicacion;
    }

    /** Árbol completo (o solo el subárbol de {@code padreId}). Por defecto sin inactivas. */
    @Transactional(readOnly = true)
    public List<CategoriaNodoDto> arbol(Long padreId, boolean incluirInactivas) {
        List<Categoria> todas = categorias.findAll().stream()
                .filter(c -> incluirInactivas || Boolean.TRUE.equals(c.getActivo()))
                .toList();
        Map<Long, List<Categoria>> hijosPorPadre = todas.stream()
                .filter(c -> c.getCategoriaPadre() != null)
                .collect(Collectors.groupingBy(c -> c.getCategoriaPadre().getId()));
        List<Categoria> raices = padreId != null
                ? hijosPorPadre.getOrDefault(padreId, List.of())
                : todas.stream().filter(c -> c.getCategoriaPadre() == null).toList();
        return nodos(raices, hijosPorPadre);
    }

    private List<CategoriaNodoDto> nodos(List<Categoria> lista, Map<Long, List<Categoria>> hijosPorPadre) {
        return lista.stream()
                .sorted(Comparator.comparing(Categoria::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(c -> new CategoriaNodoDto(c.getId(), c.getNombre(),
                        c.getCategoriaPadre() == null ? null : c.getCategoriaPadre().getId(),
                        Boolean.TRUE.equals(c.getActivo()),
                        nodos(hijosPorPadre.getOrDefault(c.getId(), List.of()), hijosPorPadre)))
                .toList();
    }

    @Transactional
    public CategoriaNodoDto crear(CategoriaSolicitud s) {
        Categoria c = new Categoria();
        c.setNombre(s.nombre().trim());
        c.setCategoriaPadre(buscarOpcional(categorias, s.padreId(), "Categoría padre"));
        c.setActivo(true);
        return nodo(categorias.save(c));
    }

    @Transactional
    public CategoriaNodoDto modificar(Long id, CategoriaSolicitud s) {
        Categoria c = buscar(categorias, id, "Categoría");
        Categoria padre = buscarOpcional(categorias, s.padreId(), "Categoría padre");
        for (Categoria p = padre; p != null; p = p.getCategoriaPadre()) {
            if (p.getId().equals(id)) {
                throw new ReglaNegocioException("Una categoría no puede quedar dentro de sí misma o de una subcategoría suya");
            }
        }
        c.setNombre(s.nombre().trim());
        c.setCategoriaPadre(padre);
        if (s.activo() != null) {
            c.setActivo(s.activo());
        }
        publicacion.verificarPublicados(establecimientos.conOfertasDeCategoria(id));
        return nodo(c);
    }

    /** Baja lógica. 422 si deja a un establecimiento publicado sin ofertas con categoría activa. */
    @Transactional
    public void darDeBaja(Long id) {
        buscar(categorias, id, "Categoría").setActivo(false);
        publicacion.verificarPublicados(establecimientos.conOfertasDeCategoria(id));
    }

    @Transactional(readOnly = true)
    public List<CatalogoItemDto> caracteristicas(Long id) {
        return ordenadas(buscar(categorias, id, "Categoría").getCaracteristicas());
    }

    /** Reemplaza las características que aplican a la categoría (modifica la colección en el lugar). */
    @Transactional
    public List<CatalogoItemDto> reemplazarCaracteristicas(Long id, List<Long> ids) {
        Categoria c = buscar(categorias, id, "Categoría");
        Set<Caracteristica> nuevas = new HashSet<>();
        for (Long idCar : new LinkedHashSet<>(ids)) {
            nuevas.add(buscar(caracteristicas, idCar, "Característica"));
        }
        c.getCaracteristicas().retainAll(nuevas);
        c.getCaracteristicas().addAll(nuevas);
        return ordenadas(c.getCaracteristicas());
    }

    /** Id de la categoría y de todas sus descendientes (para filtrar por una rama del árbol). */
    @Transactional(readOnly = true)
    public Set<Long> conDescendientes(Long id) {
        Map<Long, List<Long>> hijos = new java.util.HashMap<>();
        for (Categoria c : categorias.findAll()) {
            if (c.getCategoriaPadre() != null) {
                hijos.computeIfAbsent(c.getCategoriaPadre().getId(), k -> new ArrayList<>()).add(c.getId());
            }
        }
        Set<Long> resultado = new LinkedHashSet<>();
        List<Long> pendientes = new ArrayList<>(List.of(id));
        while (!pendientes.isEmpty()) {
            Long actual = pendientes.remove(pendientes.size() - 1);
            if (resultado.add(actual)) {
                pendientes.addAll(hijos.getOrDefault(actual, List.of()));
            }
        }
        return resultado;
    }

    private static CategoriaNodoDto nodo(Categoria c) {
        return new CategoriaNodoDto(c.getId(), c.getNombre(),
                c.getCategoriaPadre() == null ? null : c.getCategoriaPadre().getId(),
                Boolean.TRUE.equals(c.getActivo()), List.of());
    }

    private static List<CatalogoItemDto> ordenadas(Collection<Caracteristica> lista) {
        return lista.stream()
                .map(CatalogoService::caracteristica)
                .sorted(Comparator.comparing(CatalogoItemDto::grupo, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(CatalogoItemDto::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
