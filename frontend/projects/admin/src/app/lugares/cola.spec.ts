import { elegirSiguiente } from './cola';

const ids = (...n: number[]) => n.map((id) => ({ id }));

describe('elegirSiguiente', () => {
  it('vista "A revisar": el confirmado sale y sigue el que estaba después', () => {
    expect(elegirSiguiente(ids(1, 2, 3), ids(1, 3, 4), 2)).toEqual({ id: 3 });
  });

  it('vista "Todos": el confirmado se queda y sigue el que estaba después', () => {
    expect(elegirSiguiente(ids(1, 2, 3), ids(1, 2, 3), 2)).toEqual({ id: 3 });
  });

  it('último de la página que sale: sigue el que entró desde la página siguiente', () => {
    expect(elegirSiguiente(ids(1, 2, 3), ids(1, 2, 4), 3)).toEqual({ id: 4 });
  });

  it('salta los que ya no están en la cola (otro los confirmó)', () => {
    expect(elegirSiguiente(ids(1, 2, 3, 4), ids(1, 4, 5), 2)).toEqual({ id: 4 });
  });

  it('no vuelve atrás a uno que estaba antes del actual', () => {
    expect(elegirSiguiente(ids(1, 2, 3), ids(1, 2, 3), 3)).toBeNull();
  });

  it('cola vacía después de confirmar el último', () => {
    expect(elegirSiguiente(ids(7), [], 7)).toBeNull();
  });
});
