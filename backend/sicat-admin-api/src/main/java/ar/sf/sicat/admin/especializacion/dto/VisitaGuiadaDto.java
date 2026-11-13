package ar.sf.sicat.admin.especializacion.dto;

import java.util.List;

import ar.sf.sicat.admin.oferta.dto.HorarioDto;

public record VisitaGuiadaDto(
        Long id,
        Long ofertaId,
        String modalidad,
        boolean requiereReserva,
        boolean habilitada,
        List<HorarioDto> horarios) {
}
