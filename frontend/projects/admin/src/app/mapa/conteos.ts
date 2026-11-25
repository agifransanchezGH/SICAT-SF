import { CategoriaNodo } from '../core/modelos';

export interface ConteoCategoria {
  id: number;
  nombre: string;
  cantidad: number;
}

export interface ConteosMapa {
  /** Cantidad por estado de publicación (BORRADOR, PUBLICADO, OCULTO). */
  porEstado: Partial<Record<string, number>>;
  /**
   * Cantidad por categoría, de mayor a menor. Sin filtro de categoría, por categoría raíz; con filtro, por las
   * subcategorías de la elegida (si no tiene, la elegida sola). Un establecimiento cuenta una vez por categoría,
   * aunque tenga varias ofertas en ella; si tiene ofertas en dos categorías, suma en las dos.
   */
  porCategoria: ConteoCategoria[];
}

/** Propiedades de un punto de GET /mapa/establecimientos que usan los conteos. */
export interface PuntoMapa {
  estadoPublicacion?: string;
  categorias?: number[];
}

/**
 * Conteos de la leyenda del mapa a partir de los puntos ya filtrados (el GeoJSON que se dibuja): así respetan
 * siempre los mismos filtros que el mapa, sin otra consulta.
 */
export function contarMapa(puntos: readonly PuntoMapa[], arbol: readonly CategoriaNodo[], categoriaFiltro: number | null): ConteosMapa {
  const porEstado: Partial<Record<string, number>> = {};
  for (const p of puntos) {
    if (p.estadoPublicacion) {
      porEstado[p.estadoPublicacion] = (porEstado[p.estadoPublicacion] ?? 0) + 1;
    }
  }

  // Grupos a mostrar y, para cada categoría del árbol, a qué grupo pertenece.
  const elegida = categoriaFiltro == null ? null : buscar(arbol, categoriaFiltro);
  const grupos: CategoriaNodo[] = elegida ? (elegida.hijos.length ? elegida.hijos : [elegida]) : [...arbol];
  const grupoDe = new Map<number, number>();
  const marcar = (n: CategoriaNodo, grupo: number) => {
    grupoDe.set(n.id, grupo);
    n.hijos.forEach((h) => marcar(h, grupo));
  };
  grupos.forEach((g) => marcar(g, g.id));

  const cantidad = new Map<number, number>();
  for (const p of puntos) {
    const suyos = new Set((p.categorias ?? []).map((c) => grupoDe.get(c)).filter((g): g is number => g != null));
    suyos.forEach((g) => cantidad.set(g, (cantidad.get(g) ?? 0) + 1));
  }
  const porCategoria = grupos
    .map((g) => ({ id: g.id, nombre: g.nombre, cantidad: cantidad.get(g.id) ?? 0 }))
    .filter((c) => c.cantidad > 0)
    .sort((a, b) => b.cantidad - a.cantidad || a.nombre.localeCompare(b.nombre, 'es'));
  return { porEstado, porCategoria };
}

function buscar(nodos: readonly CategoriaNodo[], id: number): CategoriaNodo | null {
  for (const n of nodos) {
    if (n.id === id) {
      return n;
    }
    const enHijos = buscar(n.hijos, id);
    if (enHijos) {
      return enHijos;
    }
  }
  return null;
}
