package ar.sf.sicat.core.comun;

/**
 * Rectángulo geográfico para filtros {@code ?bbox=minLng,minLat,maxLng,maxLat} (orden de GeoJSON / Leaflet
 * {@code map.getBounds().toBBoxString()}).
 */
public record Bbox(double minLng, double minLat, double maxLng, double maxLat) {

    public static Bbox parse(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String[] partes = texto.split(",");
        if (partes.length != 4) {
            throw new DatoInvalidoException("bbox debe tener 4 valores: minLng,minLat,maxLng,maxLat");
        }
        try {
            Bbox b = new Bbox(Double.parseDouble(partes[0].trim()), Double.parseDouble(partes[1].trim()),
                    Double.parseDouble(partes[2].trim()), Double.parseDouble(partes[3].trim()));
            if (b.minLng < -180 || b.maxLng > 180 || b.minLat < -90 || b.maxLat > 90
                    || b.minLng > b.maxLng || b.minLat > b.maxLat) {
                throw new DatoInvalidoException("bbox fuera de rango o con mínimos mayores que máximos");
            }
            return b;
        } catch (NumberFormatException e) {
            throw new DatoInvalidoException("bbox debe contener números");
        }
    }
}
