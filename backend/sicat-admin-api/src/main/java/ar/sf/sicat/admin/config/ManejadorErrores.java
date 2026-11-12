package ar.sf.sicat.admin.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import ar.sf.sicat.core.comun.EntidadNoEncontradaException;
import ar.sf.sicat.core.comun.ReglaNegocioException;

/**
 * Errores en formato RFC 7807 ({@code application/problem+json}).
 * Las validaciones de jakarta.validation ya las resuelve {@link ResponseEntityExceptionHandler} como 400.
 */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    @ExceptionHandler(EntidadNoEncontradaException.class)
    ProblemDetail noEncontrado(EntidadNoEncontradaException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Recurso no encontrado");
        return pd;
    }

    @ExceptionHandler(ReglaNegocioException.class)
    ProblemDetail reglaNegocio(ReglaNegocioException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setTitle("Regla de negocio");
        return pd;
    }
}
