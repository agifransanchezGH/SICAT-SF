package ar.sf.sicat.core.comun;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/** Utilidades geográficas. SRID 4326 (WGS84); en JTS x = longitud, y = latitud. */
public final class Geo {

    public static final int SRID = 4326;
    public static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), SRID);

    private Geo() {
    }

    public static Point punto(Coordenadas c) {
        if (c == null) {
            return null;
        }
        return FACTORY.createPoint(new Coordinate(c.lng(), c.lat()));
    }

    public static Coordenadas coordenadas(Point p) {
        if (p == null || p.isEmpty()) {
            return null;
        }
        return new Coordenadas(p.getY(), p.getX());
    }
}
