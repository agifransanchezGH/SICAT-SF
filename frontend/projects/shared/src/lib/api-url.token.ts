import { InjectionToken } from '@angular/core';

/** URL base de la API que usa cada app ('/api/admin/v1' o '/api/public/v1'). */
export const API_URL = new InjectionToken<string>('API_URL');
