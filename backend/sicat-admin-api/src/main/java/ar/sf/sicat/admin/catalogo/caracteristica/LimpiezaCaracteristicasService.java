package ar.sf.sicat.admin.catalogo.caracteristica;

import static ar.sf.sicat.admin.comun.Entidades.buscar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.sf.sicat.admin.catalogo.caracteristica.dto.CaracteristicaUsoDto;
import ar.sf.sicat.admin.catalogo.caracteristica.dto.FusionSolicitud;
import ar.sf.sicat.admin.catalogo.caracteristica.dto.LimpiezaResultadoDto;
import ar.sf.sicat.admin.catalogo.caracteristica.dto.SeparacionSolicitud;
import ar.sf.sicat.admin.importacion.lectura.Texto;
import ar.sf.sicat.core.comun.ConflictoException;
import ar.sf.sicat.core.comun.DatoInvalidoException;
import ar.sf.sicat.core.comun.ReglaNegocioException;
import ar.sf.sicat.core.dominio.clasificacion.Caracteristica;
import ar.sf.sicat.core.dominio.clasificacion.CaracteristicaRepository;
import ar.sf.sicat.core.dominio.clasificacion.Categoria;
import ar.sf.sicat.core.dominio.clasificacion.CategoriaRepository;
import ar.sf.sicat.core.dominio.especializacion.salon.SalonCaracteristica;
import ar.sf.sicat.core.dominio.especializacion.salon.SalonCaracteristicaRepository;
import ar.sf.sicat.core.dominio.establecimiento.Establecimiento;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoCaracteristica;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoCaracteristicaRepository;

/**
 * Limpieza del catálogo de características (ADR 0017): fusionar duplicados, separar entradas pegadas y pasar a
 * notas internas lo que no es una característica (p. ej. una dirección). Todo por JPA, así la auditoría registra
 * cada asignación movida y cada característica borrada.
 */
@Service
public class LimpiezaCaracteristicasService {

    private final CaracteristicaRepository caracteristicas;
    private final EstablecimientoCaracteristicaRepository deEstablecimientos;
    private final SalonCaracteristicaRepository deSalones;
    private final CategoriaRepository categorias;

    public LimpiezaCaracteristicasService(CaracteristicaRepository caracteristicas,
                                          EstablecimientoCaracteristicaRepository deEstablecimientos,
                                          SalonCaracteristicaRepository deSalones, CategoriaRepository categorias) {
        this.caracteristicas = caracteristicas;
        this.deEstablecimientos = deEstablecimientos;
        this.deSalones = deSalones;
        this.categorias = categorias;
    }

