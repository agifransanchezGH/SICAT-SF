package ar.sf.sicat.admin.comun;

/** Expresiones de {@code @PreAuthorize} usadas en los controllers de ABM (ARQUITECTURA.md §6). */
public final class Roles {

    /** Lectura: cualquier usuario autenticado (ADMIN, CARGADOR, CONSULTA). */
    public static final String LECTURA = "isAuthenticated()";
    /** Altas y modificaciones. */
    public static final String CARGA = "hasAnyRole('ADMIN', 'CARGADOR')";
    /** Bajas de entidades principales y catálogos. */
    public static final String ADMIN = "hasRole('ADMIN')";

    private Roles() {
    }
}
