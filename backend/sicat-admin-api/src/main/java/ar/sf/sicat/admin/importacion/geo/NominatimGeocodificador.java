package ar.sf.sicat.admin.importacion.geo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import ar.sf.sicat.core.comun.Coordenadas;

/**
 * Geocodificación con Nominatim (OpenStreetMap). Respeta su política de uso: User-Agent identificable y
 * como máximo una consulta por segundo (cada intento espera el intervalo configurado).
 * <p>
 * Intentos, en orden, hasta encontrar un resultado:
 * <ol>
 *   <li>búsqueda estructurada "número + calle" (con abreviaturas expandidas), que ubica mejor la altura;</li>
 *   <li>búsqueda libre con la dirección limpia y expandida;</li>
 *   <li>búsqueda libre con la dirección original.</li>
 * </ol>
 * Si se pidió un número y lo que devuelve es una calle o zona, el resultado se marca como aproximado.
 */
@Component
public class NominatimGeocodificador implements Geocodificador {

    /**
     * Ciudad de Santa Fe, Santo Tomé y la costa (Colastiné, Rincón): minLng,maxLat,maxLng,minLat (formato
     * viewbox de Nominatim). Deja afuera Recreo y Esperanza, que tienen calles con los mismos nombres.
     */
    private static final String VIEWBOX = "-60.82,-31.52,-60.55,-31.74";
    /** Tipos de resultado que indican que no se encontró el número exacto. */
    private static final Set<String> TIPOS_APROXIMADOS = Set.of("road", "street", "neighbourhood", "suburb",
            "quarter", "city", "town", "village", "municipality", "county", "state", "residential", "hamlet");

    private final RestClient http;
    private final long intervaloMs;
    private long ultimaConsulta;

    public NominatimGeocodificador(
            @Value("${sicat.geocodificacion.url:https://nominatim.openstreetmap.org}") String url,
            @Value("${sicat.geocodificacion.user-agent:SICAT-SF/0.1 (Municipalidad de Santa Fe)}") String userAgent,
            @Value("${sicat.geocodificacion.intervalo-ms:1100}") long intervaloMs) {
        this.http = RestClient.builder().baseUrl(url).defaultHeader("User-Agent", userAgent).build();
        this.intervaloMs = intervaloMs;
    }

    @Override
    public Optional<Resultado> buscar(Consulta c) {
        boolean conNumero = c.altura() != null;
        List<Map<String, String>> intentos = new ArrayList<>();
        if (c.calle() != null && c.altura() != null) {
            Map<String, String> estructurada = new LinkedHashMap<>();
            estructurada.put("street", c.altura() + " " + DireccionGeo.expandir(c.calle()));
            estructurada.put("city", c.localidad());
            estructurada.put("state", c.provincia());
            estructurada.put("country", "Argentina");
            intentos.add(estructurada);
        }
        String limpia = DireccionGeo.limpiar(c.direccion());
        intentos.add(Map.of("q", String.join(", ", limpia, c.localidad(), c.provincia(), "Argentina")));
        String original = c.direccion().replace('\n', ' ');
        if (!original.equalsIgnoreCase(limpia)) {
            intentos.add(Map.of("q", String.join(", ", original, c.localidad(), c.provincia(), "Argentina")));
        }

        Resultado aproximado = null;
        for (Map<String, String> parametros : intentos) {
            JsonNode r = consultar(parametros);
            if (r == null) {
                continue;
            }
            String tipo = r.path("addresstype").asText(r.path("type").asText(""));
            Resultado resultado = new Resultado(new Coordenadas(r.path("lat").asDouble(), r.path("lon").asDouble()),
                    conNumero && TIPOS_APROXIMADOS.contains(tipo));
            if (!resultado.aproximado()) {
                return Optional.of(resultado);
            }
            if (aproximado == null) {
                aproximado = resultado;
            }
        }
        return Optional.ofNullable(aproximado);
    }

    /** Primer resultado de Nominatim o null. Espera lo necesario para no superar una consulta por segundo. */
    private synchronized JsonNode consultar(Map<String, String> parametros) {
        esperarTurno();
        try {
            JsonNode r = http.get()
                    .uri(u -> {
                        UriBuilder b = u.path("/search");
                        parametros.forEach(b::queryParam);
                        return b.queryParam("format", "jsonv2")
                                .queryParam("limit", 1)
                                .queryParam("countrycodes", "ar")
                                .queryParam("viewbox", VIEWBOX)
                                .queryParam("bounded", 1)
                                .build();
                    })
                    .retrieve()
                    .body(JsonNode.class);
            return r != null && r.isArray() && !r.isEmpty() ? r.get(0) : null;
        } catch (RestClientException e) {
            throw new IllegalStateException("Nominatim no respondió: " + e.getMessage(), e);
        } finally {
            ultimaConsulta = System.currentTimeMillis();
        }
    }

    private void esperarTurno() {
        long espera = ultimaConsulta + intervaloMs - System.currentTimeMillis();
        if (espera > 0) {
            try {
                Thread.sleep(espera);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Geocodificación interrumpida", e);
            }
        }
    }
}
