package ar.sf.sicat.admin.importacion;

import java.util.Set;
import java.util.regex.Pattern;

import ar.sf.sicat.admin.importacion.lectura.Texto;

/**
 * Qué de las planillas es de uso interno y no debe salir en el sitio público (ADR 0015).
 * Es el mismo criterio que aplicó la migración V11 a lo ya importado: mantenerlos iguales.
 */
public final class Internos {

    /** Grupos de características de uso interno (comparados con {@link Texto#clave}). */
    static final Set<String> GRUPOS = Set.of(
            "Plataformas de venta", "Redes que utiliza", "Uso de redes sociales",
            "Información y calidad", "Dominio", "Otros", "Seguridad").stream()
            .map(Texto::clave).collect(java.util.stream.Collectors.toUnmodifiableSet());

    /** Etiquetas de preguntas de la encuesta y datos internos: van a notas internas, no a la descripción. */
    private static final Pattern ETIQUETA = Pattern.compile(
            "^\\s*(s[ií] seleccion|seleccion|s[ií] marc[oó]|remodelaciones|reformas|cantidad de personal|capital humano"
                    + "|organismo otorgante|distinciones? de calidad|estado de (las )?rampas|c?observaciones|mail)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private Internos() {
    }

    public static boolean grupoInterno(String grupo) {
        return grupo != null && GRUPOS.contains(Texto.clave(grupo));
    }

    public static boolean etiquetaInterna(String etiqueta) {
        return etiqueta != null && ETIQUETA.matcher(etiqueta).find();
    }
}
