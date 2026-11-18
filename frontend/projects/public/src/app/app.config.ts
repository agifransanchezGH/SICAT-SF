import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import localeEsAr from '@angular/common/locales/es-AR';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling, withViewTransitions } from '@angular/router';
import { API_URL } from 'shared';

import { routes } from './app.routes';
import { PaginadorEnEspanol } from './core/paginador';
import { environment } from '../environments/environment';

registerLocaleData(localeEsAr);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'enabled' }),
      // Del listado a la ficha el nombre "viaja" (view-transition-name); se omite si el navegador no lo soporta.
      withViewTransitions({ skipInitialTransition: true }),
    ),
    provideHttpClient(),
    { provide: API_URL, useValue: environment.apiUrl },
    { provide: LOCALE_ID, useValue: 'es-AR' },
    { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { appearance: 'outline', subscriptSizing: 'dynamic' } },
    { provide: MatPaginatorIntl, useClass: PaginadorEnEspanol },
  ],
};
