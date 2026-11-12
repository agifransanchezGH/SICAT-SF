import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

import { AuthService, Rol } from './auth.service';

/** Exige sesión iniciada; si no hay, redirige a /login conservando la ruta pedida. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  return auth.autenticado() || inject(Router).createUrlTree(['/login'], { queryParams: { volver: state.url } });
};

/** Uso: canActivate: [rolGuard('ADMIN')]. Sin el rol, vuelve al inicio del panel. */
export const rolGuard =
  (...roles: Rol[]): CanActivateFn =>
  () =>
    inject(AuthService).tieneRol(...roles) || inject(Router).createUrlTree(['/']);
