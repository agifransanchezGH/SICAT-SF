import { DestroyRef, Injectable, inject } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';
import { Observable, map, of } from 'rxjs';

import { Confirmar } from './confirmar';

interface Fuente {
  etiqueta: string;
  hay: () => boolean;
}

/**
 * Registro de formularios con cambios sin guardar. Cada editor se anota con `avisarSiHayCambios` y, mientras
 * exista, la navegación (guard `sinCambiosPendientes`), la recarga o el cierre de la pestaña piden confirmación.
 */
@Injectable({ providedIn: 'root' })
export class CambiosPendientes {
  private readonly fuentes = new Set<Fuente>();

  constructor() {
    // Recargar o cerrar la pestaña: el navegador muestra su propio aviso (no admite texto propio).
    window.addEventListener('beforeunload', (e) => {
      if (this.pendientes().length) {
        e.preventDefault();
      }
    });
  }

  agregar(fuente: Fuente): () => void {
    this.fuentes.add(fuente);
    return () => this.fuentes.delete(fuente);
  }

  /** Etiquetas de lo que tiene cambios, sin repetir: ["Contactos", "Características"]. */
  pendientes(): string[] {
    return [...new Set([...this.fuentes].filter((f) => f.hay()).map((f) => f.etiqueta))];
  }

  /** Pide confirmación si hay cambios; emite true si se puede seguir. */
  confirmarSalida(confirmar: Confirmar): Observable<boolean> {
    const p = this.pendientes();
    if (!p.length) {
      return of(true);
    }
    const donde = p.length === 1 ? p[0] : `${p.slice(0, -1).join(', ')} y ${p[p.length - 1]}`;
    return confirmar
      .pedir('Hay cambios sin guardar', `Hay cambios sin guardar en ${donde}. Si salís ahora, se pierden.`, 'Salir sin guardar')
      .pipe(map((ok) => ok === true)); // cerrar el diálogo con Escape también es quedarse
  }
}

/**
 * Anota un formulario del componente actual (llamar en el constructor o en un inicializador de campo).
 * Se quita solo cuando el componente se destruye.
 */
export function avisarSiHayCambios(etiqueta: string, hay: () => boolean): void {
  const quitar = inject(CambiosPendientes).agregar({ etiqueta, hay });
  inject(DestroyRef).onDestroy(quitar);
}

/** Guard de las rutas con formularios: avisa antes de salir si quedó algo sin guardar. */
export const sinCambiosPendientes: CanDeactivateFn<unknown> = () =>
  inject(CambiosPendientes).confirmarSalida(inject(Confirmar));
