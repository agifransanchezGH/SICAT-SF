import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL, EstablecimientoFicha, EstablecimientoResumen, Pagina } from 'shared';

/** Cliente de la API Pública (solo GET). */
@Injectable({ providedIn: 'root' })
export class ApiPublicaService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(API_URL);

  listar(filtros: Record<string, string | number> = {}): Observable<Pagina<EstablecimientoResumen>> {
    return this.http.get<Pagina<EstablecimientoResumen>>(`${this.api}/establecimientos`, {
      params: new HttpParams({ fromObject: filtros }),
    });
  }

  ficha(id: number): Observable<EstablecimientoFicha> {
    return this.http.get<EstablecimientoFicha>(`${this.api}/establecimientos/${id}`);
  }
}
