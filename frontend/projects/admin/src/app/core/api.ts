import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from 'shared';

export type Parametros = Record<string, string | number | boolean | null | undefined | (string | number)[]>;

/** Cliente fino de la API Admin: arma la URL base y omite los parámetros vacíos. */
@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_URL);

  get<T>(ruta: string, parametros: Parametros = {}): Observable<T> {
    return this.http.get<T>(this.base + ruta, { params: params(parametros) });
  }

  post<T>(ruta: string, cuerpo: unknown = {}, parametros: Parametros = {}): Observable<T> {
    return this.http.post<T>(this.base + ruta, cuerpo, { params: params(parametros) });
  }

  put<T>(ruta: string, cuerpo: unknown): Observable<T> {
    return this.http.put<T>(this.base + ruta, cuerpo);
  }

  patch<T>(ruta: string, cuerpo: unknown): Observable<T> {
    return this.http.patch<T>(this.base + ruta, cuerpo);
  }

  delete(ruta: string): Observable<void> {
    return this.http.delete<void>(this.base + ruta);
  }

  subir<T>(ruta: string, archivo: File): Observable<T> {
    const datos = new FormData();
    datos.append('archivo', archivo);
    return this.http.post<T>(this.base + ruta, datos);
  }
}

function params(p: Parametros): HttpParams {
  let r = new HttpParams();
  for (const [clave, valor] of Object.entries(p)) {
    if (valor === null || valor === undefined || valor === '') {
      continue;
    }
    if (Array.isArray(valor)) {
      valor.forEach((v) => (r = r.append(clave, String(v))));
    } else {
      r = r.set(clave, String(valor));
    }
  }
  return r;
}
