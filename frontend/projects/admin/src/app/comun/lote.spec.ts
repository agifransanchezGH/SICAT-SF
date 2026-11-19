import { ResultadoLote, resumenLote } from './lote';

const resultado = (exitosos: number, fallidos: number): ResultadoLote => ({
  procesados: exitosos + fallidos,
  exitosos,
  fallidos,
  resultados: [],
});

describe('resumenLote', () => {
  it('todos bien', () => {
    expect(resumenLote(resultado(3, 0), 'publicado', 'publicar')).toBe('3 publicados');
  });

  it('singular y plural de los rechazados', () => {
    expect(resumenLote(resultado(1, 1), 'publicado', 'publicar')).toBe('1 publicado, 1 no se pudo publicar');
    expect(resumenLote(resultado(0, 2), 'confirmada', 'confirmar')).toBe('0 confirmadas, 2 no se pudieron confirmar');
  });
});
