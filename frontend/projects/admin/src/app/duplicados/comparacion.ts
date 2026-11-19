import { GrupoDuplicados } from '../core/modelos';

/** Una fila de la comparación: un dato de cada establecimiento (varias líneas si es una lista). */
export interface FilaComparacion {
  etiqueta: string;
  valores: string[][];
  /** No todos tienen el mismo valor (sin importar mayúsculas, acentos ni el orden de la lista). */
  difiere: boolean;
}

const clave = (v: string[]) =>
  v.map((x) => x.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim()).sort().join('|');

/** Filas para comparar los integrantes de un grupo en columnas. */
export function filasComparacion(g: GrupoDuplicados): FilaComparacion[] {
  const e = g.establecimientos;
  const fila = (etiqueta: string, valores: string[][]): FilaComparacion => ({
    etiqueta,
    valores,
    difiere: new Set(valores.map(clave)).size > 1,
  });
  return [
    fila('Dirección', e.map((x) => (x.direccion ? [x.direccion] : []))),
    fila('Oferente', e.map((x) => [x.oferente])),
    fila('Contactos', e.map((x) => x.contactos.map((c) => `${c.tipo}: ${c.valor}${c.esPublico ? '' : ' (interno)'}`))),
    fila('Categorías', e.map((x) => x.categorias)),
    fila('Ofertas activas', e.map((x) => [String(x.ofertasActivas)])),
    fila('Estado', e.map((x) => [x.estadoPublicacion])),
  ];
}
