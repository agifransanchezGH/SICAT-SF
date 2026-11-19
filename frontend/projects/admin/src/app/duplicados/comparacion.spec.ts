import { DuplicadoItem, GrupoDuplicados } from '../core/modelos';
import { filasComparacion } from './comparacion';

const item = (id: number, cambios: Partial<DuplicadoItem> = {}): DuplicadoItem => ({
  id,
  nombre: 'Museo',
  direccion: 'San Martín 2000',
  estadoPublicacion: 'BORRADOR',
  oferente: 'Museo',
  categorias: ['Museo'],
  ofertasActivas: 1,
  contactos: [],
  ...cambios,
});

const grupo = (...items: DuplicadoItem[]): GrupoDuplicados => ({ clave: 'museo', mismaDireccion: true, establecimientos: items });
const fila = (g: GrupoDuplicados, etiqueta: string) => filasComparacion(g).find((f) => f.etiqueta === etiqueta)!;

describe('filasComparacion', () => {
  it('una fila por dato, con un valor por establecimiento', () => {
    const f = filasComparacion(grupo(item(1), item(2)));
    expect(f.map((x) => x.etiqueta)).toEqual(['Dirección', 'Oferente', 'Contactos', 'Categorías', 'Ofertas activas', 'Estado']);
    expect(f.every((x) => x.valores.length === 2)).toBeTrue();
  });

  it('no marca diferencias por mayúsculas, acentos ni orden de la lista', () => {
    const g = grupo(item(1, { direccion: 'San Martín 2000', categorias: ['Museo', 'Cultura'] }),
      item(2, { direccion: 'SAN MARTIN 2000', categorias: ['Cultura', 'Museo'] }));
    expect(fila(g, 'Dirección').difiere).toBeFalse();
    expect(fila(g, 'Categorías').difiere).toBeFalse();
  });

  it('marca lo que difiere y muestra los contactos internos', () => {
    const g = grupo(item(1, { ofertasActivas: 0, contactos: [{ tipo: 'Web', valor: 'museo.gob.ar', esPublico: false }] }), item(2));
    expect(fila(g, 'Ofertas activas').difiere).toBeTrue();
    expect(fila(g, 'Contactos').valores[0]).toEqual(['Web: museo.gob.ar (interno)']);
    expect(fila(g, 'Contactos').difiere).toBeTrue();
  });

  it('sin dirección queda vacío (y difiere de la que tiene)', () => {
    const g = grupo(item(1, { direccion: null }), item(2));
    expect(fila(g, 'Dirección').valores[0]).toEqual([]);
    expect(fila(g, 'Dirección').difiere).toBeTrue();
  });
});
