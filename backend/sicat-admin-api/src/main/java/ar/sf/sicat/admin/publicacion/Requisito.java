package ar.sf.sicat.admin.publicacion;

/**
 * Requisitos para publicar (ADR 0008). El texto corto es para listados y tooltips ("Falta: una oferta activa");
 * el mensaje largo, con nombres, lo arma {@link PublicacionService}.
 */
public enum Requisito {
    ESTABLECIMIENTO_ACTIVO("establecimiento activo"),
    OFERENTE_ACTIVO("oferente activo"),
    LUGAR("un lugar asignado"),
    COORDENADAS("coordenadas del lugar"),
    UBICACION_VERIFICADA("ubicación sin verificar"),
    OFERTA_ACTIVA("una oferta activa");

    private final String corto;

    Requisito(String corto) {
        this.corto = corto;
    }

    public String corto() {
        return corto;
    }
}
