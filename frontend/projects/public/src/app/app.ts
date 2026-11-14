import { Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatIconModule],
  template: `
    <header class="barra">
      <div class="interior">
        <a routerLink="/" class="marca">
          <mat-icon>travel_explore</mat-icon>
          <span><strong>Santa Fe</strong> · Oferta turística</span>
        </a>
        <nav>
          <a routerLink="/" routerLinkActive="activo" [routerLinkActiveOptions]="{ paths: 'exact', queryParams: 'ignored', matrixParams: 'ignored', fragment: 'ignored' }">Buscar</a>
          <a routerLink="/mapa" routerLinkActive="activo">Mapa</a>
        </nav>
      </div>
    </header>
    <main><router-outlet /></main>
    <footer class="pie">
      <div class="interior suave">Municipalidad de la Ciudad de Santa Fe · Dirección de Turismo</div>
    </footer>
  `,
  styles: `
    :host { display: flex; flex-direction: column; min-height: 100vh; }
    .barra { background: var(--mat-sys-surface); border-bottom: 1px solid var(--sicat-borde); }
    .interior { max-width: 1120px; margin: 0 auto; padding: 14px 16px; box-sizing: border-box; }
    .barra .interior { display: flex; align-items: center; gap: 16px; }
    nav { margin-left: auto; display: flex; gap: 4px; }
    nav a { padding: 6px 12px; border-radius: 999px; color: inherit; text-decoration: none; }
    nav a.activo { background: var(--mat-sys-secondary-container); }
    .marca { display: inline-flex; align-items: center; gap: 8px; color: inherit; text-decoration: none; font-size: 18px; }
    .marca mat-icon { color: var(--mat-sys-primary); }
    main { flex: 1; }
    .pie { border-top: 1px solid var(--sicat-borde); font-size: 14px; }
  `,
})
export class App {}
