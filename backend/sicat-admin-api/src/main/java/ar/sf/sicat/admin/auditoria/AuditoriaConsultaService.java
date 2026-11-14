package ar.sf.sicat.admin.auditoria;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.sf.sicat.admin.auditoria.dto.AuditoriaDto;
import ar.sf.sicat.admin.auditoria.dto.AuditoriaFiltro;
import ar.sf.sicat.admin.auditoria.dto.UsuarioResumen;
import ar.sf.sicat.core.auditoria.Auditoria;
import ar.sf.sicat.core.auditoria.AuditoriaRepository;
import ar.sf.sicat.core.comun.PaginaDto;
import ar.sf.sicat.core.seguridad.Usuario;
import jakarta.persistence.criteria.Predicate;

/** Consulta de la auditoría (solo lectura). */
@Service
public class AuditoriaConsultaService {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    private final AuditoriaRepository repo;
    private final ObjectMapper json;

    public AuditoriaConsultaService(AuditoriaRepository repo, ObjectMapper json) {
        this.repo = repo;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public PaginaDto<AuditoriaDto> buscar(AuditoriaFiltro f, Pageable pageable) {
        return PaginaDto.de(repo.findAll(especificacion(f), pageable), this::aDto);
    }

    /** Historial completo de un registro, del cambio más reciente al más antiguo. */
    @Transactional(readOnly = true)
    public List<AuditoriaDto> historial(String tabla, String clave) {
        AuditoriaFiltro f = new AuditoriaFiltro(tabla, clave, null, null, null, null, null);
        return repo.findAll(especificacion(f), Sort.by(Sort.Order.desc("fechaHora"), Sort.Order.desc("id")))
                .stream().map(this::aDto).toList();
    }

    private static Specification<Auditoria> especificacion(AuditoriaFiltro f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.tabla() != null && !f.tabla().isBlank()) {
                String tabla = f.tabla().trim().toLowerCase();
                p.add(tabla.contains(".")
                        ? cb.equal(root.get("tablaAfectada"), tabla)
                        : cb.or(cb.equal(root.get("tablaAfectada"), tabla),
                                cb.like(root.get("tablaAfectada"), "%." + tabla)));
            }
            if (f.clave() != null && !f.clave().isBlank()) {
                p.add(cb.equal(root.get("claveRegistro"), f.clave().trim()));
            }
            if (f.usuario() != null) {
                p.add(cb.equal(root.get("usuario").get("id"), f.usuario()));
            }
            if (f.accion() != null) {
                p.add(cb.equal(root.get("accion"), f.accion()));
            }
            if (f.origen() != null) {
                p.add(cb.equal(root.get("origen"), f.origen()));
            }
            if (f.desde() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("fechaHora"),
                        f.desde().atStartOfDay(ZONA).toOffsetDateTime()));
            }
            if (f.hasta() != null) {
                p.add(cb.lessThan(root.get("fechaHora"),
                        f.hasta().plusDays(1).atStartOfDay(ZONA).toOffsetDateTime()));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private AuditoriaDto aDto(Auditoria a) {
        Usuario u = a.getUsuario();
        UsuarioResumen usuario = u == null ? null : new UsuarioResumen(u.getId(), u.getNombreCompleto(), u.getEmail());
        return new AuditoriaDto(a.getId(), a.getTablaAfectada(), a.getClaveRegistro(), a.getAccion(), usuario,
                a.getFechaHora(), a.getOrigen(), leer(a.getDatosAnteriores()), leer(a.getDatosNuevos()));
    }

    private JsonNode leer(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            return json.readTree(texto);
        } catch (JsonProcessingException e) {
            return json.getNodeFactory().textNode(texto);
        }
    }
}
