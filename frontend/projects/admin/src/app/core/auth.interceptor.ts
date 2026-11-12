import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';

import { AuthService } from './auth.service';

const SIN_TOKEN = ['/auth/login', '/auth/refresh'];

/**
 * Agrega Authorization: Bearer <jwt>. Ante un 401 intenta renovar la sesión una vez
 * y repite el pedido; si la renovación falla, cierra la sesión y vuelve al login.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (SIN_TOKEN.some((ruta) => req.url.endsWith(ruta))) {
    return next(req);
  }
  const auth = inject(AuthService);
  const conToken = (r: HttpRequest<unknown>, token: string | null) =>
    token ? r.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : r;

  return next(conToken(req, auth.obtenerToken())).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || !auth.autenticado()) {
        return throwError(() => error);
      }
      return auth.renovar().pipe(
        catchError((errorRenovar) => {
          auth.cerrarSesion();
          return throwError(() => errorRenovar);
        }),
        switchMap((token) => next(conToken(req, token))),
      );
    }),
  );
};
