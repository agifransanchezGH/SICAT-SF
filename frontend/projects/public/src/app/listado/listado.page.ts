import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EMPTY, catchError, finalize, map, of, switchMap } from 'rxjs';
import { EstablecimientoResumen, Pagina, Puente, nombreLegible } from 'shared';

import { ApiPublicaService } from '../core/api-publica.service';
import { FiltrosBusqueda } from '../core/filtros-busqueda';

/** Buscador del sitio público: texto (nombre y categorías), categoría y localidad. Los filtros viven en la URL. */
@Component({
  selector: 'app-listado-page',
  imports: [RouterLink, FiltrosBusqueda, Puente, MatIconModule, MatButtonModule, MatPaginatorModule, MatProgressBarModule],
  template: `
    <section class="heroe">
      <div class="contenido">
        <h1 class="titular">Qué hacer en Santa Fe</h1>
        <p class="bajada">Museos, paseos por la costanera, dónde comer y dónde dormir, con cómo llegar a cada lugar.</p>
        <div class="panel-busqueda"><app-filtros-busqueda /></div>
        @if (atajos().length) {
          <nav class="atajos" aria-label="Categorías">
            @for (c of atajos(); track c.id) {
              <a [routerLink]="[]" [queryParams]="{ categoria: '' + c.id, pagina: null }" queryParamsHandling="merge"
                 [class.activo]="categoriaActual() === '' + c.id" [attr.aria-current]="categoriaActual() === '' + c.id ? 'true' : null">{{ c.nombre }}</a>
            }
          </nav>
        }
      </div>
      <sicat-puente class="puente" />
    </section>

    <section class="contenido" aria-live="polite" [attr.aria-busy]="cargando()">
      @if (cargando()) { <mat-progress-bar mode="indeterminate" aria-label="Cargando resultados" /> }
      @if (pagina(); as p) {
        <div class="resultado">
          <h2 class="cantidad">{{ p.totalElementos === 1 ? '1 lugar' : p.totalElementos + ' lugares' }}</h2>
          <a class="ver-mapa" routerLink="/mapa" queryParamsHandling="merge" [queryParams]="{ pagina: null }"><mat-icon aria-hidden="true">map</mat-icon> Ver en el mapa</a>
        </div>
        <ul class="lista">
          @for (e of p.contenido; track e.id) {
            <li>
              <a class="item" [routerLink]="['/establecimientos', e.id]" [state]="{ nombre: e.nombre }">
                @if (e.categorias.length) { <span class="categoria">{{ e.categorias.join(', ') }}</span> }
                <h3 class="nombre-lugar nombre" [style.view-transition-name]="'nombre-' + e.id">{{ legible(e.nombre) }}</h3>
                @if (e.direccion || e.localidad) { <span class="direccion">{{ unir(e.direccion, e.localidad) }}</span> }
                @if (e.destacado) { <span class="destacado"><mat-icon inline aria-hidden="true">star</mat-icon> Destacado</span> }
              </a>
            </li>
          } @empty {
            <li class="vacio">
              <p>No hay lugares con esa búsqueda.</p>
              <a routerLink="/">Ver todos los lugares</a>
            </li>
          }
        </ul>
        @if (p.totalPaginas > 1) {
          <mat-paginator [length]="p.totalElementos" [pageIndex]="p.pagina" [pageSize]="p.tamanio" [hidePageSize]="true"
                         (page)="paginar($event)" />
        }
      } @else if (error()) {
        <p class="vacio">No se pudo cargar la información. Volvé a intentar en unos minutos.</p>
      }
    </section>
  `,
  styles: `
    .heroe { position: relative; background: var(--sicat-rio); color: #fff; overflow: hidden; }
    .heroe .contenido { position: relative; z-index: 1; padding-top: 48px; padding-bottom: 128px; }
    .heroe h1 { font-size: clamp(42px, 7vw, 84px); max-width: 11ch; }
    .bajada { margin: 16px 0 24px; font-size: 18px; line-height: 1.45; color: var(--sicat-rio-claro); max-width: 52ch; }
    .panel-busqueda { background: var(--mat-sys-surface); color: var(--sicat-tinta); border-radius: 8px; padding: 16px 16px 4px; max-width: 820px; box-shadow: 0 12px 32px rgb(6 30 33 / 28%); }
    .panel-busqueda app-filtros-busqueda { display: block; padding-bottom: 12px; }
    .atajos { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 16px; max-width: 820px; }
    .atajos a { color: #fff; text-decoration: none; font-size: 14px; padding: 5px 12px; border: 1px solid rgb(255 255 255 / 40%); border-radius: 999px; transition: background var(--sicat-duracion) var(--sicat-curva); }
    .atajos a:hover { background: rgb(255 255 255 / 12%); }
    .atajos a.activo { background: #fff; color: var(--sicat-rio); border-color: #fff; }
    .atajos a:focus-visible { outline-color: #fff; }
    .puente { position: absolute; left: 0; right: 0; bottom: 0; max-height: 128px; color: #fff; opacity: 0.6; }
    .resultado { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; margin: 4px 0 8px; }
    .cantidad { margin: 0; font-size: 15px; font-weight: 500; color: var(--sicat-texto-suave); }
    .ver-mapa { display: inline-flex; align-items: center; gap: 6px; font-weight: 600; text-decoration: none; }
    .ver-mapa:hover { text-decoration: underline; }
    .lista { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); column-gap: 40px; }
    .item { display: flex; flex-direction: column; gap: 2px; padding: 18px 0 20px; border-top: 1px solid var(--sicat-borde); color: inherit; text-decoration: none; height: 100%; box-sizing: border-box; }
    .item:hover .nombre { color: var(--sicat-rio); text-decoration: underline; text-decoration-thickness: 1px; text-underline-offset: 0.15em; }
    .categoria { font-size: 14px; font-weight: 600; color: var(--sicat-rio); }
    .nombre { font-size: 21px; margin: 2px 0 4px; }
    .categoria { order: 0; }
    .direccion { font-size: 15px; color: var(--sicat-texto-suave); }
    .destacado { margin-top: 6px; font-size: 14px; font-weight: 600; color: var(--sicat-limo-texto); }
    .destacado mat-icon { vertical-align: -2px; }
    .vacio { grid-column: 1 / -1; border-top: 1px solid var(--sicat-borde); }
    .vacio p { margin: 0 0 8px; color: var(--sicat-tinta); font-size: 17px; }
    mat-paginator { margin-top: 24px; background: transparent; }
    @media (max-width: 600px) {
      .heroe .contenido { padding-top: 32px; padding-bottom: 56px; }

      /* En el celular los atajos van en una sola fila que se desplaza */
      .atajos { flex-wrap: nowrap; overflow-x: auto; margin-inline: -16px; padding: 2px 16px 6px; scrollbar-width: none; }
      .atajos a { flex: none; }
    }
  `,
})
export class ListadoPage {
  private readonly api = inject(ApiPublicaService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly pagina = signal<Pagina<EstablecimientoResumen> | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal(false);
  protected readonly unir = (...partes: (string | undefined)[]) => partes.filter(Boolean).join(', ');
  protected readonly legible = nombreLegible;
  /** Atajos de la portada: las categorías de primer nivel. */
  protected readonly atajos = toSignal(this.api.categorias().pipe(catchError(() => of([]))), { initialValue: [] });
  protected readonly categoriaActual = toSignal(this.route.queryParamMap.pipe(map((p) => p.get('categoria'))));

  constructor() {
    inject(Title).setTitle('Qué hacer en Santa Fe');
    this.route.queryParamMap
      .pipe(
        switchMap((p) => {
          this.cargando.set(true);
          this.error.set(false);
          return this.api
            .listar({
              q: p.get('q'),
              categoria: p.get('categoria'),
              localidad: p.get('localidad'),
              page: Number(p.get('pagina') ?? 0),
              size: 24,
            })
            .pipe(
              catchError(() => {
                this.error.set(true);
                this.pagina.set(null);
                return EMPTY;
              }),
              finalize(() => this.cargando.set(false)),
            );
        }),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe((r) => this.pagina.set(r));
  }

  protected paginar(e: PageEvent): void {
    this.router.navigate([], { queryParams: { pagina: e.pageIndex || null }, queryParamsHandling: 'merge' });
    window.scrollTo({ top: 0 });
  }
}
