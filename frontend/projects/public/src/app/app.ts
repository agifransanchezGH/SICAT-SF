import { Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, MatIconModule],
  template: `
    <header class="barra">
      <div class="interior">
        <a routerLink="/" class="marca">
          <mat-icon>travel_explore</mat-icon>
          <span><strong>Santa Fe</strong> · Oferta turística</span>
        </a>
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
    .marca { display: inline-flex; align-items: center; gap: 8px; color: inherit; text-decoration: none; font-size: 18px; }
    .marca mat-icon { color: var(--mat-sys-primary); }
    main { flex: 1; }
    .pie { border-top: 1px solid var(--sicat-borde); font-size: 14px; }
  `,
})
export class App {}
