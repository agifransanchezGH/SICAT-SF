package ar.sf.sicat.publica.config;

import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import ar.sf.sicat.core.comun.DatoInvalidoException;
import ar.sf.sicat.core.comun.EntidadNoEncontradaException;

/** Errores en formato RFC 7807. */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    @ExceptionHandler(EntidadNoEncontradaException.class)
    ProblemDetail noEncontrado(EntidadNoEncontradaException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    /** bbox mal formado. */
    @ExceptionHandler(DatoInvalidoException.class)
    ProblemDetail datoInvalido(DatoInvalidoException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Dato inválido", ex.getMessage());
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
