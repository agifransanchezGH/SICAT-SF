import { EstablecimientoResumen } from '../core/modelos';

/** Botón de publicación de una fila del listado: qué hace, si se puede usar y, si no, por qué. */
export interface AccionRapida {
  accion: 'publicar' | 'despublicar';
  habilitada: boolean;
  /** "Falta: ubicación sin verificar, una oferta activa" (null si está habilitada). */
  motivo: string | null;
}

/**
 * Un publicado siempre se puede despublicar. Lo demás se puede publicar solo si no le falta ningún requisito
 * (ADR 0008); el motivo se arma con los textos cortos que manda la API, en el mismo orden.
 */
export function accionRapida(e: Pick<EstablecimientoResumen, 'estadoPublicacion' | 'faltantes'>): AccionRapida {
  if (e.estadoPublicacion === 'PUBLICADO') {
    return { accion: 'despublicar', habilitada: true, motivo: null };
  }
  const faltan = e.faltantes ?? [];
  return faltan.length
    ? { accion: 'publicar', habilitada: false, motivo: `Falta: ${faltan.map((f) => f.texto).join(', ')}` }
    : { accion: 'publicar', habilitada: true, motivo: null };
}