    /** Todas (activas e inactivas) o las de un grupo, con sus usos. Ordenadas por grupo y nombre. */
    @Transactional(readOnly = true)
    public List<CaracteristicaUsoDto> uso(String grupo) {
        Map<Long, Long> est = contar(deEstablecimientos.contarPorCaracteristica());
        Map<Long, Long> sal = contar(deSalones.contarPorCaracteristica());
        Map<Long, Long> cat = contar(categorias.contarPorCaracteristica());
        String g = grupo == null || grupo.isBlank() ? null : Texto.clave(grupo);
        return caracteristicas.findAll().stream()
                .filter(c -> g == null || Texto.clave(c.getGrupo()).equals(g))
                .sorted(Comparator.comparing((Caracteristica c) -> Objects.toString(c.getGrupo(), ""), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Caracteristica::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(c -> dto(c, est.getOrDefault(c.getId(), 0L), sal.getOrDefault(c.getId(), 0L), cat.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    /**
     * Pasa todos los usos de las absorbidas a la conservada y las borra. Si un establecimiento o salón ya tenía
     * la conservada, no se duplica: queda una sola asignación, con la cantidad de la conservada o, si no tenía,
     * la de la absorbida.
     */
    @Transactional
    public LimpiezaResultadoDto fusionar(FusionSolicitud s) {
        Set<Long> absorber = new HashSet<>(s.absorberIds());
        if (absorber.size() != s.absorberIds().size()) {
            throw new DatoInvalidoException("Hay características repetidas");
        }
        if (absorber.contains(s.conservarId())) {
            throw new DatoInvalidoException("La característica que queda no puede estar entre las que se absorben");
        }
        Caracteristica destino = buscar(caracteristicas, s.conservarId(), "Característica");
        List<Caracteristica> origenes = s.absorberIds().stream().map(id -> buscar(caracteristicas, id, "Característica")).toList();
        for (Caracteristica o : origenes) {
            if (!Texto.clave(o.getGrupo()).equals(Texto.clave(destino.getGrupo()))) {
                throw new ReglaNegocioException("Solo se fusionan características del mismo grupo: '" + o.getNombre()
                        + "' es de '" + o.getGrupo() + "' y '" + destino.getNombre() + "' de '" + destino.getGrupo() + "'");
            }
        }
        if (s.nombre() != null && !s.nombre().isBlank()) {
            renombrar(destino, s.nombre().trim());
        }
        int[] cuentas = new int[3];
        for (Caracteristica o : origenes) {
            sumar(cuentas, mover(o, List.of(destino)));
            if (Boolean.TRUE.equals(o.getAdmiteCantidad())) {
                destino.setAdmiteCantidad(true);
            }
            caracteristicas.delete(o);
        }
        return resultado(List.of(destino), cuentas, origenes.size());
    }

    /** Reemplaza una característica por varias del mismo grupo, con los mismos usos que tenía la original. */
    @Transactional
    public LimpiezaResultadoDto separar(Long id, SeparacionSolicitud s) {
        Caracteristica original = buscar(caracteristicas, id, "Característica");
        Map<String, String> partes = new LinkedHashMap<>();
        for (String n : s.nombres()) {
            partes.putIfAbsent(Texto.clave(n), n.trim());
        }
        if (partes.size() < 2) {
            throw new DatoInvalidoException("Hacen falta al menos dos partes distintas");
        }
        if (partes.containsKey(Texto.clave(original.getNombre()))) {
            throw new DatoInvalidoException("Una parte no puede llamarse igual que la original");
        }
        String grupo = Texto.clave(original.getGrupo());
        List<Caracteristica> delGrupo = caracteristicas.findAll().stream()
                .filter(c -> Texto.clave(c.getGrupo()).equals(grupo)).toList();
        List<Caracteristica> destinos = new ArrayList<>();
        for (String nombre : partes.values()) {
            destinos.add(delGrupo.stream()
                    .filter(c -> !c.getId().equals(original.getId()) && Texto.clave(c.getNombre()).equals(Texto.clave(nombre)))
                    .findFirst()
                    .orElseGet(() -> {
                        Caracteristica c = new Caracteristica();
                        c.setGrupo(original.getGrupo());
                        c.setNombre(nombre);
                        c.setAdmiteCantidad(false);
                        c.setEsPublica(original.getEsPublica());
                        c.setActivo(true);
                        return caracteristicas.save(c);
                    }));
        }
        int[] cuentas = mover(original, destinos);
        caracteristicas.delete(original);
        return resultado(destinos, cuentas, 1);
    }

    /**
     * Para lo que no es una característica (una dirección, una aclaración): se agrega a las notas internas de cada
     * establecimiento que la tenía y se borra. Mismo criterio que la importación (ADR 0015).
     */
    @Transactional
    public LimpiezaResultadoDto aNotasInternas(Long id) {
        Caracteristica c = buscar(caracteristicas, id, "Característica");
        if (!deSalones.findByCaracteristicaId(id).isEmpty() || !categorias.findByCaracteristicasId(id).isEmpty()) {
            throw new ConflictoException("La usan salones o categorías, que no tienen notas internas: fusionala o quitala de ahí primero");
        }
        String nota = (c.getGrupo() == null ? "" : c.getGrupo() + ": ") + c.getNombre();
        List<EstablecimientoCaracteristica> usos = deEstablecimientos.findByCaracteristicaId(id);
        for (EstablecimientoCaracteristica uso : usos) {
            Establecimiento e = uso.getEstablecimiento();
            String previas = e.getNotasInternas();
            if (previas == null || !previas.contains(nota)) {
                e.setNotasInternas(previas == null || previas.isBlank() ? nota : previas + "\n\n" + nota);
            }
            deEstablecimientos.delete(uso);
        }
        caracteristicas.delete(c);
        return new LimpiezaResultadoDto(List.of(), usos.size(), 0, 0, 1);
    }

    // ------------------------------------------------------------------ interno

    /** Copia los usos de {@code origen} a cada destino (sin duplicar) y borra las asignaciones del origen. */
    private int[] mover(Caracteristica origen, List<Caracteristica> destinos) {
        int[] cuentas = new int[3];
        for (EstablecimientoCaracteristica uso : deEstablecimientos.findByCaracteristicaId(origen.getId())) {
            Map<Long, EstablecimientoCaracteristica> actuales = porCaracteristica(
                    deEstablecimientos.findByEstablecimientoId(uso.getEstablecimiento().getId()), x -> x.getCaracteristica().getId());
            for (Caracteristica d : destinos) {
                EstablecimientoCaracteristica ya = actuales.get(d.getId());
                if (ya == null) {
                    EstablecimientoCaracteristica nueva = new EstablecimientoCaracteristica();
                    nueva.setEstablecimiento(uso.getEstablecimiento());
                    nueva.setCaracteristica(d);
                    nueva.setCantidad(destinos.size() == 1 ? uso.getCantidad() : null);
                    deEstablecimientos.save(nueva);
                    cuentas[0]++;
                } else if (ya.getCantidad() == null && uso.getCantidad() != null && destinos.size() == 1) {
                    ya.setCantidad(uso.getCantidad());
                }
            }
            deEstablecimientos.delete(uso);
        }
        for (SalonCaracteristica uso : deSalones.findByCaracteristicaId(origen.getId())) {
            Map<Long, SalonCaracteristica> actuales = porCaracteristica(
                    deSalones.findBySalonId(uso.getSalon().getId()), x -> x.getCaracteristica().getId());
            for (Caracteristica d : destinos) {
                SalonCaracteristica ya = actuales.get(d.getId());
                if (ya == null) {
                    SalonCaracteristica nueva = new SalonCaracteristica();
                    nueva.setSalon(uso.getSalon());
                    nueva.setCaracteristica(d);
                    nueva.setCantidad(destinos.size() == 1 ? uso.getCantidad() : null);
                    deSalones.save(nueva);
                    cuentas[1]++;
                } else if (ya.getCantidad() == null && uso.getCantidad() != null && destinos.size() == 1) {
                    ya.setCantidad(uso.getCantidad());
                }
            }
            deSalones.delete(uso);
        }
        for (Categoria cat : categorias.findByCaracteristicasId(origen.getId())) {
            cat.getCaracteristicas().remove(origen);
            cat.getCaracteristicas().addAll(destinos);
            cuentas[2]++;
        }
        return cuentas;
    }

    private void renombrar(Caracteristica c, String nombre) {
        boolean ocupado = caracteristicas.findAll().stream()
                .anyMatch(o -> !o.getId().equals(c.getId()) && Texto.clave(o.getGrupo()).equals(Texto.clave(c.getGrupo()))
                        && Texto.clave(o.getNombre()).equals(Texto.clave(nombre)));
        if (ocupado) {
            throw new ConflictoException("Ya hay otra característica '" + nombre + "' en el grupo: incluila en la fusión");
        }
        c.setNombre(nombre);
    }

    private LimpiezaResultadoDto resultado(List<Caracteristica> resultantes, int[] cuentas, int eliminadas) {
        caracteristicas.flush();
        Map<Long, Long> est = contar(deEstablecimientos.contarPorCaracteristica());
        Map<Long, Long> sal = contar(deSalones.contarPorCaracteristica());
        Map<Long, Long> cat = contar(categorias.contarPorCaracteristica());
        return new LimpiezaResultadoDto(
                resultantes.stream().map(c -> dto(c, est.getOrDefault(c.getId(), 0L), sal.getOrDefault(c.getId(), 0L),
                        cat.getOrDefault(c.getId(), 0L))).toList(),
                cuentas[0], cuentas[1], cuentas[2], eliminadas);
    }

    private static void sumar(int[] total, int[] parcial) {
        for (int i = 0; i < total.length; i++) {
            total[i] += parcial[i];
        }
    }

    private static <T> Map<Long, T> porCaracteristica(List<T> lista, Function<T, Long> id) {
        Map<Long, T> m = new HashMap<>();
        lista.forEach(x -> m.put(id.apply(x), x));
        return m;
    }

    private static Map<Long, Long> contar(List<Object[]> filas) {
        Map<Long, Long> m = new HashMap<>();
        for (Object[] f : filas) {
            m.put(((Number) f[0]).longValue(), ((Number) f[1]).longValue());
        }
        return m;
    }

    private static CaracteristicaUsoDto dto(Caracteristica c, long est, long sal, long cat) {
        return new CaracteristicaUsoDto(c.getId(), c.getGrupo(), c.getNombre(), Boolean.TRUE.equals(c.getAdmiteCantidad()),
                !Boolean.FALSE.equals(c.getEsPublica()), Boolean.TRUE.equals(c.getActivo()), est, sal, cat);
    }
}
