package ar.sf.sicat.admin.catalogo;

import java.util.Arrays;

import ar.sf.sicat.core.comun.EntidadNoEncontradaException;

/** Catálogos simples administrables por {@code /catalogos/{nombre}}. */
public enum TipoCatalogo {
    TIPO_OFERENTE("tipo-oferente"),
    TIPO_CANAL("tipo-canal"),
    TIPO_LUGAR("tipo-lugar"),
    LOCALIDAD("localidad"),
    DISTINCION("distincion"),
    CARACTERISTICA("caracteristica");

    private final String ruta;

    TipoCatalogo(String ruta) {
        this.ruta = ruta;
    }

    public String ruta() {
        return ruta;
    }

    public static TipoCatalogo desdeRuta(String ruta) {
        return Arrays.stream(values())
                .filter(t -> t.ruta.equalsIgnoreCase(ruta))
                .findFirst()
                .orElseThrow(() -> new EntidadNoEncontradaException("Catálogo", ruta));
    }
}
