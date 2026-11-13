package ar.sf.sicat.admin.especializacion;

import static ar.sf.sicat.admin.comun.Entidades.buscar;
import static ar.sf.sicat.admin.comun.Entidades.buscarOpcional;
import static ar.sf.sicat.admin.comun.Entidades.texto;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.sf.sicat.admin.comun.CaracteristicaAsignadaDto;
import ar.sf.sicat.admin.comun.CaracteristicaAsignadaSolicitud;
import ar.sf.sicat.admin.comun.Caracteristicas;
import ar.sf.sicat.admin.especializacion.dto.AlojamientoDto;
import ar.sf.sicat.admin.especializacion.dto.CircuitoDto;
import ar.sf.sicat.admin.especializacion.dto.CircuitoSolicitud;
import ar.sf.sicat.admin.especializacion.dto.ParadaDto;
import ar.sf.sicat.admin.especializacion.dto.ParadaSolicitud;
import ar.sf.sicat.admin.especializacion.dto.SalonDto;
import ar.sf.sicat.admin.especializacion.dto.SalonSolicitud;
import ar.sf.sicat.admin.especializacion.dto.VisitaGuiadaDto;
import ar.sf.sicat.admin.especializacion.dto.VisitaGuiadaSolicitud;
import ar.sf.sicat.admin.oferta.HorarioService;
import ar.sf.sicat.core.comun.EntidadNoEncontradaException;
import ar.sf.sicat.core.comun.GeoJson;
import ar.sf.sicat.core.dominio.clasificacion.Caracteristica;
import ar.sf.sicat.core.dominio.clasificacion.CaracteristicaRepository;
import ar.sf.sicat.core.dominio.especializacion.alojamiento.Alojamiento;
import ar.sf.sicat.core.dominio.especializacion.alojamiento.AlojamientoRepository;
import ar.sf.sicat.core.dominio.especializacion.circuito.Circuito;
import ar.sf.sicat.core.dominio.especializacion.circuito.CircuitoParada;
import ar.sf.sicat.core.dominio.especializacion.circuito.CircuitoParadaRepository;
import ar.sf.sicat.core.dominio.especializacion.circuito.CircuitoRepository;
import ar.sf.sicat.core.dominio.especializacion.salon.Salon;
import ar.sf.sicat.core.dominio.especializacion.salon.SalonCaracteristica;
import ar.sf.sicat.core.dominio.especializacion.salon.SalonCaracteristicaRepository;
import ar.sf.sicat.core.dominio.especializacion.salon.SalonRepository;
import ar.sf.sicat.core.dominio.especializacion.visitaguiada.VisitaGuiada;
import ar.sf.sicat.core.dominio.especializacion.visitaguiada.VisitaGuiadaRepository;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoRepository;
import ar.sf.sicat.core.dominio.lugar.LugarRepository;
import ar.sf.sicat.core.dominio.oferta.Oferta;
import ar.sf.sicat.core.dominio.oferta.OfertaRepository;

/** Datos propios de cada tipo de oferta: alojamiento, circuito (y paradas), salones y visitas guiadas. */
@Service
public class EspecializacionService {

    private final OfertaRepository ofertas;
    private final AlojamientoRepository alojamientos;
    private final CircuitoRepository circuitos;
    private final CircuitoParadaRepository paradas;
    private final SalonRepository salones;
    private final SalonCaracteristicaRepository caracteristicasSalon;
    private final CaracteristicaRepository caracteristicas;
    private final VisitaGuiadaRepository visitas;
    private final LugarRepository lugares;
    private final EstablecimientoRepository establecimientos;
    private final HorarioService horarios;

