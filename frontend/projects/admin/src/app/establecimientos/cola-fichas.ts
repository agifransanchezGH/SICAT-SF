import { Injectable, signal } from '@angular/core';
import { Params } from '@angular/router';
import { Observable, map, of, switchMap } from 'rxjs';

/** Una página de la cola: ids en el orden del listado y total de la consulta. */
export interface PaginaCola {
  ids: number[];
  total: number;
}

/** Listado desde el que se abrió una ficha, con sus filtros y orden. */
export interface Cola {
  /** "Listos para publicar", "Ubicaciones a revisar"… */
  etiqueta: string;
  /** Para volver al listado tal como estaba. */
  volver: { ruta: string; queryParams: Params };
  pagina: number;
  tamanio: number;
  /**
   * false cuando las páginas no son de fichas sino de otra cosa (la cola de ubicaciones pagina lugares, cada uno
   * con 0 o más establecimientos): se navega igual, pero sin "5 de 77".
   */
  numerada: boolean;
  /** Pide otra página con los mismos filtros y orden. */
  cargar: (pagina: number) => Observable<PaginaCola>;
}

interface Estado extends Cola {
  ids: number[];
  total: number;
}

/**
 * Navegación entre fichas de una cola (listado filtrado o cola de ubicaciones). Recuerda qué ya quedó atrás:
 * al llegar al final de la página la vuelve a pedir, porque lo que se publicó o verificó puede haber salido de
 * la cola y corrido a los siguientes hacia esta página; recién si no queda nada pendiente pasa a la próxima.
 */
@Injectable({ providedIn: 'root' })
export class NavegacionCola {
  private readonly estado = signal<Estado | null>(null);
  /** Ids que ya quedaron atrás en el recorrido (el abierto y los anteriores a él). */
  private pasados = new Set<number>();

  readonly cola = this.estado.asReadonly();

  /** Se llama al abrir una ficha desde un listado: guarda la página visible y su posición. */
  fijar(cola: Cola, pagina: PaginaCola, abierto: number): void {
    this.estado.set({ ...cola, ...pagina });
    this.pasados = new Set();
    this.marcarHasta(abierto);
  }

  olvidar(): void {
    this.estado.set(null);
    this.pasados = new Set();
  }

  /** "5 de 77" si la ficha está en la cola (números null si la cola no es numerada); null si se abrió por otro camino. */
  posicion(id: number): { numero: number | null; total: number | null } | null {
    const e = this.estado();
    const i = e?.ids.indexOf(id) ?? -1;
    if (!e || i < 0) {
      return null;
    }
    return e.numerada ? { numero: e.pagina * e.tamanio + i + 1, total: e.total } : { numero: null, total: null };
  }

  siguiente(actual: number): Observable<number | null> {
    const e = this.estado();
    if (!e) {
      return of(null);
    }
    this.marcarHasta(actual);
    // Dentro de la página se sigue el orden (también después de haber vuelto con "Anterior").
    const i = e.ids.indexOf(actual);
    if (i >= 0 && i + 1 < e.ids.length) {
      return of(this.abrir(e.ids[i + 1]));
    }
    // Fin de la página: puede haber pendientes que se corrieron hacia esta página (los que salieron de la cola).
    return e.cargar(e.pagina).pipe(
      switchMap((misma) => {
        // Si el actual sigue en la página recargada, va el que le sigue; si salió de la cola, el primero pendiente.
        const j = misma.ids.indexOf(actual);
        const pendiente = j >= 0 && j + 1 < misma.ids.length ? misma.ids[j + 1] : primeroPendiente(misma.ids, this.pasados);
        if (pendiente != null) {
          this.estado.set({ ...e, ...misma });
          return of(this.abrir(pendiente));
        }
        if ((e.pagina + 1) * e.tamanio >= misma.total) {
          return of(null);
        }
        return e.cargar(e.pagina + 1).pipe(
          map((otra) => {
            this.estado.set({ ...e, ...otra, pagina: e.pagina + 1 });
            const primero = primeroPendiente(otra.ids, this.pasados);
            return primero == null ? null : this.abrir(primero);
          }),
        );
      }),
    );
  }

  anterior(actual: number): Observable<number | null> {
    const e = this.estado();
    const i = e?.ids.indexOf(actual) ?? -1;
    if (!e || i < 0) {
      return of(null);
    }
    if (i > 0) {
      return of(e.ids[i - 1]);
    }
    if (e.pagina === 0) {
      return of(null);
    }
    return e.cargar(e.pagina - 1).pipe(
      map((previa) => {
        this.estado.set({ ...e, ...previa, pagina: e.pagina - 1 });
        return previa.ids.at(-1) ?? null;
      }),
    );
  }

  hayAnterior(id: number): boolean {
    const e = this.estado();
    const i = e?.ids.indexOf(id) ?? -1;
    return !!e && i >= 0 && (i > 0 || e.pagina > 0);
  }

  private abrir(id: number): number {
    this.marcarHasta(id);
    return id;
  }

  /** Marca como pasados el id y todos los que lo preceden en la página actual. */
  private marcarHasta(id: number): void {
    const ids = this.estado()?.ids ?? [];
    const i = ids.indexOf(id);
    ids.slice(0, i + 1).forEach((x) => this.pasados.add(x));
    this.pasados.add(id);
  }
}

/** Primer id de la página que todavía no quedó atrás. */
export function primeroPendiente(ids: readonly number[], pasados: ReadonlySet<number>): number | null {
  return ids.find((id) => !pasados.has(id)) ?? null;
}
