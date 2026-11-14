package ar.sf.sicat.core.comun;

import java.math.BigDecimal;

/** Arma una dirección legible: "Calle 123", "Ruta 1 km 5.5" o "Calle y Cruce", más el complemento. */
public final class Direcciones {

    private Direcciones() {
    }

    public static String formatear(String calle, Integer altura, BigDecimal km, String calleCruce, String complemento) {
        StringBuilder sb = new StringBuilder();
        if (calle != null) {
            sb.append(calle);
            if (altura != null) {
                sb.append(' ').append(altura);
            } else if (km != null) {
                sb.append(" km ").append(km.stripTrailingZeros().toPlainString());
            } else if (calleCruce != null) {
                sb.append(" y ").append(calleCruce);
            }
        }
        if (complemento != null) {
            sb.append(sb.isEmpty() ? "" : ", ").append(complemento);
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
