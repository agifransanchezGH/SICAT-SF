package ar.sf.sicat.core.comun;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Conversión mínima entre geometrías JTS (SRID 4326) y GeoJSON (RFC 7946):
 * Point, LineString, Polygon, MultiPoint, MultiLineString y MultiPolygon.
 * Coordenadas en orden [lng, lat].
 */
public final class GeoJson {

    private static final JsonNodeFactory NODOS = JsonNodeFactory.instance;

    private GeoJson() {
    }

    // ------------------------------------------------------------------ JTS -> GeoJSON

    public static ObjectNode aGeoJson(Geometry g) {
        if (g == null) {
            return null;
        }
        ObjectNode n = NODOS.objectNode();
        n.put("type", g.getGeometryType());
        switch (g) {
            case Point p -> n.set("coordinates", posicion(p.getCoordinate()));
            case LineString l -> n.set("coordinates", posiciones(l.getCoordinates()));
            case Polygon p -> n.set("coordinates", anillos(p));
            case MultiPoint m -> n.set("coordinates", posiciones(m.getCoordinates()));
            case MultiLineString m -> {
                ArrayNode lineas = NODOS.arrayNode();
                for (int i = 0; i < m.getNumGeometries(); i++) {
                    lineas.add(posiciones(m.getGeometryN(i).getCoordinates()));
                }
                n.set("coordinates", lineas);
            }
            case MultiPolygon m -> {
                ArrayNode poligonos = NODOS.arrayNode();
                for (int i = 0; i < m.getNumGeometries(); i++) {
                    poligonos.add(anillos((Polygon) m.getGeometryN(i)));
                }
                n.set("coordinates", poligonos);
            }
            default -> throw new DatoInvalidoException("Tipo de geometría no soportado: " + g.getGeometryType());
        }
        return n;
    }

    private static ArrayNode posicion(Coordinate c) {
        return NODOS.arrayNode().add(c.x).add(c.y);
    }

    private static ArrayNode posiciones(Coordinate[] cs) {
        ArrayNode a = NODOS.arrayNode();
        for (Coordinate c : cs) {
            a.add(posicion(c));
        }
        return a;
    }

    private static ArrayNode anillos(Polygon p) {
        ArrayNode a = NODOS.arrayNode();
        a.add(posiciones(p.getExteriorRing().getCoordinates()));
        for (int i = 0; i < p.getNumInteriorRing(); i++) {
            a.add(posiciones(p.getInteriorRingN(i).getCoordinates()));
        }
        return a;
    }

    // ------------------------------------------------------------------ GeoJSON -> JTS

    /** Lanza {@link DatoInvalidoException} (HTTP 400) si el GeoJSON no es válido. */
    public static Geometry aGeometria(JsonNode n) {
        if (n == null || n.isNull()) {
            return null;
        }
        String tipo = n.path("type").asText("");
        JsonNode c = n.path("coordinates");
        try {
            Geometry g = switch (tipo) {
                case "Point" -> Geo.FACTORY.createPoint(coordenada(c));
                case "LineString" -> Geo.FACTORY.createLineString(coordenadas(c));
                case "Polygon" -> poligono(c);
                case "MultiPoint" -> Geo.FACTORY.createMultiPointFromCoords(coordenadas(c));
                case "MultiLineString" -> {
                    LineString[] lineas = new LineString[c.size()];
                    for (int i = 0; i < c.size(); i++) {
                        lineas[i] = Geo.FACTORY.createLineString(coordenadas(c.get(i)));
                    }
                    yield Geo.FACTORY.createMultiLineString(lineas);
                }
                case "MultiPolygon" -> {
                    Polygon[] poligonos = new Polygon[c.size()];
                    for (int i = 0; i < c.size(); i++) {
                        poligonos[i] = poligono(c.get(i));
                    }
                    yield Geo.FACTORY.createMultiPolygon(poligonos);
                }
                default -> throw new DatoInvalidoException("GeoJSON: tipo de geometría no soportado: '" + tipo + "'");
            };
            if (!g.isValid()) {
                throw new DatoInvalidoException("GeoJSON: la geometría no es válida");
            }
            return g;
        } catch (IllegalArgumentException e) {
            throw new DatoInvalidoException("GeoJSON inválido: " + e.getMessage());
        }
    }

    /** Exige un tipo concreto (p. ej. LineString para el trazado de un circuito). */
    public static <T extends Geometry> T aGeometria(JsonNode n, Class<T> tipo) {
        Geometry g = aGeometria(n);
        if (g != null && !tipo.isInstance(g)) {
            throw new DatoInvalidoException("Se esperaba una geometría " + tipo.getSimpleName());
        }
        return tipo.cast(g);
    }

    private static Polygon poligono(JsonNode anillos) {
        LinearRing exterior = Geo.FACTORY.createLinearRing(coordenadas(anillos.get(0)));
        LinearRing[] huecos = new LinearRing[Math.max(0, anillos.size() - 1)];
        for (int i = 1; i < anillos.size(); i++) {
            huecos[i - 1] = Geo.FACTORY.createLinearRing(coordenadas(anillos.get(i)));
        }
        return Geo.FACTORY.createPolygon(exterior, huecos);
    }

    private static Coordinate[] coordenadas(JsonNode a) {
        if (a == null || !a.isArray()) {
            throw new DatoInvalidoException("GeoJSON: se esperaba una lista de posiciones");
        }
        Coordinate[] cs = new Coordinate[a.size()];
        for (int i = 0; i < a.size(); i++) {
            cs[i] = coordenada(a.get(i));
        }
        return cs;
    }

    private static Coordinate coordenada(JsonNode p) {
        if (p == null || !p.isArray() || p.size() < 2 || !p.get(0).isNumber() || !p.get(1).isNumber()) {
            throw new DatoInvalidoException("GeoJSON: posición inválida, se espera [lng, lat]");
        }
        double lng = p.get(0).asDouble();
        double lat = p.get(1).asDouble();
        if (lng < -180 || lng > 180 || lat < -90 || lat > 90) {
            throw new DatoInvalidoException("GeoJSON: posición fuera de rango [lng, lat]");
        }
        return new Coordinate(lng, lat);
    }
}
