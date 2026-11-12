package ar.sf.sicat.publica.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import ar.sf.sicat.core.comun.EntidadNoEncontradaException;

/** Errores en formato RFC 7807. */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    @ExceptionHandler(EntidadNoEncontradaException.class)
    ProblemDetail noEncontrado(EntidadNoEncontradaException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Recurso no encontrado");
        return pd;
    }
}
