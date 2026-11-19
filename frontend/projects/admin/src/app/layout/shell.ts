import { BreakpointObserver } from '@angular/cdk/layout';
import { DOCUMENT } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, fromEvent, map } from 'rxjs';

import { AuthService, Rol } from '../core/auth.service';
import { Contadores } from '../core/contadores';
import { ContadoresPanel } from '../core/modelos';

/** Pendiente de trabajo que se muestra junto a un ítem: cuántos hay y qué son (para lectores de pantalla). */
interface Pendiente {
  contador: keyof ContadoresPanel;
  /** "a revisar", "grupos": completa "42 a revisar". */
  que: string;
}

interface ItemMenu {
  ruta: string;
  texto: string;
  icono: string;
  roles?: Rol[];
  pendiente?: Pendiente;
  /** Accesos directos a una cola de la sección (se muestran debajo, con sangría). */
  colas?: { texto: string; queryParams: Record<string, string>; pendiente: Pendiente }[];
  /** Separa el flujo de revisión del resto de las secciones. */
  separadorDespues?: boolean;
}

const CLAVE_MENU = 'sicat.menuCompacto';

function leerPreferencia(): boolean {
  try {
    return localStorage.getItem(CLAVE_MENU) === '1';
  } catch {
    return false;
  }
}

