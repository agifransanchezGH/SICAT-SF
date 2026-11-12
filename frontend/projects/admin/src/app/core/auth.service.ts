import { Injectable, computed, signal } from '@angular/core';

export type Rol = 'ADMIN' | 'CARGADOR' | 'CONSULTA';

/** Guarda el JWT de la sesión. El login real se implementa junto con /auth/login (paso 3). */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly token = signal<string | null>(sessionStorage.getItem('sicat.token'));
  private readonly roles = signal<Rol[]>([]);

  readonly autenticado = computed(() => this.token() !== null);

  obtenerToken(): string | null {
    return this.token();
  }

  tieneRol(...roles: Rol[]): boolean {
    return this.roles().some((r) => roles.includes(r));
  }

  iniciarSesion(token: string, roles: Rol[]): void {
    sessionStorage.setItem('sicat.token', token);
    this.token.set(token);
    this.roles.set(roles);
  }

  cerrarSesion(): void {
    sessionStorage.removeItem('sicat.token');
    this.token.set(null);
    this.roles.set([]);
  }
}
