import { computed, signal } from '@angular/core';

/** Estado de la casilla "seleccionar todo" de una página. */
export type EstadoPagina = 'todos' | 'algunos' | 'ninguno';

/**
 * Selección múltiple de un listado paginado. Guarda una copia de cada fila elegida, así la acción en lote sabe
 * qué hay seleccionado en otras páginas (p. ej. cuántos cumplen los requisitos) sin volver a pedirlo.
 */
export class Seleccion<T extends { id: number }> {
  private readonly filas = signal<ReadonlyMap<number, T>>(new Map());

  readonly cantidad = computed(() => this.filas().size);
  readonly elegidas = computed(() => [...this.filas().values()]);
  readonly ids = computed(() => [...this.filas().keys()]);

  tiene(id: number): boolean {
    return this.filas().has(id);
  }

  alternar(fila: T): void {
    this.filas.update((m) => {
      const n = new Map(m);
      if (n.has(fila.id)) {
        n.delete(fila.id);
      } else {
        n.set(fila.id, fila);
      }
      return n;
    });
  }

  /** "todos" si están todas las filas elegibles de la página; "algunos" si hay alguna; si no, "ninguno". */
  estadoDe(pagina: readonly T[]): EstadoPagina {
    const marcadas = pagina.filter((f) => this.filas().has(f.id)).length;
    return marcadas === 0 ? 'ninguno' : marcadas === pagina.length ? 'todos' : 'algunos';
  }

  /** Casilla del encabezado: si ya están todas, las quita; si no, agrega las que falten. */
  alternarPagina(pagina: readonly T[]): void {
    const quitar = this.estadoDe(pagina) === 'todos';
    this.filas.update((m) => {
      const n = new Map(m);
      pagina.forEach((f) => (quitar ? n.delete(f.id) : n.set(f.id, f)));
      return n;
    });
  }

  quitar(ids: Iterable<number>): void {
    this.filas.update((m) => {
      const n = new Map(m);
      for (const id of ids) {
        n.delete(id);
      }
      return n;
    });
  }

  limpiar(): void {
    this.filas.set(new Map());
  }
}
