package ar.sf.sicat.admin.importacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ar.sf.sicat.admin.importacion.lectura.LectorExcel.Fila;
import ar.sf.sicat.admin.importacion.lectura.LectorExcel.Hoja;
import ar.sf.sicat.admin.importacion.lectura.Texto;
import ar.sf.sicat.admin.importacion.modelo.Perfil;
import ar.sf.sicat.admin.importacion.modelo.Perfil.HojaConfig;
import ar.sf.sicat.admin.importacion.modelo.Registro;

/**
 * Convierte las filas de una hoja en {@link Registro}s. Las columnas conocidas se mapean al modelo; las que
 * no se reconocen no se pierden: las listas y los "Sí" pasan a características y los textos a la descripción.
 */
final class Interprete {

    private static final Pattern ESTRELLAS = Pattern.compile("(\\d)\\s*\\*");
    private static final Pattern URL = Pattern.compile("(?i)^(https?://|www\\.)\\S+$");
    /** Columnas que no aportan datos del establecimiento. */
    private static final Set<String> IGNORADAS = Set.of("MARCA TEMPORAL", "DIRECCION DE CORREO ELECTRONICO");

    /** Encabezado (prefijo normalizado) → grupo de características, para columnas con listas de valores. */
    private static final Map<String, String> GRUPOS = new LinkedHashMap<>();

    static {
        GRUPOS.put("TIPO DE HABITACIONES", "Tipos de habitación");
        GRUPOS.put("SELECCIONE LAS REDES SOCIALES", "Redes que utiliza");
        GRUPOS.put("CON QUE FIN", "Uso de redes sociales");
        GRUPOS.put("SELECCIONE EL SISTEMA DE RESERVA", "Sistema de reserva");
        GRUPOS.put("SELECCIONES LA APLICACION", "Plataformas de venta");
        GRUPOS.put("SELECCIONE LA MODALIDAD DE PAGO", "Medios de pago");
        GRUPOS.put("SELECCIONE LOS SERVICIOS", "Servicios");
        GRUPOS.put("SERVICIOS GENERALES", "Servicios");
        GRUPOS.put("SELECCIONE LO QUE TIENE", "Equipamiento");
        GRUPOS.put("EQUIPAMIENTO", "Equipamiento");
        GRUPOS.put("ACCESIBILIDAD FISICA DE INGRESO", "Accesibilidad: ingreso");
        GRUPOS.put("ACCESIBILIDAD FISICA DE PERMANENCIA", "Accesibilidad: permanencia y uso");
        GRUPOS.put("BANOS", "Accesibilidad: baños");
        GRUPOS.put("ACCESIBILIDAD COMUNICACIONAL", "Accesibilidad: comunicacional");
        GRUPOS.put("SEGURIDAD", "Seguridad");
        GRUPOS.put("SELECCIONE SEGUN CORRESPONDA", "Seguridad");
        GRUPOS.put("SUSTENTAB", "Sustentabilidad");
        GRUPOS.put("PRACTICAS", "Sustentabilidad");
        GRUPOS.put("SISTEMA DE INFORMACION", "Información y calidad");
        GRUPOS.put("INGRESO", "Accesibilidad: ingreso");
    }

    private Interprete() {
    }

    static List<Registro> interpretar(Hoja hoja, HojaConfig config) {
        Map<String, String> originales = originales(hoja);
        return switch (config.tipo()) {
            case SALONES -> salones(hoja, config, originales);
            default -> hoja.filas().stream().map(f -> fila(hoja.nombre(), f, config, originales)).toList();
        };
    }

    // ------------------------------------------------------------------ una fila = un establecimiento

    private static Registro fila(String nombreHoja, Fila f, HojaConfig config, Map<String, String> originales) {
        Registro r = new Registro(nombreHoja, f.numero());
        Columnas c = new Columnas(f, originales);
        r.categoria = config.categoria();
        r.subcategoria = config.subcategoria();

        switch (config.tipo()) {
            case GASTRONOMIA -> {
                r.nombre = c.tomar("TITULO", "NOMBRE");
                r.subcategoria = capitalizar(c.tomar("CATEGORIA"));
                for (String cocina : Texto.lista(c.tomar("SUBTITULO"))) {
                    r.agregarCaracteristica("Tipo de cocina", cocina, null);
                }
                r.agregarDescripcion(null, c.tomar("COMENTARIO"));
            }
            case ALOJAMIENTO -> alojamiento(r, c);
            default -> r.nombre = c.tomar("NOMBRE");
        }
        comunes(r, c);
        resto(r, c, config.tipo() == Perfil.Tipo.ALOJAMIENTO ? "Otros" : null);
        validar(r);
        return r;
    }

