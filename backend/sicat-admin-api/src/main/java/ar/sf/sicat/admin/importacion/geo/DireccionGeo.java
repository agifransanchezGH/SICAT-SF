package ar.sf.sicat.admin.importacion.geo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prepara una dirección para buscarla en OpenStreetMap: expande abreviaturas ("Bv." → "Bulevar",
 * "Gral." → "General") y quita lo que confunde al buscador (paréntesis, local, piso, "N°").
 */
public final class DireccionGeo {

    private static final Map<String, String> ABREVIATURAS = new LinkedHashMap<>();

    static {
        ABREVIATURAS.put("bvard|bvd|blvd|bv|boulevard|boulevar", "Bulevar");
        ABREVIATURAS.put("avda|av", "Avenida");
        ABREVIATURAS.put("gral", "General");
        ABREVIATURAS.put("pte", "Presidente");
        ABREVIATURAS.put("pje|psje", "Pasaje");
        ABREVIATURAS.put("dr", "Doctor");
        ABREVIATURAS.put("dra", "Doctora");
        ABREVIATURAS.put("alte|almte", "Almirante");
        ABREVIATURAS.put("cnel", "Coronel");
        ABREVIATURAS.put("mons", "Monseñor");
        ABREVIATURAS.put("gdor", "Gobernador");
        ABREVIATURAS.put("ing", "Ingeniero");
        ABREVIATURAS.put("prof", "Profesor");
        ABREVIATURAS.put("pbro", "Presbítero");
        ABREVIATURAS.put("tte", "Teniente");
        ABREVIATURAS.put("cmte|cte", "Comandante");
        ABREVIATURAS.put("sgto", "Sargento");
        ABREVIATURAS.put("sta", "Santa");
        ABREVIATURAS.put("sto", "Santo");
    }

    private static final Pattern PARENTESIS = Pattern.compile("\\([^)]*\\)?");
    private static final Pattern SOBRANTE = Pattern.compile(
            "(?i)\\b(local|loc|piso|dpto|depto|departamento|of|oficina|pb|planta baja)\\b\\.?\\s*[\\w°º-]*");
    private static final Pattern NUMERO = Pattern.compile("(?i)\\b(n°|nº|nro\\.?|n\\.)\\s*");

    private DireccionGeo() {
    }

    /** "Bv. Gálvez 1620 (Local 3)" → "Bulevar Gálvez 1620". */
    public static String limpiar(String direccion) {
        if (direccion == null) {
            return null;
        }
        String t = direccion.replace('\n', ' ');
        t = PARENTESIS.matcher(t).replaceAll(" ");
        t = SOBRANTE.matcher(t).replaceAll(" ");
        t = NUMERO.matcher(t).replaceAll("");
        t = t.replaceAll("(?i)\\bs/n\\b", " ");
        return expandir(t).replaceAll("\\s+", " ").replaceAll("[\\s,.-]+$", "").trim();
    }

    /** Solo expande abreviaturas: "Gral. Paz" → "General Paz". */
    public static String expandir(String texto) {
        if (texto == null) {
            return null;
        }
        String t = texto;
        for (Map.Entry<String, String> e : ABREVIATURAS.entrySet()) {
            Matcher m = Pattern.compile("(?i)(?<![\\p{L}])(" + e.getKey() + ")\\.?(?=\\s|$)").matcher(t);
            t = m.replaceAll(Matcher.quoteReplacement(e.getValue()));
        }
        return t;
    }
}
