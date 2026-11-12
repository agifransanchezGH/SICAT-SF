package ar.sf.sicat.core.comun;

/** El recurso choca con otro existente (p. ej. email repetido). Se traduce a HTTP 409. */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
