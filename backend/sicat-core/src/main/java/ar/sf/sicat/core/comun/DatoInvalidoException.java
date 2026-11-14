package ar.sf.sicat.core.comun;

/** Dato de entrada mal formado que no cubre jakarta.validation (p. ej. GeoJSON o bbox). Se traduce a HTTP 400. */
public class DatoInvalidoException extends RuntimeException {

    public DatoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
