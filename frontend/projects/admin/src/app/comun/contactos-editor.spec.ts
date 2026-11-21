import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_URL } from 'shared';

import { ContactosEditor } from './contactos-editor';

describe('ContactosEditor: tabla editable', () => {
  function crear() {
    TestBed.configureTestingModule({
      imports: [ContactosEditor],
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: '/api' }],
    });
    const f = TestBed.createComponent(ContactosEditor);
    f.componentRef.setInput('ruta', '/establecimientos/1/contactos');
    f.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne((r) => r.url.endsWith('/catalogos/tipo-canal')).flush([{ id: 1, nombre: 'Teléfono', activo: true }]);
    http.expectOne((r) => r.url.endsWith('/establecimientos/1/contactos')).flush([
      { id: 10, tipoCanalId: 1, valor: '342 455 1234', uso: 'Reservas', esPublico: true },
      { id: 11, tipoCanalId: 1, valor: '342 400 0000', uso: '', esPublico: false },
    ]);
    f.detectChanges();
    return f.nativeElement as HTMLElement;
  }

  it('muestra los títulos de columna una sola vez y una fila por contacto', () => {
    const el = crear();
    const titulos = [...el.querySelectorAll('.te-cabecera span')].map((s) => s.textContent?.trim()).filter(Boolean);
    expect(titulos).toEqual(['Tipo', 'Valor', 'Uso', 'Público']);
    expect(el.querySelectorAll('.te-fila').length).toBe(2);
  });

  it('cada campo se nombra con su columna y el número de fila (no depende de los títulos visuales)', () => {
    const el = crear();
    const fila = el.querySelectorAll('.te-fila')[1];
    expect(fila.getAttribute('aria-label')).toBe('Contacto 2');
    const nombres = [...fila.querySelectorAll('input[matinput]')].map((i) => i.getAttribute('aria-label'));
    expect(nombres).toEqual(['Valor del contacto 2', 'Uso del contacto 2']);
    expect(fila.querySelector('mat-select')?.getAttribute('aria-label')).toBe('Tipo del contacto 2');
    expect(fila.querySelector('button[aria-label="Quitar contacto 2"]')).not.toBeNull();
  });
});
