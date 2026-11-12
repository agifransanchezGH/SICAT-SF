import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./listado/listado.page').then((m) => m.ListadoPage) },
  { path: 'establecimientos/:id', loadComponent: () => import('./ficha/ficha.page').then((m) => m.FichaPage) },
  { path: 'mapa', loadComponent: () => import('./mapa/mapa.page').then((m) => m.MapaPage) },
  { path: '**', redirectTo: '' },
];
