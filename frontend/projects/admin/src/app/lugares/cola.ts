import { InjectionToken } from '@angular/core';

/** Posición del lugar abierto dentro de la cola de la pantalla de Lugares (1-based). */
export interface PosicionEnCola {
  posicion: number;
  total: number;
}

/** Si el diálogo de lugar se abre desde una cola, recibe su posición y ofrece "Confirmar y siguiente". */
export const POSICION_EN_COLA = new InjectionToken<PosicionEnCola>('POSICION_EN_COLA');

/**
 * Siguiente elemento de una cola después de guardar el actual y recargar la página.
 * Al confirmar, el actual puede desaparecer de la cola (vista "A revisar") o quedarse (vista "Todos"), y entrar
 * al final un elemento que antes estaba en la página siguiente. Se elige, en orden:
 * 1. el primero que estaba después del actual y sigue en la página;
 * 2. si no hay, uno que no estaba antes (entró desde la página siguiente).
 * null: no queda nada en esta página (quien llama decide si pasar a la siguiente).
 */
export function elegirSiguiente<T extends { id: number }>(
  anterior: readonly T[],
  nueva: readonly T[],
  actualId: number,
): T | null {
  const i = anterior.findIndex((x) => x.id === actualId);
  const antes = new Set(anterior.slice(0, Math.max(i, 0)).map((x) => x.id));
  const despues = new Set(anterior.slice(i + 1).map((x) => x.id));
  return (
    nueva.find((x) => x.id !== actualId && despues.has(x.id)) ??
    nueva.find((x) => x.id !== actualId && !antes.has(x.id) && !despues.has(x.id)) ??
    null
  );
}
