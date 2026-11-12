import { Routes } from '@angular/router';

import { rolGuard } from './core/rol.guard';
import { Shell } from './layout/shell';

export const routes: Routes = [
  {
    path: '',
    component: Shell,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'establecimientos' },
      { path: 'oferentes', loadComponent: () => import('./oferentes/oferentes.page').then((m) => m.OferentesPage) },
      { path: 'establecimientos', loadComponent: () => import('./establecimientos/establecimientos.page').then((m) => m.EstablecimientosPage) },
      { path: 'ofertas', loadComponent: () => import('./ofertas/ofertas.page').then((m) => m.OfertasPage) },
      { path: 'especializaciones', loadComponent: () => import('./especializaciones/especializaciones.page').then((m) => m.EspecializacionesPage) },
      { path: 'lugares', loadComponent: () => import('./lugares/lugares.page').then((m) => m.LugaresPage) },
      { path: 'catalogos', loadComponent: () => import('./catalogos/catalogos.page').then((m) => m.CatalogosPage) },
      { path: 'mapa', loadComponent: () => import('./mapa/mapa.page').then((m) => m.MapaPage) },
      {
        path: 'auditoria',
        canActivate: [rolGuard('ADMIN')],
        loadComponent: () => import('./auditoria/auditoria.page').then((m) => m.AuditoriaPage),
      },
    ],
  },
];
