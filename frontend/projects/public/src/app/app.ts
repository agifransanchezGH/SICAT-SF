import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <a class="saltar" href="#principal">Ir al contenido</a>
    <header class="barra">
      <div class="interior">
        <a routerLink="/" class="marca" aria-label="Santa Fe, qué hacer: inicio">Santa Fe <span>Qué hacer</span></a>
        <nav aria-label="Secciones">
          <a routerLink="/" routerLinkActive="activo" ariaCurrentWhenActive="page"
             [routerLinkActiveOptions]="{ paths: 'exact', queryParams: 'ignored', matrixParams: 'ignored', fragment: 'ignored' }">Buscar</a>
          <a routerLink="/mapa" routerLinkActive="activo" ariaCurrentWhenActive="page">Mapa</a>
        </nav>
      </div>
    </header>
    <main id="principal" tabindex="-1"><router-outlet /></main>
    <footer class="pie">
      <div class="interior">Municipalidad de la Ciudad de Santa Fe. Dirección de Turismo.</div>
    </footer>
  `,
  styles: `
    :host { display: flex; flex-direction: column; min-height: 100vh; }
    .saltar { position: absolute; left: 8px; top: -48px; z-index: 10; padding: 8px 12px; background: var(--sicat-tinta); color: #fff; border-radius: 4px; }
    .saltar:focus { top: 8px; }
    .barra { background: var(--mat-sys-surface); border-bottom: 1px solid var(--sicat-borde); }
    .interior { max-width: 1120px; margin: 0 auto; padding: 0 16px; box-sizing: border-box; }
    .barra .interior { display: flex; align-items: center; gap: 16px; min-height: 56px; }
    .marca { color: var(--sicat-tinta); text-decoration: none; font-size: 19px; font-weight: 800; font-stretch: var(--sicat-ancho-titular); letter-spacing: -0.01em; }
    .marca span { font-weight: 400; font-stretch: 100%; color: var(--sicat-texto-suave); margin-left: 4px; letter-spacing: 0; }
    nav { margin-left: auto; display: flex; gap: 20px; }
    nav a { color: var(--sicat-tinta); text-decoration: none; font-weight: 500; padding: 17px 0 15px; border-bottom: 2px solid transparent; }
    nav a:hover { border-bottom-color: var(--sicat-borde); }
    nav a.activo { border-bottom-color: var(--sicat-rio); color: var(--sicat-rio); }
    main { flex: 1; outline: none; }
    .pie { border-top: 1px solid var(--sicat-borde); font-size: 14px; color: var(--sicat-texto-suave); }
    .pie .interior { padding-top: 20px; padding-bottom: 24px; }
  `,
})
export class App {}
