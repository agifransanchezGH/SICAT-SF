import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';

import { AuthService, Rol } from '../core/auth.service';

interface ItemMenu {
  ruta: string;
  texto: string;
  icono: string;
  roles?: Rol[];
}

@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatSidenavModule, MatListModule, MatIconModule, MatButtonModule,
  ],
  template: `
    <mat-toolbar class="barra">
      @if (movil()) {
        <button mat-icon-button aria-label="Menú" (click)="nav.toggle()"><mat-icon>menu</mat-icon></button>
      }
      <span class="marca">SICAT SF</span>
      <span class="sub">Panel de turismo</span>
      <span class="espacio"></span>
      @if (auth.usuario(); as u) {
        <span class="usuario">{{ u.nombreCompleto }} <span class="rol">{{ rolLegible(u.rol) }}</span></span>
        <button mat-icon-button aria-label="Cerrar sesión" title="Cerrar sesión" (click)="auth.cerrarSesion()">
          <mat-icon>logout</mat-icon>
        </button>
      }
    </mat-toolbar>
    <mat-sidenav-container class="contenedor">
      <mat-sidenav #nav [mode]="movil() ? 'over' : 'side'" [opened]="!movil()" class="lateral">
        <mat-nav-list aria-label="Secciones del panel">
          @for (item of menu(); track item.ruta) {
            <a mat-list-item [routerLink]="item.ruta" routerLinkActive #rla="routerLinkActive" [activated]="rla.isActive"
               (click)="movil() && nav.close()">
              <mat-icon matListItemIcon aria-hidden="true">{{ item.icono }}</mat-icon>
              <span matListItemTitle>{{ item.texto }}</span>
            </a>
          }
        </mat-nav-list>
      </mat-sidenav>
      <mat-sidenav-content>
        <router-outlet />
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: `
    :host { display: flex; flex-direction: column; height: 100vh; }
    .barra { gap: 10px; background: var(--mat-sys-surface); border-bottom: 1px solid var(--sicat-borde); }
    .marca { font-weight: 800; font-stretch: var(--sicat-ancho-titular); letter-spacing: -0.01em; font-size: 18px; color: var(--sicat-tinta); }
    .sub { color: var(--sicat-texto-suave); font-weight: 400; font-size: 15px; }
    .espacio { flex: 1; }
    .usuario { font-size: 14px; color: var(--sicat-tinta); }
    .rol { margin-left: 6px; padding: 1px 8px; border-radius: 999px; background: var(--sicat-rio-claro); color: var(--sicat-rio); font-size: 12px; font-weight: 600; }
    .contenedor { flex: 1; background: var(--sicat-fondo); }
    /* Menú en tinta de río: separa la herramienta del contenido sin competir con él */
    .lateral {
      width: 252px; border-right: 0; border-radius: 0; background: var(--sicat-tinta);
      --mat-list-list-item-label-text-color: #c9d6d8;
      --mat-list-list-item-hover-label-text-color: #ffffff;
      --mat-list-list-item-focus-label-text-color: #ffffff;
      --mat-list-list-item-leading-icon-color: #8fa6aa;
      --mat-list-list-item-hover-leading-icon-color: #ffffff;
      --mat-list-list-item-hover-state-layer-color: #ffffff;
      --mat-list-list-item-focus-state-layer-color: #ffffff;
      --mat-list-active-indicator-color: rgb(255 255 255 / 12%);
      --mat-list-active-indicator-shape: 6px;
    }
    .lateral mat-nav-list { padding: 12px 8px; }
    .lateral a.mdc-list-item--activated { --mat-list-list-item-label-text-color: #ffffff; --mat-list-list-item-leading-icon-color: #ffffff; box-shadow: inset 3px 0 var(--sicat-limo); }
    .lateral a:focus-visible { outline-color: #ffffff; outline-offset: -2px; }
    @media (max-width: 720px) { .usuario, .sub { display: none; } }
  `,
})
export class Shell {
  protected readonly auth = inject(AuthService);
  protected readonly rolLegible = (rol: string) =>
    ({ ADMIN: 'Administración', CARGADOR: 'Carga', CONSULTA: 'Consulta' } as Record<string, string>)[rol] ?? rol;

  protected readonly movil = toSignal(
    inject(BreakpointObserver).observe('(max-width: 900px)').pipe(map((r) => r.matches)),
    { initialValue: false },
  );

  private readonly items: ItemMenu[] = [
    { ruta: 'establecimientos', texto: 'Establecimientos', icono: 'storefront' },
    { ruta: 'oferentes', texto: 'Oferentes', icono: 'badge' },
    { ruta: 'lugares', texto: 'Lugares y ubicaciones', icono: 'place' },
    { ruta: 'duplicados', texto: 'Posibles duplicados', icono: 'content_copy', roles: ['ADMIN', 'CARGADOR'] },
    { ruta: 'catalogos', texto: 'Catálogos', icono: 'list_alt' },
    { ruta: 'categorias', texto: 'Categorías', icono: 'account_tree' },
    { ruta: 'mapa', texto: 'Mapa', icono: 'map' },
    { ruta: 'importacion', texto: 'Importación', icono: 'upload_file', roles: ['ADMIN'] },
    { ruta: 'usuarios', texto: 'Usuarios', icono: 'group', roles: ['ADMIN'] },
    { ruta: 'auditoria', texto: 'Auditoría', icono: 'history', roles: ['ADMIN'] },
  ];

  protected readonly menu = computed(() =>
    this.items.filter((i) => !i.roles || this.auth.tieneRol(...i.roles)),
  );
}
