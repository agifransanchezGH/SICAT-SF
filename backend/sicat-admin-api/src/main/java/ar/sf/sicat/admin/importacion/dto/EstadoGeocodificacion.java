package ar.sf.sicat.admin.importacion.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EstadoGeocodificacion(
        boolean enCurso,
        @Schema(description = "Lugares a procesar en esta corrida") int total,
        int procesados,
        @Schema(description = "Ubicados con su número (AUTOMATICO)") int encontrados,
        @Schema(description = "Ubicados en la calle o zona, sin el número (APROXIMADO)") int aproximados,
        int noEncontrados,
        @Schema(description = "Lugares que siguen PENDIENTE en la base") Long pendientes,
        OffsetDateTime inicio,
        OffsetDateTime fin,
        String error) {

    public static EstadoGeocodificacion inicial() {
        return new EstadoGeocodificacion(false, 0, 0, 0, 0, 0, null, null, null, null);
    }

    public EstadoGeocodificacion conPendientes(long n) {
        return new EstadoGeocodificacion(enCurso, total, procesados, encontrados, aproximados, noEncontrados, n,
                inicio, fin, error);
    }
}
