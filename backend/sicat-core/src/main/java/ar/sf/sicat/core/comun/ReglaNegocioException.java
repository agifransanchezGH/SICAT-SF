package ar.sf.sicat.core.comun;

import java.util.List;

/**
 * Violación de una regla de negocio (p. ej. publicar sin datos mínimos). Se traduce a HTTP 422.
 * {@code errores} detalla cada motivo (sale en la propiedad "errores" del Problem Details).
 */
public class ReglaNegocioException extends RuntimeException {

    private final List<String> errores;

    public ReglaNegocioException(String mensaje) {
        this(mensaje, List.of());
    }

    public ReglaNegocioException(String mensaje, List<String> errores) {
        super(mensaje);
        this.errores = List.copyOf(errores);
    }

    public List<String> getErrores() {
        return errores;
    }
}
