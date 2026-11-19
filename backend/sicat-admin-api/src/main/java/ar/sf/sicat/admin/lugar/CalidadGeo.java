package ar.sf.sicat.admin.lugar;

import java.util.ArrayList;
import java.util.List;

import ar.sf.sicat.admin.lugar.dto.LugarDto;
import ar.sf.sicat.core.comun.Coordenadas;
import ar.sf.sicat.core.dominio.lugar.Lugar;

/**
 * Señal de calidad de una ubicación, para ordenar y revisar la cola. Se calcula con lo que ya está guardado (sin
 * cambio de esquema): el estado de la geocodificación (ADR 0010), si el punto lo comparten otros lugares (señal de
 * que OpenStreetMap ubicó la calle o la zona, no la puerta), la distancia al centro y si la dirección tiene altura.
 */
final class CalidadGeo {

    /** Plaza 25 de Mayo, igual que SANTA_FE en el frontend. */
    private static final double CENTRO_LAT = -31.6488;
    private static final double CENTRO_LNG = -60.7087;
    /** Santa Fe, Santo Tomé y la costa entran holgados en este radio. */
    static final double KM_LEJOS = 25;

    private CalidadGeo() {
    }

    /**
     * @param compartidos cuántos otros lugares tienen el mismo punto (a menos de un metro)
     */
    static LugarDto.Calidad de(Lugar l, Coordenadas c, long compartidos) {
        String estado = l.getEstadoGeo();
        if ("VERIFICADO".equals(estado) && c != null) {
            return new LugarDto.Calidad(LugarDto.Nivel.VERIFICADA, List.of("Confirmada por una persona"));
        }
        if (c == null) {
            String motivo = "NO_ENCONTRADO".equals(estado) ? "OpenStreetMap no encontró la dirección"
                    : "PENDIENTE".equals(estado) ? "Todavía no se buscó en OpenStreetMap" : "Sin coordenadas";
            return new LugarDto.Calidad(LugarDto.Nivel.BAJA, List.of(motivo));
        }
        List<String> motivos = new ArrayList<>();
        LugarDto.Nivel nivel;
        if ("APROXIMADO".equals(estado)) {
            nivel = LugarDto.Nivel.MEDIA;
            motivos.add("Se ubicó la calle o la zona, no el número");
        } else if ("AUTOMATICO".equals(estado)) {
            nivel = LugarDto.Nivel.ALTA;
            motivos.add("OpenStreetMap encontró la dirección con su número");
        } else {
            nivel = LugarDto.Nivel.MEDIA;
            motivos.add("Coordenadas cargadas sin verificar");
        }
        if (compartidos > 0) {
            nivel = bajar(nivel);
            motivos.add("Mismo punto que " + compartidos + (compartidos == 1 ? " lugar más" : " lugares más"));
        }
        double km = distanciaKm(c.lat(), c.lng(), CENTRO_LAT, CENTRO_LNG);
        if (km > KM_LEJOS) {
            nivel = LugarDto.Nivel.BAJA;
            motivos.add("A " + Math.round(km) + " km del centro de Santa Fe");
        }
        if (l.getCalle() != null && l.getAltura() == null && l.getKm() == null && l.getCalleCruce() == null) {
            motivos.add("La dirección no tiene altura");
        }
        return new LugarDto.Calidad(nivel, motivos);
    }

    private static LugarDto.Nivel bajar(LugarDto.Nivel n) {
        return n == LugarDto.Nivel.ALTA ? LugarDto.Nivel.MEDIA : LugarDto.Nivel.BAJA;
    }

    /** Distancia en km (haversine): alcanza para saber si un punto quedó en otra ciudad. */
    static double distanciaKm(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLng / 2), 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }
}
