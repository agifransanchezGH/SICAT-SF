import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { API_URL, CategoriaPublica, EstablecimientoFicha, EstablecimientoResumen, LocalidadPublica, Pagina } from 'shared';

export interface FiltrosListado {
  q?: string | null;
  categoria?: number | string | null;
  localidad?: number | string | null;
  page?: number;
  size?: number;
}

/** Cliente de la API Pública (solo GET). */
@Injectable({ providedIn: 'root' })
export class ApiPublicaService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(API_URL);

  private categorias$?: Observable<CategoriaPublica[]>;
  private localidades$?: Observable<LocalidadPublica[]>;

  listar(filtros: FiltrosListado = {}): Observable<Pagina<EstablecimientoResumen>> {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(filtros)) {
      if (valor !== null && valor !== undefined && valor !== '') {
        params = params.set(clave, String(valor));
      }
    }
    return this.http.get<Pagina<EstablecimientoResumen>>(`${this.api}/establecimientos`, { params });
  }

  ficha(id: number): Observable<EstablecimientoFicha> {
    return this.http.get<EstablecimientoFicha>(`${this.api}/establecimientos/${id}`);
  }

  categorias(): Observable<CategoriaPublica[]> {
    this.categorias$ ??= this.http.get<CategoriaPublica[]>(`${this.api}/categorias`).pipe(shareReplay(1));
    return this.categorias$;
  }

  /** GeoJSON de puntos de los publicados; mismos filtros que el listado. */
  mapa(filtros: FiltrosListado = {}): Observable<GeoJSON.FeatureCollection<GeoJSON.Point>> {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(filtros)) {
      if (valor !== null && valor !== undefined && valor !== '') {
        params = params.set(clave, String(valor));
      }
    }
    return this.http.get<GeoJSON.FeatureCollection<GeoJSON.Point>>(`${this.api}/mapa`, { params });
  }

  /** GeoJSON de líneas de los circuitos publicados. */
  circuitos(): Observable<GeoJSON.FeatureCollection<GeoJSON.LineString>> {
    return this.http.get<GeoJSON.FeatureCollection<GeoJSON.LineString>>(`${this.api}/mapa/circuitos`);
  }

  localidades(): Observable<LocalidadPublica[]> {
    this.localidades$ ??= this.http.get<LocalidadPublica[]>(`${this.api}/localidades`).pipe(shareReplay(1));
    return this.localidades$;
  }
}
