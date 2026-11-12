import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';

import { AuthService, Rol } from './auth.service';

/** Uso: canActivate: [rolGuard('ADMIN')]. Mientras no exista el login, deja pasar. */
export const rolGuard =
  (...roles: Rol[]): CanActivateFn =>
  () => {
    const auth = inject(AuthService);
    return !auth.autenticado() || auth.tieneRol(...roles);
  };
