import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { API_URL } from 'shared';

import { Lugar } from '../core/modelos';
import { PanelUbicacion } from './panel-ubicacion';

const lugar = (extra: Partial<Lugar> = {}) =>
  ({ id: 7, nombre: 'Plaza', coordenadas: { lat: -31.6, lng: -60.7 }, establecimientos: [], ...extra }) as Lugar;

describe('PanelUbicacion: eliminar', () => {
  function crear(l: Lugar, eliminable: boolean) {
    TestBed.configureTestingModule({
      imports: [PanelUbicacion],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), { provide: API_URL, useValue: '/api' }],
    });
    const f = TestBed.createComponent(PanelUbicacion);
    f.componentRef.setInput('lugar', l);
    f.componentRef.setInput('eliminable', eliminable);
    f.detectChanges();
    const boton = [...(f.nativeElement as HTMLElement).querySelectorAll('button')].find((b) => b.textContent?.includes('Eliminar lugar'));
    return { f, boton };
  }

  it('lo ofrece a un administrador si ningún establecimiento usa el lugar, y avisa con el lugar', () => {
    const { f, boton } = crear(lugar(), true);
    let emitido: Lugar | undefined;
    f.componentInstance.eliminar.subscribe((l) => (emitido = l));
    boton!.click();
    expect(emitido?.id).toBe(7);
  });

  it('no lo ofrece sin rol de administrador', () => {
    expect(crear(lugar(), false).boton).toBeUndefined();
  });

  it('no lo ofrece si algún establecimiento (aunque esté de baja) usa el lugar', () => {
    const usado = lugar({ establecimientos: [{ id: 1, nombre: 'Bar', activo: false }] as Lugar['establecimientos'] });
    expect(crear(usado, true).boton).toBeUndefined();
  });
});