@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatSidenavModule, MatListModule, MatIconModule, MatButtonModule, MatTooltipModule,
  ],
  template: `
    <mat-toolbar class="barra">
      @if (movil()) {
        <button mat-icon-button aria-label="Menú" aria-controls="menu-lateral" [attr.aria-expanded]="nav.opened" (click)="nav.toggle()">
          <mat-icon>menu</mat-icon>
        </button>
      } @else {
        <!-- Contraer / expandir el menú lateral: el contenido toma el ancho que queda libre -->
        <button mat-icon-button aria-controls="menu-lateral" [attr.aria-expanded]="!compacto()"
                [attr.aria-label]="compacto() ? 'Expandir menú' : 'Contraer menú'"
                [matTooltip]="compacto() ? 'Expandir menú' : 'Contraer menú'" (click)="alternarMenu()">
          <mat-icon>{{ compacto() ? 'menu' : 'menu_open' }}</mat-icon>
        </button>
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
    <!-- autosize: al contraer o expandir el menú, el contenido se acomoda al nuevo ancho -->
    <mat-sidenav-container class="contenedor" autosize>
      <mat-sidenav #nav id="menu-lateral" [mode]="movil() ? 'over' : 'side'" [opened]="!movil()" class="lateral"
                   [class.compacto]="compacto() && !movil()">
        <mat-nav-list aria-label="Secciones del panel">
          @for (item of menu(); track item.ruta) {
            <a mat-list-item [routerLink]="item.ruta" routerLinkActive #rla="routerLinkActive"
               [routerLinkActiveOptions]="{ paths: 'subset', queryParams: 'ignored', matrixParams: 'ignored', fragment: 'ignored' }"
               [activated]="rla.isActive" (click)="movil() && nav.close()"
               [matTooltip]="tooltip(item)" matTooltipPosition="right" [matTooltipDisabled]="!compacto() || movil()">
              <mat-icon matListItemIcon aria-hidden="true">{{ item.icono }}</mat-icon>
              <span matListItemTitle class="titulo">
                <span class="texto">{{ item.texto }}</span>
                @if (item.pendiente && cantidad(item.pendiente); as n) {
                  <span class="contador"><span aria-hidden="true">{{ n }}</span><span class="cdk-visually-hidden">, {{ n }} {{ item.pendiente.que }}</span></span>
                }
              </span>
            </a>
            @for (cola of item.colas ?? []; track cola.texto) {
              @if (cantidad(cola.pendiente); as n) {
                <a mat-list-item class="cola" [routerLink]="item.ruta" [queryParams]="cola.queryParams"
                   routerLinkActive #rlaCola="routerLinkActive" [activated]="rlaCola.isActive" ariaCurrentWhenActive="page"
                   [routerLinkActiveOptions]="{ paths: 'exact', queryParams: 'subset', matrixParams: 'ignored', fragment: 'ignored' }"
                   (click)="movil() && nav.close()">
                  <span matListItemTitle class="titulo">
                    <span class="texto">{{ cola.texto }}</span>
                    <span class="contador"><span aria-hidden="true">{{ n }}</span><span class="cdk-visually-hidden">, {{ n }} {{ cola.pendiente.que }}</span></span>
                  </span>
                </a>
              }
            }
            @if (item.separadorDespues) { <div class="separador" role="separator"></div> }
          }
        </mat-nav-list>
        @if (!movil()) {
          <button type="button" class="contraer" aria-controls="menu-lateral" [attr.aria-expanded]="!compacto()"
                  [matTooltip]="'Expandir menú'" matTooltipPosition="right" [matTooltipDisabled]="!compacto()"
                  (click)="alternarMenu()">
            <mat-icon aria-hidden="true">{{ compacto() ? 'keyboard_double_arrow_right' : 'keyboard_double_arrow_left' }}</mat-icon>
            <span class="texto-contraer">{{ compacto() ? 'Expandir menú' : 'Contraer menú' }}</span>
          </button>
        }
      </mat-sidenav>
      <mat-sidenav-content class="contenido">
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
    /* Lo que queda fijo arriba (checklist de la ficha) o abajo (barra de selección) no tapa al foco: al
       recorrer con Tab, el navegador deja ese margen al traer el elemento a la vista (WCAG 2.4.11). */
    .contenido { scroll-padding-block: 120px 96px; }
    /* Menú en tinta de río: separa la herramienta del contenido sin competir con él */
    .lateral {
      width: 292px; border-right: 0; border-radius: 0; background: var(--sicat-tinta);
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
    /* Título y contador en la misma línea; si no entran (p. ej. con barra de scroll), el texto pasa a dos líneas en vez de cortarse */
    .titulo { display: flex !important; align-items: center; gap: 6px; white-space: normal !important; }
    .titulo .texto { flex: 1; min-width: 0; line-height: 1.2; }
    /* Pendientes: tinta sobre ocre (contraste 4,6:1), números tabulares para que no bailen al cambiar */
    .contador {
      flex: none; min-width: 24px; height: 22px; padding: 0 7px; box-sizing: border-box; border-radius: 11px;
      display: inline-flex; align-items: center; justify-content: center;
      background: var(--sicat-limo); color: var(--sicat-tinta);
      font-size: 12px; font-weight: 700; font-variant-numeric: tabular-nums; line-height: 1;
    }
    /* Colas de una sección: sangría hasta el texto del ítem, sin ícono, un poco más chicas */
    .lateral a.cola {
      --mat-list-list-item-one-line-container-height: 36px;
      --mat-list-list-item-label-text-size: 13.5px;
      padding-left: 40px;
    }
    /* En las colas, solo contorno: el ocre aclarado da 8,4:1 sobre tinta y el borde 3,4:1 */
    .lateral a.cola .contador { background: transparent; color: color-mix(in srgb, var(--sicat-limo) 55%, white); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--sicat-limo) 80%, var(--sicat-tinta)); }
    .separador { height: 1px; margin: 10px 12px; background: rgb(255 255 255 / 14%); }
    .lateral ::ng-deep .mat-drawer-inner-container { display: flex; flex-direction: column; }
    .lateral mat-nav-list { flex: 1; }
    /* Botón al pie del menú, con texto: la forma evidente de contraerlo (y de volver a abrirlo) */
    .contraer {
      display: flex; align-items: center; gap: 12px; margin: 8px; min-height: 44px; padding: 0 16px;
      border: 1px solid rgb(255 255 255 / 22%); border-radius: 8px; background: transparent;
      color: #c9d6d8; font: inherit; font-size: 14px; cursor: pointer;
    }
    .contraer:hover { background: rgb(255 255 255 / 8%); color: #ffffff; }
    .contraer:focus-visible { outline: 2px solid #ffffff; outline-offset: 2px; }

    /* Menú contraído: solo íconos (con su nombre en un tooltip y para lectores de pantalla) */
    .lateral.compacto { width: 76px; }
    .compacto .titulo .texto, .compacto .texto-contraer {
      position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap;
    }
    .compacto a.cola { display: none; }
    .compacto mat-nav-list { padding: 12px 6px; }
    .compacto a.mdc-list-item { padding-left: 14px; padding-right: 0; }
    .compacto .titulo { position: static; }
    /* El contador queda como insignia sobre el ícono */
    .compacto .contador {
      position: absolute; top: 4px; left: 32px; min-width: 18px; height: 18px; padding: 0 4px; border-radius: 9px; font-size: 11px;
    }
    .compacto .contraer { justify-content: center; padding: 0; margin: 8px 12px; }
    .compacto .separador { margin: 10px 8px; }
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

  /** Primero el flujo de trabajo con lo importado (importar → ubicar → publicar → depurar); después, el resto. */
  private readonly items: ItemMenu[] = [
    { ruta: 'importacion', texto: 'Importación', icono: 'upload_file', roles: ['ADMIN'] },
    {
      ruta: 'lugares', texto: 'Lugares y ubicaciones', icono: 'place',
      pendiente: { contador: 'ubicacionesARevisar', que: 'ubicaciones a revisar' },
    },
    {
      ruta: 'establecimientos', texto: 'Establecimientos', icono: 'storefront',
      colas: [
        {
          texto: 'Listos para publicar', queryParams: { listos: 'true' },
          pendiente: { contador: 'listosParaPublicar', que: 'establecimientos' },
        },
        {
          texto: 'Contactos a revisar', queryParams: { contactosARevisar: 'true' },
          pendiente: { contador: 'contactosARevisar', que: 'establecimientos' },
        },
      ],
    },
    {
      ruta: 'duplicados', texto: 'Posibles duplicados', icono: 'content_copy', roles: ['ADMIN', 'CARGADOR'],
      pendiente: { contador: 'posiblesDuplicados', que: 'grupos' }, separadorDespues: true,
    },
    { ruta: 'oferentes', texto: 'Oferentes', icono: 'badge' },
    { ruta: 'mapa', texto: 'Mapa', icono: 'map' },
    { ruta: 'catalogos', texto: 'Catálogos', icono: 'list_alt' },
    { ruta: 'categorias', texto: 'Categorías', icono: 'account_tree' },
    { ruta: 'usuarios', texto: 'Usuarios', icono: 'group', roles: ['ADMIN'] },
    { ruta: 'auditoria', texto: 'Auditoría', icono: 'history', roles: ['ADMIN'] },
  ];

  protected readonly menu = computed(() =>
    this.items.filter((i) => !i.roles || this.auth.tieneRol(...i.roles)),
  );

  private readonly contadores = inject(Contadores);

  /** Menú contraído a íconos (en pantallas anchas). Se recuerda en este navegador. */
  protected readonly compacto = signal(leerPreferencia());

  protected alternarMenu(): void {
    this.compacto.update((c) => !c);
    try {
      localStorage.setItem(CLAVE_MENU, this.compacto() ? '1' : '0');
    } catch {
      // sin almacenamiento (modo privado): vale solo para esta visita
    }
  }

  /** Tooltip del ítem contraído: el nombre y, si hay, los pendientes ("Lugares y ubicaciones · 582 a revisar"). */
  protected tooltip(item: ItemMenu): string {
    const n = item.pendiente ? this.cantidad(item.pendiente) : null;
    return n ? `${item.texto} · ${n} ${item.pendiente!.que}` : item.texto;
  }

  /** Cantidad pendiente, o null si es 0 o todavía no se sabe (no se muestra nada). */
  protected cantidad(p: Pendiente): number | null {
    return this.contadores.valores()?.[p.contador] || null;
  }

  constructor() {
    const destroy = inject(DestroyRef);
    // Se refrescan al cambiar de página y al volver a la pestaña; tras cada acción, por el interceptor.
    inject(Router).events.pipe(filter((e) => e instanceof NavigationEnd), takeUntilDestroyed(destroy))
      .subscribe(() => this.contadores.refrescar());
    const documento = inject(DOCUMENT);
    fromEvent(documento, 'visibilitychange')
      .pipe(filter(() => documento.visibilityState === 'visible'), takeUntilDestroyed(destroy))
      .subscribe(() => this.contadores.refrescar());
    this.contadores.refrescar();
  }
}
