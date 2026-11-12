import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { AuthService } from './auth.service';

/** Agrega Authorization: Bearer <jwt> a los pedidos a la API Admin. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthService).obtenerToken();
  return next(token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req);
};
