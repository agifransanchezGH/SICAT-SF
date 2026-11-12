import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, finalize, map, shareReplay, tap } from 'rxjs';
import { API_URL } from 'shared';

export type Rol = 'ADMIN' | 'CARGADOR' | 'CONSULTA';

export interface UsuarioSesion {
  id: number;
  nombreCompleto: string;
  email: string;
  rol: Rol;
}

/** Respuesta de /auth/login y /auth/refresh. */
interface SesionRespuesta {
  accessToken: string;
  refreshToken: string;
  tipo: string;
  expiraEn: number;
  usuario: UsuarioSesion;
}

interface Sesion {
  accessToken: string;
  refreshToken: string;
  usuario: UsuarioSesion;
}

const CLAVE = 'sicat.sesion';

/**
 * Sesión del panel: guarda access y refresh token en sessionStorage (se pierde al cerrar la pestaña).
 * La renovación la dispara authInterceptor ante un 401.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly api = inject(API_URL);

  private readonly sesion = signal<Sesion | null>(leerSesion());
  private renovacionEnCurso: Observable<string> | null = null;

  readonly usuario = computed(() => this.sesion()?.usuario ?? null);
  readonly autenticado = computed(() => this.sesion() !== null);

  obtenerToken(): string | null {
    return this.sesion()?.accessToken ?? null;
  }

  tieneRol(...roles: Rol[]): boolean {
    const rol = this.usuario()?.rol;
    return rol !== undefined && roles.includes(rol);
  }

  iniciarSesion(email: string, password: string): Observable<UsuarioSesion> {
    return this.http
      .post<SesionRespuesta>(`${this.api}/auth/login`, { email, password })
      .pipe(
        tap((r) => this.guardar(r)),
        map((r) => r.usuario),
      );
  }

  /** Pide un access token nuevo con el refresh token. Comparte el pedido si ya hay uno en curso. */
  renovar(): Observable<string> {
    const refreshToken = this.sesion()?.refreshToken;
    if (!refreshToken) {
      throw new Error('No hay sesión para renovar');
    }
    this.renovacionEnCurso ??= this.http
      .post<SesionRespuesta>(`${this.api}/auth/refresh`, { refreshToken })
      .pipe(
        tap((r) => this.guardar(r)),
        map((r) => r.accessToken),
        finalize(() => (this.renovacionEnCurso = null)),
        shareReplay(1),
      );
    return this.renovacionEnCurso;
  }

  cerrarSesion(): void {
    sessionStorage.removeItem(CLAVE);
    this.sesion.set(null);
    this.router.navigate(['/login']);
  }

  private guardar(r: SesionRespuesta): void {
    const sesion: Sesion = { accessToken: r.accessToken, refreshToken: r.refreshToken, usuario: r.usuario };
    sessionStorage.setItem(CLAVE, JSON.stringify(sesion));
    this.sesion.set(sesion);
  }
}

function leerSesion(): Sesion | null {
  try {
    const crudo = sessionStorage.getItem(CLAVE);
    return crudo ? (JSON.parse(crudo) as Sesion) : null;
  } catch {
    return null;
  }
}
