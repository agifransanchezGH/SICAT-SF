package ar.sf.sicat.core.comun;

/** Se traduce a HTTP 404 (RFC 7807) en las APIs. */
public class EntidadNoEncontradaException extends RuntimeException {

    public EntidadNoEncontradaException(String entidad, Object id) {
        super(entidad + " " + id + " no encontrado");
    }
}
