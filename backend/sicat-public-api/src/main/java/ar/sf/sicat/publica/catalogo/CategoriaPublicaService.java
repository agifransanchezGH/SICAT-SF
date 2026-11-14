package ar.sf.sicat.publica.catalogo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.sf.sicat.core.vistapublica.CategoriaPublica;
import ar.sf.sicat.core.vistapublica.CategoriaPublicaRepository;
import ar.sf.sicat.core.vistapublica.EstablecimientoPublicoRepository;
import ar.sf.sicat.publica.catalogo.dto.CategoriaNodoDto;
import ar.sf.sicat.publica.catalogo.dto.LocalidadDto;

@Service
@Transactional(readOnly = true)
public class CategoriaPublicaService {

    private final CategoriaPublicaRepository categorias;
    private final EstablecimientoPublicoRepository establecimientos;

    public CategoriaPublicaService(CategoriaPublicaRepository categorias,
                                   EstablecimientoPublicoRepository establecimientos) {
        this.categorias = categorias;
        this.establecimientos = establecimientos;
    }

    /** Árbol de categorías activas, ordenado por nombre. */
    @Cacheable("categorias")
    public List<CategoriaNodoDto> arbol() {
        Map<Long, List<CategoriaPublica>> hijos = new HashMap<>();
        List<CategoriaPublica> raices = new ArrayList<>();
        Set<Long> ids = todas().stream().map(CategoriaPublica::getId).collect(Collectors.toSet());
        for (CategoriaPublica c : todas()) {
            // una categoría cuyo padre está inactivo se muestra como raíz
            if (c.getIdCategoriaPadre() == null || !ids.contains(c.getIdCategoriaPadre())) {
                raices.add(c);
            } else {
                hijos.computeIfAbsent(c.getIdCategoriaPadre(), k -> new ArrayList<>()).add(c);
            }
        }
        return nodos(raices, hijos);
    }

    private List<CategoriaNodoDto> nodos(List<CategoriaPublica> lista, Map<Long, List<CategoriaPublica>> hijos) {
        return lista.stream()
                .sorted(Comparator.comparing(CategoriaPublica::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(c -> new CategoriaNodoDto(c.getId(), c.getNombre(),
                        nodos(hijos.getOrDefault(c.getId(), List.of()), hijos)))
                .toList();
    }

    /** La categoría y todas sus descendientes activas. */
    public Set<Long> conDescendientes(Long id) {
        Map<Long, List<Long>> hijos = new HashMap<>();
        for (CategoriaPublica c : todas()) {
            if (c.getIdCategoriaPadre() != null) {
                hijos.computeIfAbsent(c.getIdCategoriaPadre(), k -> new ArrayList<>()).add(c.getId());
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

    /** Localidades que tienen al menos un establecimiento publicado (para el filtro del sitio). */
    @Cacheable("localidades")
    public List<LocalidadDto> localidades() {
        return establecimientos.findAll(Sort.by("localidad")).stream()
                .filter(e -> e.getIdLocalidad() != null)
                .map(e -> new LocalidadDto(e.getIdLocalidad(), e.getLocalidad(), e.getProvincia()))
                .distinct()
                .toList();
    }

    private List<CategoriaPublica> todas() {
        return categorias.findAll(Sort.by("nombre"));
    }
}