    private static void alojamiento(Registro r, Columnas c) {
        r.nombre = c.tomar("NOMBRE DEL ALOJAM");
        r.anioInauguracion = Texto.anio(c.tomar("ANO DE INA")).orElse(null);
        String tipo = c.tomar("SELECCIONE EL TIPO DE ALOJAMIENTO", "TIPO DE ALOJAMIENTO");
        Short estrellas = null;
        if (tipo != null) {
            String t = Texto.clave(tipo);
            Matcher m = ESTRELLAS.matcher(tipo);
            if (m.find()) {
                estrellas = Short.valueOf(m.group(1));
            }
            if (t.contains("apart")) {
                r.subcategoria = "Apart hotel";
            } else if (t.contains("hostel")) {
                r.subcategoria = "Hostel";
            } else if (t.contains("hotel")) {
                r.subcategoria = "Hotel";
            } else if (t.contains("complejo") || t.contains("delegac")) {
                r.subcategoria = "Delegación y complejo social";
            } else {
                r.subcategoria = capitalizar(tipo);
            }
        }
        Integer habitaciones = Texto.entero(c.tomar("CANTIDAD DE HABITACIONES ACCESIBLES")).orElse(null);
        Integer plazasAccesibles = Texto.entero(c.tomar("CANTIDAD DE PLAZAS ACCESIBLES")).orElse(null);
        Integer cantHabitaciones = Texto.entero(c.tomar("CANTIDAD DE HABITACIONES")).orElse(null);
        Integer plazas = Texto.entero(c.tomar("TOTAL DE CANTIDAD DE PLAZAS", "TOTAL")).orElse(null);
        Integer permanente = Texto.entero(c.tomarSi(h -> h.contains("PERSONAL") && h.contains("PERMANENTE")
                && !h.contains("NO PERMANENTE"))).orElse(null);
        Integer eventual = Texto.entero(c.tomarSi(h -> h.contains("PERSONAL")
                && (h.contains("EVENTUAL") || h.contains("NO PERMANENTE")))).orElse(null);
        r.alojamiento = new Registro.Alojamiento(estrellas, cantHabitaciones, plazas, habitaciones, plazasAccesibles,
                permanente, eventual);

        String referente = c.tomar("NOMBRE DEL REFERENTE");
        String telReferente = Texto.numeroComoTexto(c.tomar("TEL DEL REFERENTE"));
        String correoReferente = c.tomar("CORREO ELECTRONICO DEL REFERENTE");
        if (referente != null || telReferente != null || correoReferente != null) {
            r.referentes.add(new Registro.Referente(referente != null ? referente : "Referente", telReferente,
                    primerCorreo(correoReferente)));
        }
        r.agregarDescripcion("Reformas en los últimos 4 años", noVacioNiNo(c.tomar("REFORMAS REALIZADAS")));
        r.agregarDescripcion("Salones", c.tomar("SI SELECCIONO SALA"));
        r.agregarDescripcion("Líneas de colectivo cercanas", c.tomar("SI SELECCIONO SI DETALLAR CUALES SON LAS LINEAS"));
        if (Texto.afirmativo(c.tomar("ARRIBO DE TRANSPORTE"))) {
            r.agregarCaracteristica("Entorno", "Transporte público a menos de 200 m", null);
        }
        if (Texto.afirmativo(c.tomar("CICLOVIAS"))) {
            r.agregarCaracteristica("Entorno", "Ciclovía o bicisenda a menos de 200 m", null);
        }
    }

    // ------------------------------------------------------------------ salones: varias filas por establecimiento

