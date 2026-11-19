package ar.sf.sicat.admin.importacion;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Cuándo dos contactos son el mismo: mismo tipo y mismo valor sin importar mayúsculas, espacios, acentos, "@",
 * "https://", "www." ni la barra final; los perfiles de Instagram y Facebook valen igual como usuario o como URL.
 * En teléfonos y WhatsApp se comparan solo los dígitos ("342 456-7890" = "3424567890").
 */
final class ClaveContacto {

    private ClaveContacto() {
    }

    static String de(String tipo, String valor) {
        String t = simple(tipo);
        return t + "|" + valor(t, valor);
    }

    private static String valor(String tipo, String valor) {
        String v = simple(valor);
        if (tipo.contains("tel") || tipo.contains("whats") || tipo.contains("cel")) {
            String digitos = v.replaceAll("\\D", "");
            return digitos.isEmpty() ? v : digitos;
        }
        v = v.replaceFirst("^https?://", "").replaceFirst("^(www|m|web)\\.", "");
        // Perfil de red social: "instagram.com/museo" y "@museo" son el mismo.
        v = v.replaceFirst("^(instagram\\.com|facebook\\.com|fb\\.com|fb\\.me)/", "");
        v = v.replaceFirst("^@", "").replaceFirst("[/?#]+$", "");
        return v.replace(" ", "");
    }

    private static String simple(String s) {
        if (s == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
