package ar.sf.sicat.admin.establecimiento;

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

import ar.sf.sicat.admin.comun.CaracteristicaAsignadaDto;
import ar.sf.sicat.admin.comun.CaracteristicaAsignadaSolicitud;
import ar.sf.sicat.admin.comun.Caracteristicas;
import ar.sf.sicat.admin.establecimiento.dto.DistincionAsignadaDto;
import ar.sf.sicat.admin.establecimiento.dto.DistincionAsignadaSolicitud;
import ar.sf.sicat.admin.establecimiento.dto.ReferenteDto;
import ar.sf.sicat.admin.establecimiento.dto.ReferenteSolicitud;
import ar.sf.sicat.core.comun.ReglaNegocioException;
import ar.sf.sicat.core.dominio.clasificacion.Caracteristica;
import ar.sf.sicat.core.dominio.clasificacion.CaracteristicaRepository;
import ar.sf.sicat.core.dominio.establecimiento.DistincionRepository;
import ar.sf.sicat.core.dominio.establecimiento.Establecimiento;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoCaracteristica;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoCaracteristicaRepository;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoDistincion;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoDistincionRepository;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoRepository;
import ar.sf.sicat.core.dominio.establecimiento.Referente;
import ar.sf.sicat.core.dominio.establecimiento.ReferenteRepository;

/**
 * Referentes, distinciones y características de un establecimiento. Los PUT reemplazan la lista
 * completa: se modifica lo que existe, se crea lo nuevo y se elimina lo que no viene.
 */
@Service
public class AnexosEstablecimientoService {

    private final EstablecimientoRepository establecimientos;
    private final ReferenteRepository referentes;
    private final DistincionRepository distinciones;
    private final EstablecimientoDistincionRepository asignacionesDistincion;
    private final CaracteristicaRepository caracteristicas;
    private final EstablecimientoCaracteristicaRepository asignacionesCaracteristica;

    public AnexosEstablecimientoService(EstablecimientoRepository establecimientos, ReferenteRepository referentes,
                                        DistincionRepository distinciones,
                                        EstablecimientoDistincionRepository asignacionesDistincion,
                                        CaracteristicaRepository caracteristicas,
                                        EstablecimientoCaracteristicaRepository asignacionesCaracteristica) {
        this.establecimientos = establecimientos;
        this.referentes = referentes;
        this.distinciones = distinciones;
        this.asignacionesDistincion = asignacionesDistincion;
        this.caracteristicas = caracteristicas;
        this.asignacionesCaracteristica = asignacionesCaracteristica;
    }

    // ------------------------------------------------------------------ referentes

    @Transactional(readOnly = true)
    public List<ReferenteDto> referentes(Long id) {
        buscar(establecimientos, id, "Establecimiento");
        return referentes.findByEstablecimientoIdOrderById(id).stream()
                .map(r -> new ReferenteDto(r.getId(), r.getNombre(), r.getCargo(), r.getTelefono(), r.getCorreo(),
                        Boolean.TRUE.equals(r.getActivo())))
                .toList();
    }

    @Transactional
    public List<ReferenteDto> reemplazarReferentes(Long id, List<ReferenteSolicitud> lista) {
        Establecimiento e = buscar(establecimientos, id, "Establecimiento");
        Map<Long, Referente> porId = referentes.findByEstablecimientoIdOrderById(id).stream()
                .collect(Collectors.toMap(Referente::getId, Function.identity()));
        Set<Long> conservados = new HashSet<>();
        for (ReferenteSolicitud s : lista) {
            Referente r;
            if (s.id() != null) {
                r = porId.get(s.id());
                if (r == null || !conservados.add(s.id())) {
                    throw new ReglaNegocioException("El referente " + s.id() + " no pertenece a este establecimiento o está repetido");
                }
            } else {
                r = new Referente();
                r.setEstablecimiento(e);
            }
            r.setNombre(s.nombre().trim());
            r.setCargo(texto(s.cargo()));
            r.setTelefono(texto(s.telefono()));
            r.setCorreo(texto(s.correo()));
            r.setActivo(s.activo() == null || s.activo());
            if (r.getId() == null) {
                referentes.save(r);
            }
        }
        porId.values().stream().filter(r -> !conservados.contains(r.getId())).forEach(referentes::delete);
        referentes.flush();
        return referentes(id);
    }

