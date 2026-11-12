import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="barra">SICAT SF · Administración</header>
    <div class="cuerpo">
      <nav>
        @for (item of menu; track item.ruta) {
          <a [routerLink]="item.ruta" routerLinkActive="activo">{{ item.texto }}</a>
        }
      </nav>
      <main><router-outlet /></main>
    </div>
  `,
  styles: `
    .barra { padding: 12px 16px; font-weight: 600; border-bottom: 1px solid #ddd; }
    .cuerpo { display: flex; min-height: calc(100vh - 48px); }
    nav { display: flex; flex-direction: column; gap: 4px; padding: 12px; min-width: 200px; border-right: 1px solid #ddd; }
    nav a { padding: 6px 8px; border-radius: 6px; color: inherit; text-decoration: none; }
    nav a.activo { background: #eef4ff; }
    main { flex: 1; padding: 16px; }
  `,
})
export class Shell {
  protected readonly menu = [
    { ruta: 'oferentes', texto: 'Oferentes' },
    { ruta: 'establecimientos', texto: 'Establecimientos' },
    { ruta: 'ofertas', texto: 'Ofertas' },
    { ruta: 'especializaciones', texto: 'Especializaciones' },
    { ruta: 'lugares', texto: 'Lugares' },
    { ruta: 'catalogos', texto: 'Catálogos' },
    { ruta: 'mapa', texto: 'Mapa' },
    { ruta: 'auditoria', texto: 'Auditoría' },
  ];
}