    private static List<Registro> salones(Hoja hoja, HojaConfig config, Map<String, String> originales) {
        Map<String, Registro> porEstablecimiento = new LinkedHashMap<>();
        for (Fila f : hoja.filas()) {
            Columnas c = new Columnas(f, originales);
            String nombre = c.tomar("NOMBRE DE SALA", "NOMBRE");
            String direccion = Texto.repararFecha(c.tomar("DIRECCION"));
            String clave = Texto.clave(nombre) + "|" + Texto.clave(direccion);
            Registro r = porEstablecimiento.get(clave);
            boolean nuevo = r == null;
            if (nuevo) {
                r = new Registro(hoja.nombre(), f.numero());
                r.nombre = nombre;
                r.direccion = direccion;
                r.categoria = config.categoria();
                r.subcategoria = capitalizar(c.tomar("CATEGORIZACION"));
                r.anioInauguracion = Texto.anio(c.tomar("ANO DE INAUGURACION")).orElse(null);
                porEstablecimiento.put(clave, r);
            } else {
                c.tomar("CATEGORIZACION", "ANO DE INAUGURACION");
            }
            String responsable = c.tomar("RESPONSABLE");
            String contacto = Texto.numeroComoTexto(c.tomar("CONTACTO"));
            if (nuevo && (responsable != null || contacto != null)) {
                boolean esCorreo = contacto != null && contacto.contains("@");
                r.referentes.add(new Registro.Referente(responsable != null ? responsable : "Responsable",
                        esCorreo ? null : contacto, esCorreo ? primerCorreo(contacto) : null));
            }
            String remodelaciones = noVacioNiNo(c.tomar("REMODELACIONES"));
            String cuales = c.tomar("CUALES");
            if (nuevo && remodelaciones != null) {
                r.agregarDescripcion("Remodelaciones en los últimos 3 años", cuales != null ? cuales : remodelaciones);
            }
            String espacio = c.tomar("ESPACIO");
            Integer auditorio = Texto.entero(c.tomar("AUDITORIO")).orElse(null);
            Integer banquete = Texto.entero(c.tomar("BANQUETE")).orElse(null);
            Integer coctel = Texto.entero(c.tomar("COCTEL")).orElse(null);
            BigDecimal m2 = decimal(c.tomar("M2"));
            if (espacio != null || auditorio != null || banquete != null || coctel != null || m2 != null) {
                r.salones.add(new Registro.Salon(espacio != null ? espacio : "Salón " + (r.salones.size() + 1),
                        auditorio, banquete, coctel, m2));
            }
            if (nuevo) {
                comunes(r, c);
            } else {
                // los datos comunes ya vienen de la primera fila; de las siguientes solo se suman contactos nuevos
                Registro repetida = new Registro(hoja.nombre(), f.numero());
                comunes(repetida, c);
                for (Registro.Contacto k : repetida.contactos) {
                    agregarContacto(r, k.tipo(), k.valor(), k.uso());
                }
            }
            resto(r, c, "Equipamiento");
        }
        porEstablecimiento.values().forEach(Interprete::validar);
        return new ArrayList<>(porEstablecimiento.values());
    }

    // ------------------------------------------------------------------ columnas comunes a todas las planillas

