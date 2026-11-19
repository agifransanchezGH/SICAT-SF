import { Seleccion } from './seleccion';

const filas = (...ids: number[]) => ids.map((id) => ({ id, nombre: `Fila ${id}` }));

describe('Seleccion', () => {
  it('alterna filas sueltas y cuenta', () => {
    const s = new Seleccion<{ id: number; nombre: string }>();
    const [a, b] = filas(1, 2);
    s.alternar(a);
    s.alternar(b);
    s.alternar(a);
    expect(s.cantidad()).toBe(1);
    expect(s.tiene(2)).toBeTrue();
    expect(s.tiene(1)).toBeFalse();
  });

  it('el estado de la página distingue todos, algunos y ninguno', () => {
    const s = new Seleccion<{ id: number; nombre: string }>();
    const pagina = filas(1, 2, 3);
    expect(s.estadoDe(pagina)).toBe('ninguno');
    s.alternar(pagina[0]);
    expect(s.estadoDe(pagina)).toBe('algunos');
    s.alternarPagina(pagina);
    expect(s.estadoDe(pagina)).toBe('todos');
    expect(s.cantidad()).toBe(3);
  });

  it('desmarcar la página quita solo sus filas y conserva las de otras páginas', () => {
    const s = new Seleccion<{ id: number; nombre: string }>();
    const pagina1 = filas(1, 2);
    const pagina2 = filas(3, 4);
    s.alternarPagina(pagina1);
    s.alternar(pagina2[0]);
    s.alternarPagina(pagina1);
    expect(s.ids()).toEqual([3]);
  });

  it('guarda la copia de la fila para usarla desde otra página', () => {
    const s = new Seleccion<{ id: number; nombre: string }>();
    s.alternar({ id: 9, nombre: 'Museo' });
    expect(s.elegidas()).toEqual([{ id: 9, nombre: 'Museo' }]);
  });

  it('quitar y limpiar', () => {
    const s = new Seleccion<{ id: number; nombre: string }>();
    s.alternarPagina(filas(1, 2, 3));
    s.quitar([1, 3]);
    expect(s.ids()).toEqual([2]);
    s.limpiar();
    expect(s.cantidad()).toBe(0);
  });

  it('una página vacía no queda "todos"', () => {
    expect(new Seleccion<{ id: number }>().estadoDe([])).toBe('ninguno');
  });
});
