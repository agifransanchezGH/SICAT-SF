package ar.sf.sicat.publica.establecimiento.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import ar.sf.sicat.core.comun.Coordenadas;
import io.swagger.v3.oas.annotations.media.Schema;

/** Ficha pública de un establecimiento (ARQUITECTURA.md §9). Solo contiene datos de las vistas públicas. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FichaDto(
        Long id,
        String nombre,
        String descripcion,
        String oferente,
        String direccion,
        String lugar,
        String localidad,
        String provincia,
        Coordenadas coordenadas,
        Short anioInauguracion,
        @Schema(description = "p. ej. 'Cerrado temporalmente'") String estadoFuncionamiento,
        String motivoEstado,
        LocalDate fechaReapertura,
        boolean destacado,
        @Schema(description = "Canales públicos, incluidas redes sociales") List<Contacto> contactos,
        List<Caracteristica> caracteristicas,
        List<Distincion> distinciones,
        List<Oferta> ofertas) implements Serializable {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Contacto(@Schema(example = "Instagram") String tipo, String valor, String uso) implements Serializable {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Caracteristica(String grupo, String nombre, Integer cantidad) implements Serializable {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Distincion(String nombre, String organismoOtorgante, Short anio, String urlVerificacion)
            implements Serializable {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Oferta(
            Long id,
            String categoria,
            List<String> categoriasAdicionales,
            String acceso,
            String publicoAdmitido,
            String frecuencia,
            List<Horario> horarios,
            @Schema(description = "Datos públicos de la especialización: alojamiento, circuito, salones, visitasGuiadas")
            Map<String, Object> detalle) implements Serializable {
    }

    public record Horario(
            @Schema(description = "1 = lunes … 7 = domingo") short diaSemana,
            @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "09:00") LocalTime desde,
            @JsonFormat(pattern = "HH:mm") @Schema(type = "string", example = "18:00") LocalTime hasta)
            implements Serializable {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Alojamiento(Short estrellas, Integer cantHabitaciones, Integer totalPlazas, Integer habAccesibles,
                              Integer plazasAccesibles) implements Serializable {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Salon(String nombre, Integer capAuditorio, Integer capBanquete, Integer capCoctel, BigDecimal m2)
            implements Serializable {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record VisitaGuiada(String modalidad, boolean requiereReserva, List<Horario> horarios) implements Serializable {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Circuito(Integer duracionMin, String lugarPartida, Coordenadas partida,
                           @Schema(description = "GeoJSON LineString") Object trazado,
                           List<Parada> paradas) implements Serializable {
    }

    public record Parada(short orden, Long establecimientoId, String nombre, Coordenadas coordenadas)
            implements Serializable {
    }
}