    private static void comunes(Registro r, Columnas c) {
        if (r.direccion == null) {
            r.direccion = c.tomar("DIRECCION", "UBICACION", "PUNTO DE PARTIDA");
        }
        r.direccion = Texto.repararFecha(r.direccion);
        if (r.fechaRelevamiento == null) {
            r.fechaRelevamiento = fecha(c.tomar("FECHA DEL RELEVAMIENTO", "FECHA DE RELEVAMIENTO"));
        }
        r.estadoFuncionamiento = capitalizar(c.tomar("FUNCIONAMIENTO"));
        String dominio = c.tomar("DOMINIO");
        if (dominio != null) {
            r.agregarCaracteristica("Dominio", capitalizar(dominio), null);
        }

        telefonos(r, "Celular", c.tomar("TEL MOVIL"));
        telefonos(r, "Teléfono", c.tomar("TEL FIJO", "TELEFONO FIJO", "TELEFONO", "TEL"));
        telefonos(r, "WhatsApp", c.tomar("WHATSAPP"));
        for (String correo : Texto.valoresContacto(c.tomar("CORREO ELECTRONICO", "CORREO"))) {
            agregarContacto(r, "Correo", correo.replaceAll("\\s+", ""), null);
        }
        contactos(r, "Web", c.tomar("WEB", "PAGINA WEB"), null);
        String instagram = Texto.usuarioInstagram(c.tomar("INSTAGRAM"));
        if (instagram != null) {
            agregarContacto(r, "Instagram", instagram, null);
        }
        contactos(r, "Facebook", c.tomar("FACEBOOK"), null);
        contactos(r, "Otro", c.tomar("OTRA"), null);

        String reserva = c.tomar("LINK DE RESERVA", "RESERVAS", "TICKETS");
        if (reserva != null) {
            if (URL.matcher(reserva.trim()).matches()) {
                agregarContacto(r, "Web", reserva.trim(), "Reservas");
            } else {
                r.acceso = unir(r.acceso, reserva);
            }
        }
        r.acceso = unir(r.acceso, c.tomar("ENTRADA", "ACCESO"));
        r.visitaGuiada = noVacioNiNo(c.tomar("VISITAS GUIADAS"));
        r.agregarDescripcion(null, c.tomar("DESCRIPCION", "DESCRICION"));
    }

    /**
     * Lo que no se reconoció. Si {@code grupoSi} no es null (fichas técnicas), las listas conocidas y los "Sí"
     * pasan a características ("Sí" → grupo {@code grupoSi}); el resto de los textos, a la descripción.
     */
    private static void resto(Registro r, Columnas c, String grupoSi) {
        boolean comoCaracteristicas = grupoSi != null;
        for (Map.Entry<String, String> e : c.pendientes().entrySet()) {
            String clave = e.getKey();
            String valor = Texto.limpio(e.getValue());
            if (valor == null || IGNORADAS.stream().anyMatch(clave::startsWith) || clave.startsWith("COLUMNA ")) {
                continue;
            }
            String etiqueta = c.original(clave);
            String grupo = GRUPOS.entrySet().stream().filter(g -> clave.startsWith(g.getKey()) || clave.contains(g.getKey()))
                    .map(Map.Entry::getValue).findFirst().orElse(null);
            if (comoCaracteristicas && grupo != null) {
                Texto.lista(valor).stream()
                        .filter(v -> !Set.of("no", "otro", "otros", "otra", "otras").contains(Texto.clave(v)))
                        .forEach(v -> r.agregarCaracteristica(grupo, v.replaceAll("[:\\s]+$", ""), null));
            } else if (comoCaracteristicas && Texto.afirmativo(valor)) {
                r.agregarCaracteristica(grupoSi, etiqueta, null);
            } else if (comoCaracteristicas && Texto.clave(valor).equals("no")) {
                // "No" no aporta
            } else {
                r.agregarDescripcion(etiqueta, valor);
            }
        }
    }

    private static void validar(Registro r) {
        if (r.nombre == null || r.nombre.isBlank()) {
            r.errores.add("Falta el nombre");
        } else if (r.nombre.length() > 300) {
            r.errores.add("El nombre supera 300 caracteres");
        }
        if (r.direccion == null) {
            r.advertencias.add("Sin dirección: no se podrá ubicar en el mapa hasta cargarla");
        }
        if (r.subcategoria == null) {
            r.advertencias.add("Sin subcategoría: se asigna a la categoría '" + r.categoria + "'");
        }
        if (r.contactos.isEmpty()) {
            r.advertencias.add("Sin contactos");
        }
    }

    // ------------------------------------------------------------------ auxiliares

    private static void contactos(Registro r, String tipo, String valor, String uso) {
        for (String v : Texto.valoresContacto(valor)) {
            agregarContacto(r, tipo, v, uso);
        }
    }

    private static void telefonos(Registro r, String tipo, String valor) {
        for (String v : Texto.valoresTelefono(valor)) {
            agregarContacto(r, tipo, v, null);
        }
    }

    private static void agregarContacto(Registro r, String tipo, String valor, String uso) {
        String v = Texto.recortar(valor.trim(), 500);
        boolean repetido = r.contactos.stream().anyMatch(c -> c.tipo().equals(tipo) && Texto.clave(c.valor()).equals(Texto.clave(v)));
        if (!repetido) {
            r.contactos.add(new Registro.Contacto(tipo, v, uso));
        }
    }

