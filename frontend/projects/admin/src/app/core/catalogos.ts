import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, shareReplay, tap, throwError } from 'rxjs';

import { Api } from './api';
import { CatalogoItem, CategoriaNodo, aplanarCategorias } from './modelos';

export type NombreCatalogo = 'tipo-oferente' | 'tipo-canal' | 'tipo-lugar' | 'localidad' | 'distincion' | 'caracteristica';

export const CATALOGOS: { nombre: NombreCatalogo; titulo: string }[] = [
  { nombre: 'tipo-oferente', titulo: 'Tipos de oferente' },
  { nombre: 'tipo-canal', titulo: 'Tipos de canal de contacto' },
  { nombre: 'tipo-lugar', titulo: 'Tipos de lugar' },
  { nombre: 'localidad', titulo: 'Localidades' },
  { nombre: 'distincion', titulo: 'Distinciones' },
  { nombre: 'caracteristica', titulo: 'Características' },
];

/**
 * Catálogos (solo activos) para los selects de los formularios. Se piden una vez por sesión;
 * la pantalla de catálogos llama a invalidar() después de cada cambio.
 */
@Injectable({ providedIn: 'root' })
export class Catalogos {
  private readonly api = inject(Api);
  private readonly cache = new Map<string, Observable<CatalogoItem[]>>();
  private arbol$?: Observable<CategoriaNodo[]>;

  /** Categorías aplanadas ("Padre › Hija"), solo activas. */
  readonly categorias = signal<{ id: number; nombre: string; activo: boolean }[]>([]);

  de(nombre: NombreCatalogo): Observable<CatalogoItem[]> {
    let r = this.cache.get(nombre);
    if (!r) {
      r = this.api.get<CatalogoItem[]>(`/catalogos/${nombre}`).pipe(
        // Un error no queda cacheado: el próximo pedido vuelve a intentar.
        catchError((e) => {
          this.cache.delete(nombre);
          return throwError(() => e);
        }),
        shareReplay(1),
      );
      this.cache.set(nombre, r);
    }
    return r;
  }

  arbolCategorias(): Observable<CategoriaNodo[]> {
    this.arbol$ ??= this.api.get<CategoriaNodo[]>('/categorias').pipe(
      tap((a) => this.categorias.set(aplanarCategorias(a))),
      catchError((e) => {
        this.arbol$ = undefined;
        return throwError(() => e);
      }),
      shareReplay(1),
    );
    return this.arbol$;
  }

  invalidar(nombre?: NombreCatalogo | 'categorias'): void {
    if (!nombre || nombre === 'categorias') {
      this.arbol$ = undefined;
    }
    if (nombre && nombre !== 'categorias') {
      this.cache.delete(nombre);
    } else if (!nombre) {
      this.cache.clear();
    }
  }
}
