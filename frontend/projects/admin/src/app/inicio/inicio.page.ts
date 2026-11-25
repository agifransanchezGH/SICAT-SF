import { DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';

import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { Avisos } from '../core/avisos';
import { Estadisticas } from '../core/modelos';

/** Una barra de la lista: nombre, cifra, proporción respecto del mayor y adónde lleva. */
interface Barra {
  texto: string;
  cantidad: number;
  porcentaje: number;
  enlace: unknown[];
  queryParams: Record<string, string>;
  clase?: string;
}

/**
 * Inicio del panel: las cifras para decidir qué hacer primero. Cada número es un enlace al listado filtrado que
 * lo explica (p. ej. "Borradores" abre los establecimientos en BORRADOR). Barras hechas con CSS, sin librerías.
 */
@Component({
  selector: 'app-inicio-page',
  imports: [RouterLink, DecimalPipe, MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    <section class="pagina">
      <div class="encabezado">
        <h1>Inicio</h1>
        <button mat-stroked-button type="button" (click)="cargar()" [disabled]="cargando()"><mat-icon>refresh</mat-icon> Actualizar</button>
      </div>
      @if (cargando() && !datos()) { <mat-progress-bar mode="indeterminate" aria-label="Cargando las cifras" /> }
      @if (error()) {
        <div class="tarjeta vacio" role="alert">
          <p>No se pudieron cargar las cifras.</p>
          <button mat-stroked-button type="button" (click)="cargar()">Reintentar</button>
        </div>
      }

      @if (datos(); as d) {
        <section class="pendientes" aria-labelledby="titulo-pendientes">
          <h2 id="titulo-pendientes">Para hacer</h2>
          <div class="fila-tarjetas">
            <a class="tarjeta cifra pendiente" routerLink="/lugares">
              <span class="numero">{{ d.pendientes.ubicacionesARevisar | number }}</span>
              <span class="rotulo">Ubicaciones a revisar</span>
              <span class="accion">Revisar en el mapa <mat-icon aria-hidden="true">arrow_forward</mat-icon></span>
            </a>
            <a class="tarjeta cifra pendiente" routerLink="/establecimientos" [queryParams]="{ listos: 'true' }">
              <span class="numero">{{ d.pendientes.listosParaPublicar | number }}</span>
              <span class="rotulo">Listos para publicar</span>
              <span class="accion">Publicarlos <mat-icon aria-hidden="true">arrow_forward</mat-icon></span>
            </a>
            @if (puedeVerDuplicados) {
              <a class="tarjeta cifra pendiente" routerLink="/duplicados">
                <span class="numero">{{ d.pendientes.posiblesDuplicados | number }}</span>
                <span class="rotulo">Grupos de posibles duplicados</span>
                <span class="accion">Compararlos <mat-icon aria-hidden="true">arrow_forward</mat-icon></span>
              </a>
            }
            <a class="tarjeta cifra pendiente" routerLink="/establecimientos" [queryParams]="{ contactosARevisar: 'true' }">
              <span class="numero">{{ d.pendientes.contactosARevisar | number }}</span>
              <span class="rotulo">Con contactos a revisar</span>
              <span class="accion">Corregirlos <mat-icon aria-hidden="true">arrow_forward</mat-icon></span>
            </a>
          </div>
        </section>

        <div class="dos-columnas">
          <section class="tarjeta bloque" aria-labelledby="titulo-estado">
            <div class="cabeza">
              <h2 id="titulo-estado">Establecimientos</h2>
              <a class="total" routerLink="/establecimientos">{{ d.establecimientos.activos | number }} activos</a>
            </div>
            <p class="ayuda">Estado de publicación de los establecimientos activos.</p>
            <ul class="barras">
              @for (b of estados(); track b.texto) {
                <li>
                  <a class="barra" [routerLink]="b.enlace" [queryParams]="b.queryParams">
                    <span class="barra-texto">{{ b.texto }}</span>
                    <span class="barra-cifra">{{ b.cantidad | number }}</span>
                    <span class="pista" aria-hidden="true"><span class="relleno" [class]="'relleno ' + (b.clase ?? '')" [style.width.%]="b.porcentaje"></span></span>
                  </a>
                </li>
              }
            </ul>
            <div class="mini-cifras">
              <a routerLink="/oferentes"><strong>{{ d.oferentes | number }}</strong> oferentes</a>
              <span><strong>{{ d.ofertasActivas | number }}</strong> ofertas activas</span>
              <a routerLink="/mapa"><strong>{{ d.circuitos | number }}</strong> {{ d.circuitos === 1 ? 'circuito' : 'circuitos' }}</a>
            </div>
          </section>

          <section class="tarjeta bloque" aria-labelledby="titulo-categorias">
            <div class="cabeza">
              <h2 id="titulo-categorias">Por categoría</h2>
            </div>
            <p class="ayuda">Establecimientos activos con una oferta activa en cada categoría (incluye sus subcategorías). Uno con ofertas en dos categorías cuenta en las dos.</p>
            @if (categorias().length) {
              <ul class="barras">
                @for (c of categorias(); track c.id) {
                  <li>
                    <a class="barra" routerLink="/establecimientos" [queryParams]="{ categoria: '' + c.id }">
                      <span class="barra-texto">{{ c.nombre }}</span>
                      <span class="barra-cifra">{{ c.establecimientos | number }}</span>
                      <span class="pista" aria-hidden="true"><span class="relleno" [style.width.%]="c.porcentaje"></span></span>
                    </a>
                    @if (c.subcategorias.length) {
                      <ul class="subcategorias" [attr.aria-label]="'Subcategorías de ' + c.nombre">
                        @for (s of c.subcategorias; track s.id) {
                          <li>
                            <a routerLink="/establecimientos" [queryParams]="{ categoria: '' + s.id }">
                              {{ s.nombre }} <strong>{{ s.establecimientos | number }}</strong>
                            </a>
                          </li>
                        }
                      </ul>
                    }
                  </li>
                }
              </ul>
            } @else {
              <p class="vacio">Todavía no hay categorías con establecimientos.</p>
            }
          </section>
        </div>
      }
    </section>
  `,
  styles: `
    h2 { margin: 0; font: 600 17px/1.3 Archivo, system-ui, sans-serif; font-stretch: var(--sicat-ancho-nombre); }
    .pendientes h2 { margin-bottom: 10px; }
    .fila-tarjetas { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 12px; }
    /* Cada cifra es un enlace: toda la tarjeta lleva al listado que la explica. */
    .cifra {
      display: flex; flex-direction: column; gap: 2px; padding: 14px 16px; color: inherit; text-decoration: none;
      transition: border-color 120ms var(--sicat-curva), background-color 120ms var(--sicat-curva);
    }
    .cifra:hover { border-color: var(--sicat-rio); background: var(--mat-sys-surface-container-lowest); }
    .cifra:focus-visible { outline: 2px solid var(--sicat-rio); outline-offset: 2px; }
    .numero {
      font: 700 32px/1.1 Archivo, system-ui, sans-serif; font-stretch: var(--sicat-ancho-titular);
      font-variant-numeric: tabular-nums; color: var(--sicat-tinta);
    }
    .rotulo { font-weight: 600; }
    .accion { display: inline-flex; align-items: center; gap: 4px; margin-top: 6px; font-size: 13px; color: var(--sicat-rio); }
    .accion mat-icon { font-size: 16px; width: 16px; height: 16px; }
    .dos-columnas { display: grid; grid-template-columns: repeat(auto-fit, minmax(340px, 1fr)); gap: 16px; align-items: start; }
    .bloque { padding: 16px 20px 18px; }
    .cabeza { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
    .total { font-weight: 600; font-variant-numeric: tabular-nums; }
    .ayuda { margin: 4px 0 12px; font-size: 13px; color: var(--sicat-texto-suave); max-width: 60ch; }
    .barras { list-style: none; margin: 0; padding: 0; display: grid; gap: 8px; }
    /* Barra: nombre y cifra arriba, la proporción abajo; todo es un enlace. */
    .barra {
      display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 2px 12px; padding: 6px 8px; margin: 0 -8px;
      border-radius: 8px; color: inherit; text-decoration: none;
    }
    .barra:hover { background: var(--mat-sys-surface-container-low); }
    .barra:hover .barra-texto { text-decoration: underline; }
    .barra:focus-visible { outline: 2px solid var(--sicat-rio); outline-offset: 0; }
    .barra-cifra { font-weight: 700; font-variant-numeric: tabular-nums; }
    .pista { grid-column: 1 / -1; height: 8px; border-radius: 4px; background: var(--mat-sys-surface-container-high); overflow: hidden; }
    /* Una cifra chica igual se ve (2px); cero no dibuja nada. */
    .relleno { display: block; height: 100%; border-radius: 4px; background: var(--sicat-rio); }
    .relleno:not([style*="width: 0%"]) { min-width: 2px; }
    .relleno.BORRADOR { background: #6b7c80; }
    .relleno.PUBLICADO { background: var(--sicat-ok); }
    .relleno.OCULTO { background: var(--sicat-error); }
    .subcategorias { list-style: none; margin: 2px 0 4px; padding: 0; display: flex; flex-wrap: wrap; gap: 4px 14px; font-size: 13px; }
    .subcategorias a { color: var(--sicat-texto-suave); }
    .subcategorias strong { color: var(--sicat-tinta); font-variant-numeric: tabular-nums; }
    .mini-cifras { display: flex; flex-wrap: wrap; gap: 6px 20px; margin-top: 16px; padding-top: 12px; border-top: 1px solid var(--sicat-borde); }
    .mini-cifras a, .mini-cifras span { color: var(--sicat-tinta); }
    .mini-cifras strong { font-variant-numeric: tabular-nums; }
    @media (prefers-reduced-motion: reduce) { .cifra { transition: none; } }
  `,
})
export class InicioPage {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  protected readonly puedeVerDuplicados = inject(AuthService).tieneRol('ADMIN', 'CARGADOR');

  protected readonly datos = signal<Estadisticas | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal(false);

  protected readonly estados = computed<Barra[]>(() => {
    const e = this.datos()?.establecimientos;
    if (!e) {
      return [];
    }
    const max = Math.max(e.borradores, e.publicados, e.ocultos, 1);
    const barra = (texto: string, cantidad: number, estado: string): Barra => ({
      texto, cantidad, porcentaje: (cantidad / max) * 100, enlace: ['/establecimientos'], queryParams: { estado }, clase: estado,
    });
    return [barra('Publicados', e.publicados, 'PUBLICADO'), barra('Borradores', e.borradores, 'BORRADOR'), barra('Ocultos', e.ocultos, 'OCULTO')];
  });

  protected readonly categorias = computed(() => {
    const lista = this.datos()?.categorias ?? [];
    const max = Math.max(...lista.map((c) => c.establecimientos), 1);
    return lista.map((c) => ({ ...c, porcentaje: (c.establecimientos / max) * 100 }));
  });

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.api.get<Estadisticas>('/estadisticas').subscribe({
      next: (d) => {
        this.datos.set(d);
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        this.error.set(true);
        this.avisos.error(e);
      },
    });
  }
}
