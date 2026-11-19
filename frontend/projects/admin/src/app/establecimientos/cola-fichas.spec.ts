import { firstValueFrom, of } from 'rxjs';

import { Cola, NavegacionCola, PaginaCola } from './cola-fichas';

/** Cola paginada de 2 en 2 sobre una lista que se puede modificar entre pedidos (simula filtros como "listos"). */
function colaSobre(lista: number[]): { cola: Cola; pedidos: number[] } {
  const pedidos: number[] = [];
  const cola: Cola = {
    etiqueta: 'Prueba',
    volver: { ruta: '/establecimientos', queryParams: {} },
    pagina: 0,
    tamanio: 2,
    numerada: true,
    cargar: (pagina) => {
      pedidos.push(pagina);
      return of<PaginaCola>({ ids: lista.slice(pagina * 2, pagina * 2 + 2), total: lista.length });
    },
  };
  return { cola, pedidos };
}

describe('NavegacionCola', () => {
  let nav: NavegacionCola;
  beforeEach(() => (nav = new NavegacionCola()));

  it('posición, siguiente dentro de la página y fuera de la cola', async () => {
    const { cola } = colaSobre([1, 2, 3]);
    nav.fijar(cola, { ids: [1, 2], total: 3 }, 1);
    expect(nav.posicion(1)).toEqual({ numero: 1, total: 3 });
    expect(nav.posicion(99)).toBeNull();
    expect(await firstValueFrom(nav.siguiente(1))).toBe(2);
  });

  it('cola fija: al final de la página pasa a la siguiente', async () => {
    const { cola, pedidos } = colaSobre([1, 2, 3, 4]);
    nav.fijar(cola, { ids: [1, 2], total: 4 }, 2);
    expect(await firstValueFrom(nav.siguiente(2))).toBe(3);
    expect(pedidos).toEqual([0, 1]); // primero revisa la misma página, después la próxima
    expect(nav.posicion(3)).toEqual({ numero: 3, total: 4 });
  });

  it('cola que se vacía (p. ej. "listos" al publicar): toma los que se corrieron a esta página', async () => {
    const lista = [1, 2, 3, 4];
    const { cola, pedidos } = colaSobre(lista);
    nav.fijar(cola, { ids: [1, 2], total: 4 }, 1);
    lista.splice(0, 2); // se publicaron 1 y 2: salen del filtro
    expect(await firstValueFrom(nav.siguiente(2))).toBe(3);
    expect(pedidos).toEqual([0]);
  });

  it('después de volver con "Anterior", "Siguiente" avanza en orden', async () => {
    const { cola } = colaSobre([1, 2, 3]);
    nav.fijar(cola, { ids: [1, 2], total: 3 }, 1);
    expect(await firstValueFrom(nav.siguiente(1))).toBe(2);
    expect(await firstValueFrom(nav.anterior(2))).toBe(1);
    expect(await firstValueFrom(nav.siguiente(1))).toBe(2);
  });

  it('no vuelve a los anteriores al abierto aunque no se hayan visto', async () => {
    const { cola } = colaSobre([1, 2]);
    nav.fijar(cola, { ids: [1, 2], total: 2 }, 2);
    expect(await firstValueFrom(nav.siguiente(2))).toBeNull();
  });

  it('anterior dentro de la página y desde el primero de la página siguiente', async () => {
    const { cola } = colaSobre([1, 2, 3, 4]);
    nav.fijar(cola, { ids: [3, 4], total: 4 }, 3);
    // la cola se fijó en la página 0 con ids de la 1: se ajusta con el estado real
    nav.fijar({ ...cola, pagina: 1 }, { ids: [3, 4], total: 4 }, 4);
    expect(await firstValueFrom(nav.anterior(4))).toBe(3);
    expect(await firstValueFrom(nav.anterior(3))).toBe(2);
    expect(nav.posicion(2)).toEqual({ numero: 2, total: 4 });
    expect(nav.hayAnterior(2)).toBeTrue();
    expect(nav.hayAnterior(99)).toBeFalse();
  });

  it('cola sin numerar (ubicaciones): posición sin números', () => {
    const { cola } = colaSobre([1, 2]);
    nav.fijar({ ...cola, numerada: false }, { ids: [1, 2], total: 2 }, 2);
    expect(nav.posicion(2)).toEqual({ numero: null, total: null });
    expect(nav.hayAnterior(2)).toBeTrue();
    expect(nav.hayAnterior(1)).toBeFalse();
  });

  it('sin cola no navega', async () => {
    expect(await firstValueFrom(nav.siguiente(1))).toBeNull();
    expect(await firstValueFrom(nav.anterior(1))).toBeNull();
  });
});
