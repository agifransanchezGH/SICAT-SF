import { CategoriaNodo } from '../core/modelos';
import { contarMapa } from './conteos';

const nodo = (id: number, nombre: string, hijos: CategoriaNodo[] = [], padreId: number | null = null): CategoriaNodo =>
  ({ id, nombre, padreId, activo: true, hijos });

// Gastronomía (1) › Restaurantes (11), Bares (12); Alojamiento (2) › Hotel (21)
const arbol = [
  nodo(1, 'Gastronomía', [nodo(11, 'Restaurantes', [], 1), nodo(12, 'Bares', [], 1)]),
  nodo(2, 'Alojamiento', [nodo(21, 'Hotel', [], 2)]),
];

describe('contarMapa', () => {
  const puntos = [
    { estadoPublicacion: 'BORRADOR', categorias: [11] },
    { estadoPublicacion: 'BORRADOR', categorias: [11, 12] },
    { estadoPublicacion: 'PUBLICADO', categorias: [21, 12] },
    { estadoPublicacion: 'OCULTO', categorias: [] },
  ];

  it('cuenta por estado', () => {
    expect(contarMapa(puntos, arbol, null).porEstado).toEqual({ BORRADOR: 2, PUBLICADO: 1, OCULTO: 1 });
  });

  it('sin filtro agrupa por categoría raíz y cuenta cada establecimiento una vez por raíz', () => {
    expect(contarMapa(puntos, arbol, null).porCategoria).toEqual([
      { id: 1, nombre: 'Gastronomía', cantidad: 3 },
      { id: 2, nombre: 'Alojamiento', cantidad: 1 },
    ]);
  });

  it('con una categoría elegida, desglosa por sus subcategorías', () => {
    expect(contarMapa(puntos, arbol, 1).porCategoria).toEqual([
      { id: 12, nombre: 'Bares', cantidad: 2 },
      { id: 11, nombre: 'Restaurantes', cantidad: 2 },
    ]);
  });

  it('con una categoría sin subcategorías, muestra solo esa', () => {
    expect(contarMapa(puntos, arbol, 21).porCategoria).toEqual([{ id: 21, nombre: 'Hotel', cantidad: 1 }]);
  });
});