    public EspecializacionService(OfertaRepository ofertas, AlojamientoRepository alojamientos,
                                  CircuitoRepository circuitos, CircuitoParadaRepository paradas,
                                  SalonRepository salones, SalonCaracteristicaRepository caracteristicasSalon,
                                  CaracteristicaRepository caracteristicas, VisitaGuiadaRepository visitas,
                                  LugarRepository lugares, EstablecimientoRepository establecimientos,
                                  HorarioService horarios) {
        this.ofertas = ofertas;
        this.alojamientos = alojamientos;
        this.circuitos = circuitos;
        this.paradas = paradas;
        this.salones = salones;
        this.caracteristicasSalon = caracteristicasSalon;
        this.caracteristicas = caracteristicas;
        this.visitas = visitas;
        this.lugares = lugares;
        this.establecimientos = establecimientos;
        this.horarios = horarios;
    }

    // ------------------------------------------------------------------ alojamiento (1 a 1)

    @Transactional(readOnly = true)
    public AlojamientoDto alojamiento(Long idOferta) {
        buscar(ofertas, idOferta, "Oferta");
        return alojamientos.findById(idOferta).map(EspecializacionService::dto)
                .orElseThrow(() -> new EntidadNoEncontradaException("Alojamiento de la oferta", idOferta));
    }

    /** Lo crea si no existe o lo reemplaza. */
    @Transactional
    public AlojamientoDto guardarAlojamiento(Long idOferta, AlojamientoDto s) {
        Oferta oferta = buscar(ofertas, idOferta, "Oferta");
        Alojamiento a = alojamientos.findById(idOferta).orElse(null);
        boolean nuevo = a == null;
        if (nuevo) {
            a = new Alojamiento();
            a.setOferta(oferta);
        }
        a.setEstrellas(s.estrellas());
        a.setCantHabitaciones(s.cantHabitaciones());
        a.setTotalPlazas(s.totalPlazas());
        a.setHabAccesibles(s.habAccesibles());
        a.setPlazasAccesibles(s.plazasAccesibles());
        a.setPersonalPermanente(s.personalPermanente());
        a.setPersonalNoPermanente(s.personalNoPermanente());
        if (nuevo) {
            alojamientos.save(a);
        }
        return dto(a);
    }

    // ------------------------------------------------------------------ circuito (1 a 1) y paradas

    @Transactional(readOnly = true)
    public CircuitoDto circuito(Long idOferta) {
        buscar(ofertas, idOferta, "Oferta");
        return circuitos.findById(idOferta).map(EspecializacionService::dto)
                .orElseThrow(() -> new EntidadNoEncontradaException("Circuito de la oferta", idOferta));
    }

    @Transactional
    public CircuitoDto guardarCircuito(Long idOferta, CircuitoSolicitud s) {
        Oferta oferta = buscar(ofertas, idOferta, "Oferta");
        Circuito c = circuitos.findById(idOferta).orElse(null);
        boolean nuevo = c == null;
        if (nuevo) {
            c = new Circuito();
            c.setOferta(oferta);
        }
        c.setLugarPartida(buscarOpcional(lugares, s.lugarPartidaId(), "Lugar de partida"));
        c.setDuracionMin(s.duracionMin());
        c.setTrazado(GeoJson.aGeometria(s.trazado(), LineString.class));
        if (nuevo) {
            circuitos.save(c);
        }
        return dto(c);
    }

    @Transactional(readOnly = true)
    public List<ParadaDto> paradas(Long idOferta) {
        circuitoExistente(idOferta);
        return paradas.findByCircuitoIdOrderByIdOrden(idOferta).stream()
                .map(p -> new ParadaDto(p.getId().getOrden(), p.getEstablecimiento().getId(),
                        p.getEstablecimiento().getNombre()))
                .toList();
    }

