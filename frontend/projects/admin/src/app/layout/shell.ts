import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService, Rol } from '../core/auth.service';

interface ItemMenu {
  ruta: string;
  texto: string;
  roles?: Rol[];
}

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="barra">
      <span class="marca">SICAT SF · Administración</span>
      @if (auth.usuario(); as u) {
        <span class="usuario">{{ u.nombreCompleto }} · {{ u.rol }}</span>
        <button type="button" (click)="auth.cerrarSesion()">Cerrar sesión</button>
      }
    </header>
    <div class="cuerpo">
      <nav>
        @for (item of menu(); track item.ruta) {
          <a [routerLink]="item.ruta" routerLinkActive="activo">{{ item.texto }}</a>
        }
      </nav>
      <main><router-outlet /></main>
    </div>
  `,
  styles: `
    .barra { display: flex; align-items: center; gap: 12px; padding: 10px 16px; border-bottom: 1px solid #ddd; }
    .marca { font-weight: 600; margin-right: auto; }
    .usuario { color: #5a626e; font-size: 14px; }
    .barra button { padding: 6px 10px; border: 1px solid #b8c0cc; border-radius: 6px; background: #fff; cursor: pointer; }
    .cuerpo { display: flex; min-height: calc(100vh - 50px); }
    nav { display: flex; flex-direction: column; gap: 4px; padding: 12px; min-width: 200px; border-right: 1px solid #ddd; }
    nav a { padding: 6px 8px; border-radius: 6px; color: inherit; text-decoration: none; }
    nav a.activo { background: #eef4ff; }
    main { flex: 1; padding: 16px; }
  `,
})
export class Shell {
  protected readonly auth = inject(AuthService);

  private readonly items: ItemMenu[] = [
    { ruta: 'oferentes', texto: 'Oferentes' },
    { ruta: 'establecimientos', texto: 'Establecimientos' },
    { ruta: 'ofertas', texto: 'Ofertas' },
    { ruta: 'especializaciones', texto: 'Especializaciones' },
    { ruta: 'lugares', texto: 'Lugares' },
    { ruta: 'catalogos', texto: 'Catálogos' },
    { ruta: 'mapa', texto: 'Mapa' },
    { ruta: 'auditoria', texto: 'Auditoría', roles: ['ADMIN'] },
  ];

  protected readonly menu = computed(() =>
    this.items.filter((i) => !i.roles || this.auth.tieneRol(...i.roles)),
  );
}
