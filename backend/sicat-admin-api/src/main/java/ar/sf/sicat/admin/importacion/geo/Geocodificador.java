package ar.sf.sicat.admin.importacion.geo;

import java.util.Optional;

import ar.sf.sicat.core.comun.Coordenadas;

/** Convierte una dirección en coordenadas. La implementación por defecto usa Nominatim (OpenStreetMap). */
public interface Geocodificador {

    Optional<Coordenadas> buscar(String direccion, String localidad, String provincia);
}
