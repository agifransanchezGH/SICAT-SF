import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EMPTY, catchError, debounceTime, finalize, map, of, switchMap } from 'rxjs';
import { CategoriaPublica, EstablecimientoResumen, Pagina } from 'shared';

import { ApiPublicaService } from '../core/api-publica.service';

interface OpcionCategoria {
  id: number;
  nombre: string;
  nivel: number;
}

function aplanar(nodos: CategoriaPublica[], nivel = 0): OpcionCategoria[] {
  return nodos.flatMap((n) => [{ id: n.id, nombre: n.nombre, nivel }, ...aplanar(n.hijos ?? [], nivel + 1)]);
}

/** Buscador del sitio público: texto (nombre y categorías), categoría y localidad. Los filtros viven en la URL. */
@Component({
  selector: 'app-listado-page',
  imports: [
    ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatSelectModule, MatIconModule,
    MatButtonModule, MatPaginatorModule, MatProgressBarModule,
  ],
  template: `
    <section class="portada">
      <div class="contenido">
        <h1>¿Qué querés hacer en Santa Fe?</h1>
        <p class="suave">Alojamientos, gastronomía, museos, paseos y mucho más.</p>
        <form class="filtros" [formGroup]="filtros" (ngSubmit)="$event.preventDefault()">
          <mat-form-field class="busqueda">
            <mat-label>Buscar</mat-label>
            <mat-icon matPrefix>search</mat-icon>
            <input matInput formControlName="q" placeholder="Ej.: parrilla, museo, hotel" />
            @if (filtros.controls.q.value) {
              <button matSuffix mat-icon-button type="button" aria-label="Borrar búsqueda" (click)="filtros.controls.q.setValue('')"><mat-icon>close</mat-icon></button>
            }
          </mat-form-field>
          <mat-form-field>
            <mat-label>Categoría</mat-label>
            <mat-select formControlName="categoria">
              <mat-option value="">Todas</mat-option>
              @for (c of categorias(); track c.id) {
                <mat-option [value]="'' + c.id" [class.subcategoria]="c.nivel > 0">{{ c.nombre }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          @if (localidades().length > 1) {
            <mat-form-field>
              <mat-label>Localidad</mat-label>
              <mat-select formControlName="localidad">
                <mat-option value="">Todas</mat-option>
                @for (l of localidades(); track l.id) { <mat-option [value]="'' + l.id">{{ l.nombre }}</mat-option> }
              </mat-select>
            </mat-form-field>
          }
        </form>
      </div>
    </section>

    <section class="contenido">
      @if (cargando()) { <mat-progress-bar mode="indeterminate" /> }
      @if (pagina(); as p) {
        <p class="suave resultado">{{ p.totalElementos === 1 ? '1 resultado' : p.totalElementos + ' resultados' }}</p>
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
    .filtros { display: flex; gap: 12px; flex-wrap: wrap; }
    .filtros mat-form-field { flex: 1 1 200px; }
    .filtros .busqueda { flex: 3 1 300px; }
    .resultado { margin: 0 0 12px; }
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
    ::ng-deep .subcategoria { padding-left: 32px !important; }
  `,
})
export class ListadoPage {
  private readonly api = inject(ApiPublicaService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly unir = (...partes: (string | undefined)[]) => partes.filter(Boolean).join(", ");
  protected readonly categorias = toSignal(this.api.categorias().pipe(map((a) => aplanar(a)), catchError(() => of([]))), {
    initialValue: [] as OpcionCategoria[],
  });
  protected readonly localidades = toSignal(this.api.localidades().pipe(catchError(() => of([]))), { initialValue: [] });
  protected readonly pagina = signal<Pagina<EstablecimientoResumen> | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal(false);

  private readonly inicial = this.route.snapshot.queryParamMap;
  protected readonly filtros = inject(NonNullableFormBuilder).group({
    q: this.inicial.get('q') ?? '',
    categoria: this.inicial.get('categoria') ?? '',
    localidad: this.inicial.get('localidad') ?? '',
  });

  constructor() {
    const destroy = inject(DestroyRef);
    inject(Title).setTitle("Santa Fe · Oferta turística");
    this.filtros.valueChanges.pipe(debounceTime(350), takeUntilDestroyed(destroy)).subscribe((f) =>
      this.router.navigate([], {
        queryParams: { q: f.q || null, categoria: f.categoria || null, localidad: f.localidad || null, pagina: null },
        queryParamsHandling: 'merge',
        replaceUrl: true,
      }),
    );

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
        takeUntilDestroyed(destroy),
      )
      .subscribe((r) => this.pagina.set(r));
  }

  protected paginar(e: PageEvent): void {
    this.router.navigate([], { queryParams: { pagina: e.pageIndex || null }, queryParamsHandling: 'merge' });
    window.scrollTo({ top: 0 });
  }
}
