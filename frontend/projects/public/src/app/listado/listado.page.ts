import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EMPTY, catchError, finalize, switchMap } from 'rxjs';
import { EstablecimientoResumen, Pagina } from 'shared';

import { ApiPublicaService } from '../core/api-publica.service';
import { FiltrosBusqueda } from '../core/filtros-busqueda';

/** Buscador del sitio público: texto (nombre y categorías), categoría y localidad. Los filtros viven en la URL. */
@Component({
  selector: 'app-listado-page',
  imports: [RouterLink, FiltrosBusqueda, MatIconModule, MatButtonModule, MatPaginatorModule, MatProgressBarModule],
  template: `
    <section class="portada">
      <div class="contenido">
        <h1>¿Qué querés hacer en Santa Fe?</h1>
        <p class="suave">Alojamientos, gastronomía, museos, paseos y mucho más.</p>
        <app-filtros-busqueda />
      </div>
    </section>

    <section class="contenido">
      @if (cargando()) { <mat-progress-bar mode="indeterminate" /> }
      @if (pagina(); as p) {
        <div class="resultado">
          <span class="suave">{{ p.totalElementos === 1 ? '1 resultado' : p.totalElementos + ' resultados' }}</span>
          <a mat-stroked-button routerLink="/mapa" queryParamsHandling="merge" [queryParams]="{ pagina: null }"><mat-icon>map</mat-icon> Ver en el mapa</a>
        </div>
        <div class="grilla">
          @for (e of p.contenido; track e.id) {
            <a class="tarjeta item" [routerLink]="['/establecimientos', e.id]">
              <div class="titulo">
                <h2>{{ e.nombre }}</h2>
                @if (e.destacado) { <mat-icon class="destacado" title="Destacado">star</mat-icon> }
              </div>
              @if (e.categorias.length) { <p class="categorias">{{ e.categorias.join(' · ') }}</p> }
              @if (e.direccion || e.localidad) {
                <p class="suave lugar"><mat-icon inline>place</mat-icon> {{ unir(e.direccion, e.localidad) }}</p>
              }
            </a>
          } @empty {
            <p class="vacio tarjeta">No encontramos resultados. Probá con otra palabra o quitá algún filtro.</p>
          }
        </div>
        @if (p.totalPaginas > 1) {
          <mat-paginator [length]="p.totalElementos" [pageIndex]="p.pagina" [pageSize]="p.tamanio" [hidePageSize]="true"
                         (page)="paginar($event)" />
        }
      } @else if (error()) {
        <p class="vacio tarjeta">No pudimos cargar la información. Intentá de nuevo en unos minutos.</p>
      }
    </section>
  `,
  styles: `
    .portada { background: var(--mat-sys-surface); border-bottom: 1px solid var(--sicat-borde); }
    .portada .contenido { padding-bottom: 12px; }
    h1 { margin: 8px 0 4px; font: var(--mat-sys-headline-medium); }
    .portada p { margin: 0 0 16px; }
    .resultado { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin: 0 0 12px; }
    .grilla { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
    .grilla .vacio { grid-column: 1 / -1; }
    .item { display: flex; flex-direction: column; gap: 6px; padding: 16px; color: inherit; text-decoration: none; transition: box-shadow .15s; }
    .item:hover, .item:focus-visible { box-shadow: 0 2px 10px rgb(0 0 0 / 10%); }
    .titulo { display: flex; align-items: flex-start; gap: 6px; }
    h2 { margin: 0; font: var(--mat-sys-title-medium); flex: 1; }
    .destacado { color: var(--sicat-acento); }
    .categorias { margin: 0; color: var(--mat-sys-primary); font-size: 14px; }
    .lugar { margin: 0; font-size: 14px; }
    .lugar mat-icon { vertical-align: -2px; }
    mat-paginator { margin-top: 16px; background: transparent; }
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

  constructor() {
    inject(Title).setTitle('Santa Fe · Oferta turística');
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
