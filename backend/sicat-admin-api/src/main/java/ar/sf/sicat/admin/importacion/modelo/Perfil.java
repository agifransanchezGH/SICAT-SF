package ar.sf.sicat.admin.importacion.modelo;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import ar.sf.sicat.admin.importacion.lectura.Texto;
import ar.sf.sicat.core.comun.EntidadNoEncontradaException;

/**
 * Planillas fuente que sabe importar el sistema ({@code /importacion/{archivo}}). Cada una indica, por hoja,
 * la categoría y subcategoría de destino (según el "Modelo de datos normalizado" de la Dirección de Turismo)
 * y cómo se interpretan sus columnas.
 */
public enum Perfil {

    GASTRONOMIA("gastronomia", "GASTRONOMIA.xlsx",
            hoja("*", "Gastronomía", null, Tipo.GASTRONOMIA)),
    ALOJAMIENTO("alojamiento", "HOTELES - ALOJAMIENTO P_ DELEGACIONES.xlsx",
            hoja("FICHA TECNICA DE HOTELES", "Alojamiento", "Hotel", Tipo.ALOJAMIENTO),
            hoja("FICHA TECNICA DE DELEGACIONES", "Alojamiento", "Delegación y complejo social", Tipo.ALOJAMIENTO)),
    MUSEOS("museos", "MUSEOS.xlsx",
            hoja("*", "Museos y salas culturales", "Museo", Tipo.GENERAL)),
    SALONES("salones", "SALAS Y SALONES PARA EVENTOS.xlsx",
            hoja("*", "Salas y salones para eventos", null, Tipo.SALONES)),
    IGLESIAS("iglesias", "IGLESIAS DESTACADAS.xlsx",
            hoja("*", "Patrimonio", "Iglesia", Tipo.GENERAL)),
    EDIFICIOS_HISTORICOS("edificios-historicos", "EDIFICIOS Y SITIOS HISTÓRICOS.xlsx",
            hoja("*", "Patrimonio", "Edificio y sitio histórico", Tipo.GENERAL)),
    ATRACTIVOS_NATURALES("atractivos-naturales", "ATRACTIVOS NATURALES.xlsx",
            hoja("*", "Patrimonio", "Atractivo natural", Tipo.GENERAL)),
    ESPACIOS_CULTURALES("espacios-culturales", "ESPACIOS CULTURALES.xlsx",
            hoja("*", "Patrimonio", "Espacio cultural", Tipo.GENERAL)),
    RECREACION("recreacion", "RECREACIÓN, DIVERSIÓN Y COMPRA.xlsx",
            hoja("ATRACTIVOS TURISTICOS DESTACADO", "Recreación, diversión y compra", "Atractivo turístico destacado", Tipo.GENERAL),
            hoja("SHOPPINGS", "Recreación, diversión y compra", "Shopping y paseo comercial", Tipo.GENERAL),
            hoja("FERIAS", "Recreación, diversión y compra", "Feria", Tipo.GENERAL),
            hoja("PRODUCTOS REGIONALES", "Recreación, diversión y compra", "Productos regionales y artesanías", Tipo.GENERAL),
            hoja("CASINO", "Recreación, diversión y compra", "Casino", Tipo.GENERAL),
            hoja("CINES", "Recreación, diversión y compra", "Cine", Tipo.GENERAL),
            hoja("BOWLINGS", "Recreación, diversión y compra", "Bowling", Tipo.GENERAL),
            hoja("OTRAS ACTIVIDADES", "Recreación, diversión y compra", "Otras actividades", Tipo.GENERAL)),
    SERVICIOS_TURISTICOS("servicios-turisticos", "SERVICIOS TURÍSTICOS - PRESTADORES.xlsx",
            hoja("AGENCIAS", "Servicios turísticos", "Agencia de turismo receptivo", Tipo.GENERAL),
            hoja("ALQUILER DE AUTOS", "Servicios turísticos", "Alquiler de autos", Tipo.GENERAL),
            hoja("TRANFERS", "Servicios turísticos", "Transfer", Tipo.GENERAL),
            hoja("TRANSFERS", "Servicios turísticos", "Transfer", Tipo.GENERAL),
            hoja("CASA DE CAMBIO", "Servicios turísticos", "Casa de cambio", Tipo.GENERAL),
            hoja("BUS TURISTICO", "Servicios turísticos", "Bus turístico", Tipo.GENERAL),
            hoja("RUTA DEL LISO", "Servicios turísticos", "Paseo guiado", Tipo.GENERAL),
            hoja("CATAMARAN", "Turismo náutico y deportivo", "Paseo náutico", Tipo.GENERAL),
            hoja("CLUBES DEPORTIVOS", "Turismo náutico y deportivo", "Club deportivo", Tipo.GENERAL),
            hoja("CLUBES NAUTICOS", "Turismo náutico y deportivo", "Club náutico", Tipo.GENERAL),
            hoja("TURISMO NAUTICO", "Turismo náutico y deportivo", "Turismo náutico", Tipo.GENERAL),
            hoja("GUARDERIAS NAUTICAS", "Turismo náutico y deportivo", "Guardería náutica y bajada de lanchas", Tipo.GENERAL),
            hoja("PARADORES", "Turismo náutico y deportivo", "Parador, playa y solárium", Tipo.GENERAL),
            hoja("ARMERIAS", "Turismo náutico y deportivo", "Armería y casa de pesca", Tipo.GENERAL));

    /** Cómo se interpretan las columnas de una hoja. */
    public enum Tipo {
        /** Columnas comunes (nombre, dirección, contactos, descripción, entrada, visitas...). */
        GENERAL,
        /** La columna "categoria" es la subcategoría y "subtitulo" el tipo de cocina. */
        GASTRONOMIA,
        /** Fichas técnicas de hoteles y delegaciones: capacidades, referente y listas como características. */
        ALOJAMIENTO,
        /** Una fila por espacio: se agrupan por establecimiento y cada fila es un salón. */
        SALONES
    }

    public record HojaConfig(String prefijo, String categoria, String subcategoria, Tipo tipo) {

        boolean aplica(String nombreHoja) {
            return prefijo.equals("*") || Texto.encabezado(nombreHoja).startsWith(prefijo);
        }
    }

    private final String ruta;
    private final String archivoEsperado;
    private final List<HojaConfig> hojas;

    Perfil(String ruta, String archivoEsperado, HojaConfig... hojas) {
        this.ruta = ruta;
        this.archivoEsperado = archivoEsperado;
        this.hojas = List.of(hojas);
    }

    private static HojaConfig hoja(String prefijo, String categoria, String subcategoria, Tipo tipo) {
        return new HojaConfig(prefijo, categoria, subcategoria, tipo);
    }

    public String ruta() {
        return ruta;
    }

    public String archivoEsperado() {
        return archivoEsperado;
    }

    public List<HojaConfig> hojas() {
        return hojas;
    }

    public Optional<HojaConfig> configDe(String nombreHoja) {
        return hojas.stream().filter(h -> h.aplica(nombreHoja)).findFirst();
    }

    public static Perfil desdeRuta(String ruta) {
        return Arrays.stream(values()).filter(p -> p.ruta.equalsIgnoreCase(ruta)).findFirst()
                .orElseThrow(() -> new EntidadNoEncontradaException("Perfil de importación", ruta));
    }
}