    /** Reemplaza las paradas: la posición en la lista es el orden (1, 2, 3...). */
    @Transactional
    public List<ParadaDto> reemplazarParadas(Long idOferta, List<ParadaSolicitud> lista) {
        Circuito circuito = circuitoExistente(idOferta);
        Map<Short, CircuitoParada> actuales = paradas.findByCircuitoIdOrderByIdOrden(idOferta).stream()
                .collect(Collectors.toMap(p -> p.getId().getOrden(), Function.identity()));
        short orden = 0;
        for (ParadaSolicitud s : lista) {
            orden++;
            CircuitoParada p = actuales.remove(orden);
            if (p == null) {
                p = new CircuitoParada();
                p.setId(new CircuitoParada.Id(idOferta, orden));
                p.setCircuito(circuito);
                p.setEstablecimiento(buscar(establecimientos, s.establecimientoId(), "Establecimiento"));
                paradas.save(p);
            } else if (!p.getEstablecimiento().getId().equals(s.establecimientoId())) {
                p.setEstablecimiento(buscar(establecimientos, s.establecimientoId(), "Establecimiento"));
            }
        }
        actuales.values().forEach(paradas::delete);
        paradas.flush();
        return paradas(idOferta);
    }

    private Circuito circuitoExistente(Long idOferta) {
        buscar(ofertas, idOferta, "Oferta");
        return circuitos.findById(idOferta)
                .orElseThrow(() -> new EntidadNoEncontradaException("Circuito de la oferta", idOferta));
    }

    // ------------------------------------------------------------------ salones (N por oferta)

    @Transactional(readOnly = true)
    public List<SalonDto> salones(Long idOferta, boolean incluirInactivos) {
        buscar(ofertas, idOferta, "Oferta");
        return salones.findByOfertaIdOrderByNombre(idOferta).stream()
                .filter(s -> incluirInactivos || Boolean.TRUE.equals(s.getActivo()))
                .map(EspecializacionService::dto).toList();
    }

    @Transactional
    public SalonDto crearSalon(Long idOferta, SalonSolicitud s) {
        Salon salon = new Salon();
        salon.setOferta(buscar(ofertas, idOferta, "Oferta"));
        aplicar(salon, s);
        salon.setActivo(true);
        return dto(salones.save(salon));
    }

    @Transactional
    public SalonDto modificarSalon(Long id, SalonSolicitud s) {
        Salon salon = buscar(salones, id, "Salón");
        aplicar(salon, s);
        if (s.activo() != null) {
            salon.setActivo(s.activo());
        }
        return dto(salon);
    }

    @Transactional
    public void darDeBajaSalon(Long id) {
        buscar(salones, id, "Salón").setActivo(false);
    }

