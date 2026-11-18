import { Routes } from '@angular/router';

import { sinCambiosPendientes } from './core/cambios-pendientes';
import { authGuard, rolGuard } from './core/rol.guard';
import { Shell } from './layout/shell';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./login/login.page').then((m) => m.LoginPage) },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'establecimientos' },
      {
        path: 'establecimientos',
        loadComponent: () => import('./establecimientos/establecimientos.page').then((m) => m.EstablecimientosPage),
      },
      {
        path: 'establecimientos/nuevo',
        canActivate: [rolGuard('ADMIN', 'CARGADOR')],
        canDeactivate: [sinCambiosPendientes],
        loadComponent: () => import('./establecimientos/establecimiento-nuevo.page').then((m) => m.EstablecimientoNuevoPage),
      },
      {
        path: 'establecimientos/:id',
        canDeactivate: [sinCambiosPendientes],
        loadComponent: () => import('./establecimientos/establecimiento.page').then((m) => m.EstablecimientoPage),
      },
      {
        path: 'ofertas/:id',
        canDeactivate: [sinCambiosPendientes],
        loadComponent: () => import('./ofertas/oferta.page').then((m) => m.OfertaPage),
      },
      { path: 'oferentes', loadComponent: () => import('./oferentes/oferentes.page').then((m) => m.OferentesPage) },
      {
        path: 'oferentes/:id',
        canDeactivate: [sinCambiosPendientes],
        loadComponent: () => import('./oferentes/oferente.page').then((m) => m.OferentePage),
      },
      { path: 'lugares', loadComponent: () => import('./lugares/lugares.page').then((m) => m.LugaresPage) },
      {
        path: 'duplicados',
        canActivate: [rolGuard('ADMIN', 'CARGADOR')],
        loadComponent: () => import('./duplicados/duplicados.page').then((m) => m.DuplicadosPage),
      },
      { path: 'catalogos', loadComponent: () => import('./catalogos/catalogos.page').then((m) => m.CatalogosPage) },
      {
        path: 'catalogos/caracteristica/limpieza',
        canActivate: [rolGuard('ADMIN')],
        loadComponent: () => import('./catalogos/limpiar-caracteristicas.page').then((m) => m.LimpiarCaracteristicasPage),
      },
      { path: 'categorias', loadComponent: () => import('./catalogos/categorias.page').then((m) => m.CategoriasPage) },
      { path: 'mapa', loadComponent: () => import('./mapa/mapa.page').then((m) => m.MapaPage) },
      {
        path: 'importacion',
        canActivate: [rolGuard('ADMIN')],
        loadComponent: () => import('./importacion/importacion.page').then((m) => m.ImportacionPage),
      },
      {
        path: 'usuarios',
        canActivate: [rolGuard('ADMIN')],
        loadComponent: () => import('./usuarios/usuarios.page').then((m) => m.UsuariosPage),
      },
      {
        path: 'auditoria',
        canActivate: [rolGuard('ADMIN')],
        loadComponent: () => import('./auditoria/auditoria.page').then((m) => m.AuditoriaPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
