package ar.sf.sicat.admin.especializacion.dto;

import java.math.BigDecimal;

public record SalonDto(
        Long id,
        Long ofertaId,
        String nombre,
        Integer capAuditorio,
        Integer capBanquete,
        Integer capCoctel,
        BigDecimal m2,
        String estadoConservacion,
        String estadoFuncionamiento,
        boolean activo) {
}
