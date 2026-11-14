package ar.sf.sicat.admin.importacion.geo;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;

import ar.sf.sicat.core.comun.Coordenadas;

/**
 * Geocodificación con Nominatim (OpenStreetMap). Respeta su política de uso: User-Agent identificable y
 * como máximo una consulta por segundo (el ritmo lo controla {@link GeocodificacionService}).
 * Busca dentro de un rectángulo alrededor de la ciudad de Santa Fe para evitar homónimos de otras ciudades.
 */
@Component
public class NominatimGeocodificador implements Geocodificador {

    /**
     * Ciudad de Santa Fe, Santo Tomé y la costa (Colastiné, Rincón): minLng,maxLat,maxLng,minLat (formato
     * viewbox de Nominatim). Deja afuera Recreo y Esperanza, que tienen calles con los mismos nombres.
     */
    private static final String VIEWBOX = "-60.82,-31.52,-60.55,-31.74";

    private final RestClient http;

    public NominatimGeocodificador(@Value("${sicat.geocodificacion.url:https://nominatim.openstreetmap.org}") String url,
                                   @Value("${sicat.geocodificacion.user-agent:SICAT-SF/0.1 (Municipalidad de Santa Fe)}")
                                   String userAgent) {
        this.http = RestClient.builder().baseUrl(url).defaultHeader("User-Agent", userAgent).build();
    }

    @Override
    public Optional<Coordenadas> buscar(String direccion, String localidad, String provincia) {
        String consulta = String.join(", ", direccion.replace('\n', ' '), localidad, provincia, "Argentina");
        try {
            JsonNode r = http.get()
                    .uri(u -> u.path("/search")
                            .queryParam("q", consulta)
                            .queryParam("format", "jsonv2")
                            .queryParam("limit", 1)
                            .queryParam("countrycodes", "ar")
                            .queryParam("viewbox", VIEWBOX)
                            .queryParam("bounded", 1)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            if (r == null || !r.isArray() || r.isEmpty()) {
                return Optional.empty();
            }
            JsonNode primero = r.get(0);
            return Optional.of(new Coordenadas(primero.path("lat").asDouble(), primero.path("lon").asDouble()));
        } catch (RestClientException e) {
            throw new IllegalStateException("Nominatim no respondió: " + e.getMessage(), e);
        }
    }
}
