package ar.sf.sicat.admin.publicacion;

import static ar.sf.sicat.admin.comun.Entidades.buscar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.sf.sicat.admin.publicacion.dto.EstadoPublicacionDto;
import ar.sf.sicat.core.comun.ReglaNegocioException;
import ar.sf.sicat.core.dominio.establecimiento.EstadoPublicacion;
import ar.sf.sicat.core.dominio.establecimiento.Establecimiento;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoRepository;
import ar.sf.sicat.core.dominio.lugar.Lugar;
import ar.sf.sicat.core.dominio.oferta.Oferta;
import ar.sf.sicat.core.dominio.oferta.OfertaRepository;

/**
 * Publicación de establecimientos (ADR 0008). Un establecimiento se puede publicar si:
 * <ul>
 *   <li>él y su oferente están activos;</li>
 *   <li>tiene un lugar con coordenadas (para el mapa);</li>
 *   <li>tiene al menos una oferta activa cuya categoría principal está activa.</li>
 * </ul>
 * Mientras está PUBLICADO, ningún cambio puede dejarlo sin esos requisitos: los servicios de ABM
 * llaman a {@link #verificarPublicados} y el cambio se rechaza (422) si los incumple.
 */
@Service
public class PublicacionService {

    private final EstablecimientoRepository establecimientos;
    private final OfertaRepository ofertas;

    public PublicacionService(EstablecimientoRepository establecimientos, OfertaRepository ofertas) {
        this.establecimientos = establecimientos;
        this.ofertas = ofertas;
    }

    @Transactional(readOnly = true)
    public EstadoPublicacionDto estado(Long id) {
        Establecimiento e = buscar(establecimientos, id, "Establecimiento");
        List<Faltante> faltan = evaluar(e, ofertas.findByEstablecimientoIdOrderById(e.getId()));
        return new EstadoPublicacionDto(e.getEstadoPublicacion(), faltan.isEmpty(),
                faltan.stream().map(Faltante::mensaje).toList(),
                faltan.stream().map(f -> new EstadoPublicacionDto.Requisito(f.requisito(), f.requisito().corto(),
                        f.mensaje())).toList());
    }

    @Transactional
    public void publicar(Long id) {
        Establecimiento e = buscar(establecimientos, id, "Establecimiento");
        List<String> faltantes = faltantes(e);
        if (!faltantes.isEmpty()) {
            throw new ReglaNegocioException("El establecimiento no cumple los requisitos para publicarse", faltantes);
        }
        e.setEstadoPublicacion(EstadoPublicacion.PUBLICADO);
    }

    /** PUBLICADO → OCULTO (OCULTO queda igual). Un BORRADOR nunca se publicó: 422. */
    @Transactional
    public void despublicar(Long id) {
        Establecimiento e = buscar(establecimientos, id, "Establecimiento");
        if (e.getEstadoPublicacion() == EstadoPublicacion.BORRADOR) {
            throw new ReglaNegocioException("El establecimiento está en borrador: no hay nada que despublicar");
        }
        e.setEstadoPublicacion(EstadoPublicacion.OCULTO);
    }

    /** Lo que falta para poder publicar (vacío si cumple todo), con el mensaje largo. */
    public List<String> faltantes(Establecimiento e) {
        return evaluar(e, e.getId() == null ? List.of() : ofertas.findByEstablecimientoIdOrderById(e.getId()))
                .stream().map(Faltante::mensaje).toList();
    }

    /** Requisito incumplido y su mensaje con nombres. */
    public record Faltante(Requisito requisito, String mensaje) {
    }

    /**
     * Único lugar donde viven los requisitos. Recibe las ofertas ya cargadas para que un listado pueda
     * evaluar una página entera con una sola consulta. La consulta SQL equivalente (filtro "listos para
     * publicar" y contadores) es {@code EstablecimientoRepository.LISTOS_PARA_PUBLICAR}; un test las compara.
     */
    public List<Faltante> evaluar(Establecimiento e, List<Oferta> ofertasDelEstablecimiento) {
        List<Faltante> faltan = new ArrayList<>();
        if (!Boolean.TRUE.equals(e.getActivo())) {
            faltan.add(new Faltante(Requisito.ESTABLECIMIENTO_ACTIVO, "El establecimiento está dado de baja"));
        }
        if (!Boolean.TRUE.equals(e.getOferente().getActivo())) {
            faltan.add(new Faltante(Requisito.OFERENTE_ACTIVO,
                    "El oferente '" + e.getOferente().getNombre() + "' está dado de baja"));
        }
        Lugar lugar = e.getLugar();
        if (lugar == null) {
            faltan.add(new Faltante(Requisito.LUGAR, "Falta asignar un lugar"));
        } else if (lugar.getPunto() == null) {
            faltan.add(new Faltante(Requisito.COORDENADAS, "El lugar '" + lugar.getNombre() + "' no tiene coordenadas"));
        } else if ("AUTOMATICO".equals(lugar.getEstadoGeo()) || "APROXIMADO".equals(lugar.getEstadoGeo())) {
            faltan.add(new Faltante(Requisito.UBICACION_VERIFICADA, "Las coordenadas del lugar '" + lugar.getNombre()
                    + "' se calcularon automáticamente: verificalas en el mapa (estadoGeo = VERIFICADO)"));
        }
        boolean tieneOferta = ofertasDelEstablecimiento.stream()
                .anyMatch(o -> Boolean.TRUE.equals(o.getActivo()) && Boolean.TRUE.equals(o.getCategoria().getActivo()));
        if (!tieneOferta) {
            faltan.add(new Faltante(Requisito.OFERTA_ACTIVA, "Falta al menos una oferta activa con categoría activa"));
        }
        return faltan;
    }

    /**
     * Rechaza el cambio en curso si deja a algún establecimiento PUBLICADO sin los requisitos.
     * Se llama después de aplicar el cambio, dentro de la misma transacción (que se revierte con la excepción).
     */
    public void verificarPublicados(Collection<Establecimiento> afectados) {
        List<String> errores = new ArrayList<>();
        for (Establecimiento e : afectados) {
            if (e.getEstadoPublicacion() == EstadoPublicacion.PUBLICADO) {
                faltantes(e).forEach(f -> errores.add(e.getNombre() + " (id " + e.getId() + "): " + f));
            }
        }
        if (!errores.isEmpty()) {
            throw new ReglaNegocioException(
                    "El cambio dejaría incompleto un establecimiento publicado; despublicalo primero", errores);
        }
    }

    public void verificarPublicado(Establecimiento e) {
        verificarPublicados(List.of(e));
    }

    /** Un establecimiento que se da de baja deja de estar publicado. */
    public static void ocultarSiEstaPublicado(Establecimiento e) {
        if (e.getEstadoPublicacion() == EstadoPublicacion.PUBLICADO) {
            e.setEstadoPublicacion(EstadoPublicacion.OCULTO);
        }
    }
}