    private static String primerCorreo(String s) {
        return Texto.valoresContacto(s).stream().filter(v -> v.contains("@")).map(v -> v.replaceAll("\\s+", ""))
                .findFirst().orElse(null);
    }

    private static String noVacioNiNo(String s) {
        return s == null || Texto.clave(s).equals("no") ? null : s;
    }

    private static String unir(String a, String b) {
        if (b == null) {
            return a;
        }
        return a == null ? b : a + " · " + b;
    }

    static String capitalizar(String s) {
        String t = Texto.limpio(s);
        if (t == null) {
            return null;
        }
        String minus = t.equals(t.toUpperCase(Locale.ROOT)) ? t.toLowerCase(Locale.ROOT) : t;
        return Character.toUpperCase(minus.charAt(0)) + minus.substring(1);
    }

    /** Fecha ISO (de una celda con formato fecha) o número de serie de Excel (45904 → 2025-09-03). */
    static LocalDate fecha(String s) {
        String t = Texto.limpio(s);
        if (t == null) {
            return null;
        }
        try {
            if (t.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(t);
            }
            if (t.matches("\\d{5}(\\.\\d+)?")) {
                return LocalDate.of(1899, 12, 30).plusDays((long) Double.parseDouble(t));
            }
        } catch (RuntimeException e) {
            return null;
        }
        return null;
    }

    private static BigDecimal decimal(String s) {
        String t = Texto.limpio(Texto.numeroComoTexto(s));
        if (t == null) {
            return null;
        }
        Matcher m = Pattern.compile("\\d+([.,]\\d+)?").matcher(t);
        return m.find() ? new BigDecimal(m.group().replace(',', '.')) : null;
    }

    private static Map<String, String> originales(Hoja hoja) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String original : hoja.encabezados()) {
            String primera = original.split("\\n")[0].trim().replaceAll("^[*\\s]+", "").replaceAll("[:*.\\s]+$", "").trim();
            m.putIfAbsent(Texto.encabezado(original), primera.isEmpty() ? original : primera);
        }
        return m;
    }

    /** Columnas de una fila; recuerda cuáles ya se usaron para que el resto no se repita. */
    private static final class Columnas {

        private final Fila fila;
        private final Map<String, String> originales;
        private final Set<String> usadas = new HashSet<>();

        Columnas(Fila fila, Map<String, String> originales) {
            this.fila = fila;
            this.originales = originales;
        }

        /** Primer valor no vacío de la primera columna cuyo encabezado empieza con alguno de los prefijos. */
        String tomar(String... prefijos) {
            for (String p : prefijos) {
                String clave = Texto.encabezado(p);
                for (Map.Entry<String, String> e : fila.valores().entrySet()) {
                    String k = e.getKey();
                    if (usadas.contains(k) || !(k.equals(clave) || k.startsWith(clave + " ") || k.startsWith(clave))) {
                        continue;
                    }
                    if ((!clave.contains("REFERENTE") && k.contains("REFERENTE"))
                            || IGNORADAS.stream().anyMatch(k::startsWith)) {
                        continue;
                    }
                    usadas.add(k);
                    String v = Texto.limpio(e.getValue());
                    if (v != null) {
                        return v;
                    }
                }
            }
            return null;
        }

        String tomarSi(java.util.function.Predicate<String> condicion) {
            for (Map.Entry<String, String> e : fila.valores().entrySet()) {
                if (!usadas.contains(e.getKey()) && condicion.test(e.getKey())) {
                    usadas.add(e.getKey());
                    return Texto.limpio(e.getValue());
                }
            }
            return null;
        }

        Map<String, String> pendientes() {
            Map<String, String> r = new LinkedHashMap<>();
            fila.valores().forEach((k, v) -> {
                if (!usadas.contains(k)) {
                    r.put(k, v);
                }
            });
            return r;
        }

        String original(String clave) {
            String base = clave.replaceAll("( BIS)+$", "");
            return originales.getOrDefault(base, base);
        }
    }
}
