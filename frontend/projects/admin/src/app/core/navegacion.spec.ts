import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { HistorialNavegacion } from './navegacion';

describe('HistorialNavegacion', () => {
  let historial: HistorialNavegacion;
  let navegar: jasmine.Spy;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    historial = TestBed.inject(HistorialNavegacion);
    navegar = spyOn(TestBed.inject(Router), 'navigateByUrl').and.resolveTo(true);
  });

  it('vuelve a la página de la que se vino, no al listado', () => {
    historial.registrar('/mapa');
    historial.registrar('/establecimientos/93');
    historial.volver('/establecimientos');
    expect(navegar).toHaveBeenCalledWith('/mapa');
  });

  it('cambiar de pestaña o de filtro no cuenta como otra página', () => {
    historial.registrar('/mapa');
    historial.registrar('/establecimientos/93');
    historial.registrar('/establecimientos/93?pestania=contactos');
    historial.registrar('/establecimientos/93?pestania=historial');
    expect(historial.anterior()).toBe('/mapa');
  });

  it('la página anterior conserva su query (filtros del listado)', () => {
    historial.registrar('/establecimientos?estado=BORRADOR');
    historial.registrar('/establecimientos/7');
    expect(historial.anterior()).toBe('/establecimientos?estado=BORRADOR');
  });

  it('al volver saca la página que se dejó: volver dos veces recorre el camino hacia atrás', () => {
    historial.registrar('/inicio');
    historial.registrar('/oferentes/5');
    historial.registrar('/establecimientos/93');
    historial.volver('/establecimientos');
    historial.registrar('/oferentes/5'); // NavigationEnd de la vuelta
    expect(historial.anterior()).toBe('/inicio');
  });

  it('sin página anterior (se entró directo) usa el destino por defecto', () => {
    historial.registrar('/establecimientos/93');
    historial.volver('/establecimientos');
    expect(navegar).toHaveBeenCalledWith('/establecimientos');
  });
});