    @Transactional(readOnly = true)
    public List<CaracteristicaAsignadaDto> caracteristicasSalon(Long id) {
        buscar(salones, id, "Salón");
        return caracteristicasSalon.findBySalonId(id).stream()
                .map(a -> Caracteristicas.dto(a.getCaracteristica(), a.getCantidad()))
                .sorted(Comparator.comparing(CaracteristicaAsignadaDto::grupo, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(CaracteristicaAsignadaDto::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public List<CaracteristicaAsignadaDto> reemplazarCaracteristicasSalon(Long id,
                                                                          List<CaracteristicaAsignadaSolicitud> lista) {
        Salon salon = buscar(salones, id, "Salón");
        Caracteristicas.validarSinRepetidos(lista);
        Map<Long, SalonCaracteristica> actuales = caracteristicasSalon.findBySalonId(id).stream()
                .collect(Collectors.toMap(a -> a.getCaracteristica().getId(), Function.identity()));
        Set<Long> pedidas = new HashSet<>();
        for (CaracteristicaAsignadaSolicitud s : lista) {
            pedidas.add(s.caracteristicaId());
            SalonCaracteristica a = actuales.get(s.caracteristicaId());
            Caracteristica c = a != null ? a.getCaracteristica()
                    : buscar(caracteristicas, s.caracteristicaId(), "Característica");
            Caracteristicas.validarCantidad(c, s.cantidad());
            if (a == null) {
                a = new SalonCaracteristica();
                a.setSalon(salon);
                a.setCaracteristica(c);
                a.setCantidad(s.cantidad());
                caracteristicasSalon.save(a);
            } else {
                a.setCantidad(s.cantidad());
            }
        }
        actuales.forEach((idCar, a) -> {
            if (!pedidas.contains(idCar)) {
                caracteristicasSalon.delete(a);
            }
        });
        caracteristicasSalon.flush();
        return caracteristicasSalon(id);
    }

    private static void aplicar(Salon salon, SalonSolicitud s) {
        salon.setNombre(s.nombre().trim());
        salon.setCapAuditorio(s.capAuditorio());
        salon.setCapBanquete(s.capBanquete());
        salon.setCapCoctel(s.capCoctel());
        salon.setM2(s.m2());
        salon.setEstadoConservacion(texto(s.estadoConservacion()));
        salon.setEstadoFuncionamiento(texto(s.estadoFuncionamiento()));
    }

    // ------------------------------------------------------------------ visitas guiadas (N por oferta)

    @Transactional(readOnly = true)
    public List<VisitaGuiadaDto> visitas(Long idOferta) {
        buscar(ofertas, idOferta, "Oferta");
        return visitas.findByOfertaIdOrderById(idOferta).stream().map(this::dto).toList();
    }

    @Transactional
    public VisitaGuiadaDto crearVisita(Long idOferta, VisitaGuiadaSolicitud s) {
        VisitaGuiada v = new VisitaGuiada();
        v.setOferta(buscar(ofertas, idOferta, "Oferta"));
        aplicar(v, s);
        return dto(visitas.save(v));
    }

    @Transactional
    public VisitaGuiadaDto modificarVisita(Long id, VisitaGuiadaSolicitud s) {
        VisitaGuiada v = buscar(visitas, id, "Visita guiada");
        aplicar(v, s);
        return dto(v);
    }

    /** Elimina la visita y sus horarios (no tiene baja lógica; para ocultarla usar habilitada=false). */
    @Transactional
    public void eliminarVisita(Long id) {
        VisitaGuiada v = buscar(visitas, id, "Visita guiada");
        horarios.eliminarDeVisita(id);
        visitas.delete(v);
    }

    private static void aplicar(VisitaGuiada v, VisitaGuiadaSolicitud s) {
        v.setModalidad(texto(s.modalidad()));
        v.setRequiereReserva(Boolean.TRUE.equals(s.requiereReserva()));
        v.setHabilitada(s.habilitada() == null || s.habilitada());
    }

    // ------------------------------------------------------------------ DTO (también los usa el detalle de oferta)

    public AlojamientoDto alojamientoOpcional(Long idOferta) {
        return alojamientos.findById(idOferta).map(EspecializacionService::dto).orElse(null);
    }

    public CircuitoDto circuitoOpcional(Long idOferta) {
        return circuitos.findById(idOferta).map(EspecializacionService::dto).orElse(null);
    }

    private static AlojamientoDto dto(Alojamiento a) {
        return new AlojamientoDto(a.getEstrellas(), a.getCantHabitaciones(), a.getTotalPlazas(), a.getHabAccesibles(),
                a.getPlazasAccesibles(), a.getPersonalPermanente(), a.getPersonalNoPermanente());
    }

    private static CircuitoDto dto(Circuito c) {
        return new CircuitoDto(c.getLugarPartida() == null ? null : c.getLugarPartida().getId(),
                c.getLugarPartida() == null ? null : c.getLugarPartida().getNombre(),
                c.getDuracionMin(), GeoJson.aGeoJson(c.getTrazado()));
    }

    private static SalonDto dto(Salon s) {
        return new SalonDto(s.getId(), s.getOferta().getId(), s.getNombre(), s.getCapAuditorio(), s.getCapBanquete(),
                s.getCapCoctel(), s.getM2(), s.getEstadoConservacion(), s.getEstadoFuncionamiento(),
                Boolean.TRUE.equals(s.getActivo()));
    }

    private VisitaGuiadaDto dto(VisitaGuiada v) {
        return new VisitaGuiadaDto(v.getId(), v.getOferta().getId(), v.getModalidad(),
                Boolean.TRUE.equals(v.getRequiereReserva()), Boolean.TRUE.equals(v.getHabilitada()),
                v.getId() == null ? List.of() : horarios.deVisita(v.getId()));
    }
}
