package ar.sf.sicat.core.comun;

/** Violación de una regla de negocio (p. ej. publicar sin datos mínimos). Se traduce a HTTP 422. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
