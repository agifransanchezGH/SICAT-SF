import { Faltante } from '../core/modelos';
import { accionRapida } from './publicacion-rapida';

const falta = (codigo: Faltante['codigo'], texto: string): Faltante => ({ codigo, texto });

describe('accionRapida', () => {
  it('un publicado siempre se puede despublicar', () => {
    expect(accionRapida({ estadoPublicacion: 'PUBLICADO', faltantes: [] }))
      .toEqual({ accion: 'despublicar', habilitada: true, motivo: null });
  });

  it('un borrador sin faltantes se puede publicar', () => {
    expect(accionRapida({ estadoPublicacion: 'BORRADOR', faltantes: [] }))
      .toEqual({ accion: 'publicar', habilitada: true, motivo: null });
  });

  it('con faltantes queda deshabilitado y el motivo los lista en orden', () => {
    const a = accionRapida({
      estadoPublicacion: 'OCULTO',
      faltantes: [falta('UBICACION_VERIFICADA', 'ubicación sin verificar'), falta('OFERTA_ACTIVA', 'una oferta activa')],
    });
    expect(a.habilitada).toBeFalse();
    expect(a.accion).toBe('publicar');
    expect(a.motivo).toBe('Falta: ubicación sin verificar, una oferta activa');
  });

  it('tolera una respuesta vieja sin faltantes', () => {
    expect(accionRapida({ estadoPublicacion: 'BORRADOR', faltantes: undefined as unknown as Faltante[] }).habilitada).toBeTrue();
  });
});
