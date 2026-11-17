import { Injectable } from '@angular/core';
import { Params } from '@angular/router';

const CLAVE = 'sicat.filtros.';

/**
 * Recuerda los filtros (query params) de un listado durante la sesión del navegador. Así, al ir a un
 * detalle y volver por la flecha, el menú o un enlace sin filtros, el listado queda como estaba.
 * Se guarda en sessionStorage (sobrevive a una recarga, no a cerrar la pestaña) con respaldo en memoria.
 */
@Injectable({ providedIn: 'root' })
export class FiltrosRecordados {
  private readonly memoria = new Map<string, Params>();

  guardar(listado: string, params: Params): void {
    const limpios = Object.fromEntries(
      Object.entries(params).filter(([, v]) => v != null && v !== '' && !(Array.isArray(v) && v.length === 0)),
    );
    this.memoria.set(listado, limpios);
    try {
      sessionStorage.setItem(CLAVE + listado, JSON.stringify(limpios));
    } catch {
      // sin almacenamiento disponible: queda solo en memoria
    }
  }

  /** Los últimos filtros del listado, o null si no hay ninguno. */
  leer(listado: string): Params | null {
    let p = this.memoria.get(listado) ?? null;
    if (!p) {
      try {
        const t = sessionStorage.getItem(CLAVE + listado);
        p = t ? (JSON.parse(t) as Params) : null;
      } catch {
        p = null;
      }
    }
    return p && Object.keys(p).length ? p : null;
  }
}
