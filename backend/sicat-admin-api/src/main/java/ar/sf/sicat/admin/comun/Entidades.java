package ar.sf.sicat.admin.comun;

import org.springframework.data.repository.CrudRepository;

import ar.sf.sicat.core.comun.EntidadNoEncontradaException;

/** Utilidades para los servicios de ABM. */
public final class Entidades {

    private Entidades() {
    }

    /** Busca por id o lanza 404. */
    public static <T, ID> T buscar(CrudRepository<T, ID> repo, ID id, String nombre) {
        return repo.findById(id).orElseThrow(() -> new EntidadNoEncontradaException(nombre, id));
    }

    /** Igual que {@link #buscar} pero admite id null (devuelve null). */
    public static <T, ID> T buscarOpcional(CrudRepository<T, ID> repo, ID id, String nombre) {
        return id == null ? null : buscar(repo, id, nombre);
    }

    public static boolean activo(Boolean b) {
        return Boolean.TRUE.equals(b);
    }

    /** Texto recortado; null si queda vacío. */
    public static String texto(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
