package ar.sf.sicat.admin.importacion.geo;

import java.util.Optional;

import ar.sf.sicat.core.comun.Coordenadas;

/** Convierte una dirección en coordenadas. La implementación por defecto usa Nominatim (OpenStreetMap). */
public interface Geocodificador {

    /**
     * @param direccion texto completo de la dirección (como vino en la planilla)
     * @param calle     calle, si se pudo separar (puede ser null)
     * @param altura    número, si se pudo separar (puede ser null)
     */
    record Consulta(String direccion, String calle, Integer altura, String localidad, String provincia) {
    }

    /**
     * @param aproximado true si el resultado es la calle o la zona y no el número pedido
     *                   (p. ej. un bulevar entero en un solo punto)
     */
    record Resultado(Coordenadas coordenadas, boolean aproximado) {
    }

    Optional<Resultado> buscar(Consulta consulta);
}
