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

    // ------------------------------------------------------------------ características (ADR 0017)

    private static final Pattern DIRECCION = Pattern.compile(
            "\\b(calle|av|avda|avenida|bv|boulevard|bulevar|ruta|esquina|esq)\\b\\.?|\\p{L}{3,}\\.?\\s+n?[°º]?\\s*\\d{3,5}\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern NUMEROS = Pattern.compile("\\d+");
    /** Parecido mínimo para tomar dos nombres como la misma característica (errores de tipeo, plurales). */
    private static final double SIMILITUD_MISMA = 0.85;
    /** Palabras que cambian el sentido: "Piscina climatizada" y "Piscina no climatizada" no son la misma. */
    private static final Set<String> NEGACIONES = Set.of("no", "sin", "ni");

    /**
     * Nombre de característica prolijo: sin puntuación suelta al principio ni al final (".Darsenas"), espacios
     * simples, sin espacios pegados a los paréntesis ("( en escaleras)") y, si viene todo en mayúsculas, en tipo
     * oración ("RAMPAS EN LAS ESQUINAS" → "Rampas en las esquinas"). Siempre empieza con mayúscula.
     */
    public static String normalizarCaracteristica(String s) {
        String t = limpio(s);
        if (t == null) {
            return null;
        }
        t = t.replaceAll("\\s+", " ")
                .replaceAll("^[\\s.,;:\\-–_*/·]+", "")
                .replaceAll("[\\s.,;:\\-–_*/·]+$", "")
                .replaceAll("\\(\\s+", "(")
                .replaceAll("\\s+\\)", ")")
                .trim();
        if (t.isEmpty()) {
            return null;
        }
        boolean todoMayusculas = t.chars().anyMatch(Character::isUpperCase) && t.chars().noneMatch(Character::isLowerCase);
        if (todoMayusculas && t.length() > 3) {
            t = t.toLowerCase(Locale.of("es", "AR"));
        }
        return t.substring(0, 1).toUpperCase(Locale.of("es", "AR")) + t.substring(1);
    }

    /** Una "característica" que en realidad es una dirección ("Por calle Lavaisse 610"): va a notas internas. */
    public static boolean pareceDireccion(String s) {
        return s != null && DIRECCION.matcher(s).find();
    }

    /** Parecido entre dos textos, de 0 a 1 (distancia de Levenshtein relativa). */
    public static double similitud(String a, String b) {
        if (a.equals(b)) {
            return 1;
        }
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        int[] previa = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previa[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            int[] actual = new int[b.length() + 1];
            actual[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int costo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                actual[j] = Math.min(Math.min(previa[j] + 1, actual[j - 1] + 1), previa[j - 1] + costo);
            }
            previa = actual;
        }
        return 1 - (double) previa[b.length()] / Math.max(a.length(), b.length());
    }

    /**
     * Dos nombres de característica que son la misma con un error de tipeo o una diferencia de plural
     * ("Grifo monocomando" / "Grifo monomando", "Sistema contra incendio" / "incendios"). No se juntan nombres
     * cortos (menos de 8 letras), los que tienen números distintos ("cap. 200" / "cap. 700") ni los que difieren
     * en una negación ("climatizada" / "no climatizada").
     */
    public static boolean mismaCaracteristica(String a, String b) {
        String ka = clave(a);
        String kb = clave(b);
        if (ka.equals(kb)) {
            return true;
        }
        if (Math.min(ka.length(), kb.length()) < 8 || !numeros(ka).equals(numeros(kb)) || !negaciones(ka).equals(negaciones(kb))) {
            return false;
        }
        return similitud(ka, kb) >= SIMILITUD_MISMA;
    }

    private static List<String> negaciones(String s) {
        return java.util.Arrays.stream(s.split(" ")).filter(NEGACIONES::contains).toList();
    }

    private static List<String> numeros(String s) {
        List<String> r = new ArrayList<>();
        Matcher m = NUMEROS.matcher(s);
        while (m.find()) {
            r.add(m.group());
        }
        return r;
    }
}