    // ------------------------------------------------------------------ distinciones

    @Transactional(readOnly = true)
    public List<DistincionAsignadaDto> distinciones(Long id) {
        buscar(establecimientos, id, "Establecimiento");
        return asignacionesDistincion.findByEstablecimientoId(id).stream()
                .map(a -> new DistincionAsignadaDto(a.getDistincion().getId(), a.getDistincion().getNombre(),
                        a.getDistincion().getOrganismoOtorgante(), a.getAnioOtorgamiento(), a.getUrlVerificacion()))
                .sorted(Comparator.comparing(DistincionAsignadaDto::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public List<DistincionAsignadaDto> reemplazarDistinciones(Long id, List<DistincionAsignadaSolicitud> lista) {
        Establecimiento e = buscar(establecimientos, id, "Establecimiento");
        Map<Long, EstablecimientoDistincion> actuales = asignacionesDistincion.findByEstablecimientoId(id).stream()
                .collect(Collectors.toMap(a -> a.getDistincion().getId(), Function.identity()));
        Set<Long> pedidas = new HashSet<>();
        for (DistincionAsignadaSolicitud s : lista) {
            if (!pedidas.add(s.distincionId())) {
                throw new ReglaNegocioException("La distinción " + s.distincionId() + " está repetida");
            }
            EstablecimientoDistincion a = actuales.get(s.distincionId());
            if (a == null) {
                a = new EstablecimientoDistincion();
                a.setEstablecimiento(e);
                a.setDistincion(buscar(distinciones, s.distincionId(), "Distinción"));
            }
            a.setAnioOtorgamiento(s.anioOtorgamiento());
            a.setUrlVerificacion(texto(s.urlVerificacion()));
            if (!actuales.containsKey(s.distincionId())) {
                asignacionesDistincion.save(a);
            }
        }
        actuales.forEach((idDistincion, a) -> {
            if (!pedidas.contains(idDistincion)) {
                asignacionesDistincion.delete(a);
            }
        });
        asignacionesDistincion.flush();
        return distinciones(id);
    }

    // ------------------------------------------------------------------ características

    @Transactional(readOnly = true)
    public List<CaracteristicaAsignadaDto> caracteristicas(Long id) {
        buscar(establecimientos, id, "Establecimiento");
        return asignacionesCaracteristica.findByEstablecimientoId(id).stream()
                .map(a -> Caracteristicas.dto(a.getCaracteristica(), a.getCantidad()))
                .sorted(Comparator.comparing(CaracteristicaAsignadaDto::grupo, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(CaracteristicaAsignadaDto::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public List<CaracteristicaAsignadaDto> reemplazarCaracteristicas(Long id,
                                                                     List<CaracteristicaAsignadaSolicitud> lista) {
        Establecimiento e = buscar(establecimientos, id, "Establecimiento");
        Caracteristicas.validarSinRepetidos(lista);
        Map<Long, EstablecimientoCaracteristica> actuales = asignacionesCaracteristica.findByEstablecimientoId(id)
                .stream().collect(Collectors.toMap(a -> a.getCaracteristica().getId(), Function.identity()));
        Set<Long> pedidas = new HashSet<>();
        for (CaracteristicaAsignadaSolicitud s : lista) {
            pedidas.add(s.caracteristicaId());
            EstablecimientoCaracteristica a = actuales.get(s.caracteristicaId());
            Caracteristica c = a != null ? a.getCaracteristica()
                    : buscar(caracteristicas, s.caracteristicaId(), "Característica");
            Caracteristicas.validarCantidad(c, s.cantidad());
            if (a == null) {
                a = new EstablecimientoCaracteristica();
                a.setEstablecimiento(e);
                a.setCaracteristica(c);
                a.setCantidad(s.cantidad());
                asignacionesCaracteristica.save(a);
            } else {
                a.setCantidad(s.cantidad());
            }
        }
        actuales.forEach((idCaracteristica, a) -> {
            if (!pedidas.contains(idCaracteristica)) {
                asignacionesCaracteristica.delete(a);
            }
        });
        asignacionesCaracteristica.flush();
        return caracteristicas(id);
    }
}
