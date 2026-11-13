package ar.sf.sicat.admin.contacto.dto;

public record CanalContactoDto(
        Long id,
        Long tipoCanalId,
        String tipoCanal,
        String valor,
        String uso,
        boolean esPublico,
        Short orden) {
}
