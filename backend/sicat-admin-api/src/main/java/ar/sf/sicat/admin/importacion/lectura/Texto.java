package ar.sf.sicat.admin.importacion.lectura;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Normalización de los valores tal como vienen en las planillas. */
public final class Texto {

    /** Valores que en las planillas significan "sin dato". */
    private static final Set<String> VACIOS = Set.of("", "-", "_", "--", ".", "s/n", "sn", "n/a", "na", "no tiene",
            "sin dato", "sin datos", "x");
    private static final Pattern NUMERO_CIENTIFICO = Pattern.compile("^\\d+(\\.\\d+)?E\\d+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENTERO_DECIMAL = Pattern.compile("^(\\d+)\\.0+$");
    private static final Pattern ANIO = Pattern.compile("\\b(1[5-9]\\d{2}|20\\d{2})\\b");
    private static final Pattern PRIMER_ENTERO = Pattern.compile("(\\d+)");
    /** Toma el último número precedido de espacio como altura ("Reg. 12 de Infantería 783" → 783). */
    private static final Pattern CALLE_ALTURA = Pattern.compile("^(.*[^\\d\\s])\\s+(?:N[°º]\\s*)?(\\d{1,5})\\b(.*)$");

    private Texto() {
    }

    /** Recorta, colapsa espacios y devuelve null si es un valor "vacío" ("-", "S/N", etc.). */
    public static String limpio(String s) {
        if (s == null) {
            return null;
        }
        String t = s.replace('\u00A0', ' ').replaceAll("[ \\t]+", " ").replaceAll("\\s*\\n\\s*", "\n").trim();
        return VACIOS.contains(t.toLowerCase(Locale.ROOT)) ? null : t;
    }

    /** Minúsculas, sin acentos, sin puntuación, espacios simples. Para comparar nombres y direcciones. */
    public static String clave(String s) {
        if (s == null) {
            return "";
        }
        String sinMarcas = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinMarcas.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    /** Encabezado normalizado: mayúsculas, sin acentos, primera línea, sin ":" ni "*" final. */
    public static String encabezado(String s) {
        if (s == null) {
            return "";
        }
        String primera = s.split("\\n")[0];
        return clave(primera).toUpperCase(Locale.ROOT);
    }

    /** Excel guarda teléfonos como números: 3.425260204E9 → "3425260204", 4564143.0 → "4564143". */
    public static String numeroComoTexto(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (NUMERO_CIENTIFICO.matcher(t).matches()) {
            return new java.math.BigDecimal(t).toPlainString();
        }
        Matcher m = ENTERO_DECIMAL.matcher(t);
        return m.matches() ? m.group(1) : t;
    }

    /**
     * Separa una celda con varios valores. Corta en coma, punto y coma, barra vertical, salto de línea y
     * ". " (las delegaciones usan punto), pero no dentro de paréntesis.
     */
    public static List<String> lista(String s) {
        List<String> r = new ArrayList<>();
        if (s == null) {
            return r;
        }
        StringBuilder actual = new StringBuilder();
        int parentesis = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                parentesis++;
            } else if (c == ')' && parentesis > 0) {
                parentesis--;
            }
            boolean corte = parentesis == 0 && (c == ',' || c == ';' || c == '|' || c == '\n'
                    || (c == '.' && i + 1 < s.length() && s.charAt(i + 1) == ' ' && !esAbreviatura(actual)));
            if (corte) {
                agregar(r, actual);
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        agregar(r, actual);
        return r;
    }

    /** Abreviaturas frecuentes en las planillas: un punto después de ellas no separa valores. */
    private static final Set<String> ABREVIATURAS = Set.of("min", "mts", "mt", "m", "km", "bv", "av", "avda", "gral",
            "dr", "dra", "nro", "n", "sr", "sra", "aprox", "tel", "pje", "sta", "sto", "nac", "prov", "ej", "etc");

    private static boolean esAbreviatura(StringBuilder s) {
        String t = s.toString().trim();
        int espacio = t.lastIndexOf(' ');
        String ultima = espacio < 0 ? t : t.substring(espacio + 1);
        return !ultima.endsWith(")") && ABREVIATURAS.contains(clave(ultima));
    }

    private static void agregar(List<String> r, StringBuilder actual) {
        String v = limpio(actual.toString().replaceAll("[.*]+$", ""));
        if (v != null && !v.isBlank()) {
            r.add(Character.toUpperCase(v.charAt(0)) + v.substring(1));
        }
    }

    /**
     * Separa valores múltiples de contacto: "a / b" y "a // b" (con espacios, para no cortar URLs),
     * "a y b" entre correos, punto y coma y saltos de línea.
     */
    public static List<String> valoresContacto(String s) {
        return separar(s, "\\s+/{1,2}\\s+|\\s*\\n\\s*|\\s*;\\s*|\\s+y\\s+(?=\\S+@)");
    }

    /** Como {@link #valoresContacto} pero también corta en "/" sin espacios: "0342-6103638/4506600". */
    public static List<String> valoresTelefono(String s) {
        return separar(s, "\\s*/{1,2}\\s*|\\s*\\n\\s*|\\s*;\\s*");
    }

    private static List<String> separar(String s, String regex) {
        List<String> r = new ArrayList<>();
        if (s == null) {
            return r;
        }
        for (String parte : s.split(regex)) {
            String v = limpio(numeroComoTexto(parte));
            if (v != null && !v.matches("[\\s/.-]*")) {
                r.add(v);
            }
        }
        return r;
    }

    /** Instagram sin URL ni "@" (protocolo del modelo normalizado). */
    public static String usuarioInstagram(String s) {
        String t = limpio(s);
        if (t == null) {
            return null;
        }
        t = t.replaceAll("(?i)^https?://(www\\.)?instagram\\.com/", "").replaceAll("[/?].*$", "").replaceFirst("^@", "");
        int parentesis = t.indexOf("(@");
        if (parentesis >= 0) {
            t = t.substring(parentesis + 2).replaceAll("\\).*$", "");
        }
        return limpio(t.trim());
    }

    public static Optional<Short> anio(String s) {
        if (s == null) {
            return Optional.empty();
        }
        Matcher m = ANIO.matcher(numeroComoTexto(s));
        return m.find() ? Optional.of(Short.valueOf(m.group(1))) : Optional.empty();
    }

    public static Optional<Integer> entero(String s) {
        String t = limpio(numeroComoTexto(s));
        if (t == null) {
            return Optional.empty();
        }
        Matcher m = PRIMER_ENTERO.matcher(t);
        return m.find() ? Optional.of(Integer.valueOf(m.group(1))) : Optional.empty();
    }

    public static boolean afirmativo(String s) {
        String c = clave(s);
        return c.equals("si") || c.startsWith("si ");
    }

    /** "San Martín 2068" → [San Martín, 2068, ""]; si no hay número, vacío. */
    public static Optional<String[]> calleYAltura(String direccion) {
        if (direccion == null) {
            return Optional.empty();
        }
        Matcher m = CALLE_ALTURA.matcher(direccion.split("[\\n(]")[0].trim());
        if (!m.matches() || m.group(1).trim().toLowerCase(Locale.ROOT).endsWith("km")) {
            return Optional.empty();
        }
        return Optional.of(new String[] {m.group(1).trim().replaceAll("[,.]$", ""), m.group(2), m.group(3).trim()});
    }

    private static final Pattern FECHA_EN_INGLES = Pattern.compile(
            "(?i)\\b(\\d{1,2}) de (January|February|March|April|May|June|July|August|September|October|November|December)\\s+0*(\\d{1,5})\\b");
    private static final Map<String, String> MESES = Map.ofEntries(
            Map.entry("january", "Enero"), Map.entry("february", "Febrero"), Map.entry("march", "Marzo"),
            Map.entry("april", "Abril"), Map.entry("may", "Mayo"), Map.entry("june", "Junio"),
            Map.entry("july", "Julio"), Map.entry("august", "Agosto"), Map.entry("september", "Septiembre"),
            Map.entry("october", "Octubre"), Map.entry("november", "Noviembre"), Map.entry("december", "Diciembre"));

    /**
     * Calles con nombre de fecha que la planilla convirtió en fecha en inglés:
     * "4 de January 0895" → "4 de Enero 895".
     */
    public static String repararFecha(String direccion) {
        if (direccion == null) {
            return null;
        }
        Matcher m = FECHA_EN_INGLES.matcher(direccion);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, m.group(1) + " de " + MESES.get(m.group(2).toLowerCase(Locale.ROOT)) + " " + m.group(3));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static String recortar(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max - 1).trim() + "…";
    }
}
