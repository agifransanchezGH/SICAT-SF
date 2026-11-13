package ar.sf.sicat.admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import ar.sf.sicat.core.comun.ConflictoException;
import ar.sf.sicat.core.comun.DatoInvalidoException;
import ar.sf.sicat.core.comun.EntidadNoEncontradaException;
import ar.sf.sicat.core.comun.ReglaNegocioException;

/**
 * Errores en formato RFC 7807 ({@code application/problem+json}).
 * Las validaciones de jakarta.validation ya las resuelve {@link ResponseEntityExceptionHandler} como 400.
 * Los 401/403 por token faltante o rol insuficiente los resuelve Spring Security.
 */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(EntidadNoEncontradaException.class)
    ProblemDetail noEncontrado(EntidadNoEncontradaException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    ProblemDetail reglaNegocio(ReglaNegocioException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Regla de negocio", ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    ProblemDetail conflicto(ConflictoException ex) {
        return problema(HttpStatus.CONFLICT, "Conflicto", ex.getMessage());
    }

    @ExceptionHandler(DatoInvalidoException.class)
    ProblemDetail datoInvalido(DatoInvalidoException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Dato inválido", ex.getMessage());
    }

    /** Red de seguridad: una restricción de la base (única, FK, CHECK) que no validó el servicio. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridad(DataIntegrityViolationException ex) {
        log.warn("Restricción de integridad: {}", ex.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "Conflicto de datos",
                "La operación viola una restricción de la base de datos (registro duplicado o referenciado).");
    }

    /** Login o refresh fallidos (lanzados desde los servicios de seguridad). */
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail noAutenticado(AuthenticationException ex) {
        return problema(HttpStatus.UNAUTHORIZED, "No autenticado", ex.getMessage());
    }

    /** {@code ?sort=} con una propiedad inexistente. */
    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail ordenInvalido(PropertyReferenceException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Orden inválido", ex.getMessage());
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalle) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalle);
        pd.setTitle(titulo);
        return pd;
    }
}
