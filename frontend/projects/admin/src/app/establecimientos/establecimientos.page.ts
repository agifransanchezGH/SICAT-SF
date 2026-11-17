import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { ActivatedRoute, Router, RouterLink, convertToParamMap } from '@angular/router';
import { EMPTY, catchError, debounceTime, finalize, skip, switchMap } from 'rxjs';

import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { Avisos } from '../core/avisos';
import { Catalogos } from '../core/catalogos';
import { EstadoChip } from '../core/estado';
import { FiltrosRecordados } from '../core/filtros-recordados';
import { ESTADOS_PUBLICACION, EstablecimientoResumen, Pagina } from '../core/modelos';

@Component({
  selector: 'app-establecimientos-page',
  imports: [
    ReactiveFormsModule, RouterLink, EstadoChip,
    MatTableModule, MatPaginatorModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatCheckboxModule, MatButtonModule, MatIconModule, MatProgressBarModule,
  ],
  template: `
    <section class="pagina">
      <div class="encabezado">
        <h1>Establecimientos</h1>
        @if (puedeEditar) {
          <a mat-flat-button routerLink="nuevo"><mat-icon>add</mat-icon> Nuevo</a>
        }
      </div>

      <form class="filtros" [formGroup]="filtros">
        <mat-form-field class="busqueda">
          <mat-label>Buscar por nombre</mat-label>
          <mat-icon matPrefix>search</mat-icon>
          <input matInput formControlName="q" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Estado</mat-label>
          <mat-select formControlName="estado">
            <mat-option value="">Todos</mat-option>
            @for (e of estados; track e) {
              <mat-option [value]="e"><app-estado [valor]="e" /></mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Categoría</mat-label>
          <mat-select formControlName="categoria">
            <mat-option value="">Todas</mat-option>
            @for (c of catalogos.categorias(); track c.id) {
              <mat-option [value]="'' + c.id">{{ c.nombre }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Localidad</mat-label>
          <mat-select formControlName="localidadId">
            <mat-option value="">Todas</mat-option>
            @for (l of localidades(); track l.id) {
              <mat-option [value]="'' + l.id">{{ l.nombre }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Ubicación</mat-label>
          <mat-select formControlName="ubicacion" multiple>
            @for (u of ubicaciones; track u.valor) { <mat-option [value]="u.valor">{{ u.texto }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-checkbox formControlName="incluirInactivos">Incluir dados de baja</mat-checkbox>
      </form>

      <div class="tarjeta">
        @if (cargando()) {
          <mat-progress-bar mode="indeterminate" />
        }
        <table mat-table [dataSource]="pagina()?.contenido ?? []" class="tabla">
          <ng-container matColumnDef="nombre">
            <th mat-header-cell *matHeaderCellDef>Nombre</th>
            <td mat-cell *matCellDef="let e">
              <strong>{{ e.nombre }}</strong>
              @if (e.destacado) { <mat-icon class="destacado" inline title="Destacado">star</mat-icon> }
              <div class="suave">{{ e.oferente }}</div>
            </td>
          </ng-container>
          <ng-container matColumnDef="categorias">
            <th mat-header-cell *matHeaderCellDef>Categorías</th>
            <td mat-cell *matCellDef="let e">{{ e.categorias.join(', ') }}</td>
          </ng-container>
          <ng-container matColumnDef="localidad">
            <th mat-header-cell *matHeaderCellDef>Localidad</th>
            <td mat-cell *matCellDef="let e">{{ e.localidad }}</td>
          </ng-container>
          <ng-container matColumnDef="ubicacion">
            <th mat-header-cell *matHeaderCellDef>Ubicación</th>
            <td mat-cell *matCellDef="let e">
              @if (e.estadoGeo) { <app-estado [valor]="e.estadoGeo" /> }
              @else if (!e.coordenadas) { <span class="suave">Sin ubicación</span> }
            </td>
          </ng-container>
          <ng-container matColumnDef="estado">
            <th mat-header-cell *matHeaderCellDef>Estado</th>
            <td mat-cell *matCellDef="let e">
              <app-estado [valor]="e.estadoPublicacion" />
              @if (!e.activo) { <app-estado valor="inactivo" /> }
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let e; columns: columnas" class="fila-link" [routerLink]="['/establecimientos', e.id]"></tr>
        </table>
        @if (pagina()?.contenido?.length === 0) {
          <p class="vacio">No hay establecimientos con esos filtros.</p>
        }
        <mat-paginator
          [length]="pagina()?.totalElementos ?? 0"
          [pageIndex]="pagina()?.pagina ?? 0"
          [pageSize]="pagina()?.tamanio ?? 20"
          [pageSizeOptions]="[20, 50, 100]"
          (page)="paginar($event)" />
      </div>
    </section>
  `,
  styles: `
    .destacado { color: #c79100; vertical-align: -2px; margin-left: 4px; }
    td app-estado + app-estado { margin-left: 4px; }
  `,
})
export class EstablecimientosPage {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  protected readonly catalogos = inject(Catalogos);
  protected readonly puedeEditar = inject(AuthService).tieneRol('ADMIN', 'CARGADOR');

  protected readonly estados = ESTADOS_PUBLICACION;
  /** "Sin ubicación": sin lugar o con un lugar sin coordenadas (no encontrado al importar). */
  protected readonly ubicaciones = [
    { valor: 'VERIFICADO', texto: 'Verificada' },
    { valor: 'AUTOMATICO', texto: 'Automática' },
    { valor: 'APROXIMADO', texto: 'Aproximada' },
    { valor: 'SIN_UBICACION', texto: 'Sin ubicación' },
  ];
  protected readonly columnas = ['nombre', 'categorias', 'localidad', 'ubicacion', 'estado'];
  protected readonly localidades = toSignal(this.catalogos.de('localidad'), { initialValue: [] });
  protected readonly pagina = signal<Pagina<EstablecimientoResumen> | null>(null);
  protected readonly cargando = signal(false);

  private readonly recordados = inject(FiltrosRecordados);
  /**
   * Si se llega sin filtros (flecha "Volver" del detalle, menú lateral, enlaces) se restauran los últimos
   * que se usaron, incluida la página: moverse entre establecimientos no pierde la búsqueda.
   */
  private readonly restaurados = Object.keys(this.route.snapshot.queryParams).length
    ? null
    : this.recordados.leer('establecimientos');
  private readonly inicial = this.restaurados ? convertToParamMap(this.restaurados) : this.route.snapshot.queryParamMap;
  protected readonly filtros = inject(NonNullableFormBuilder).group({
    q: this.inicial.get('q') ?? '',
    estado: this.inicial.get('estado') ?? '',
    categoria: this.inicial.get('categoria') ?? '',
    localidadId: this.inicial.get('localidadId') ?? '',
    ubicacion: [this.inicial.getAll('ubicacion')],
    incluirInactivos: this.inicial.get('incluirInactivos') === 'true',
  });

  constructor() {
    const destroy = inject(DestroyRef);
    this.catalogos.arbolCategorias().pipe(takeUntilDestroyed(destroy)).subscribe();

    // Los filtros viven en la URL: al volver del detalle se conserva la búsqueda.
    this.filtros.valueChanges.pipe(debounceTime(300), takeUntilDestroyed(destroy)).subscribe((f) =>
      this.router.navigate([], {
        queryParams: { ...f, incluirInactivos: f.incluirInactivos || null, pagina: null },
        queryParamsHandling: 'merge',
        replaceUrl: true,
      }),
    );

    if (this.restaurados) {
      this.router.navigate([], { queryParams: this.restaurados, replaceUrl: true });
    }

    this.route.queryParamMap
      .pipe(
        // al restaurar, la primera emisión es la URL vacía: se espera la restaurada
        skip(this.restaurados ? 1 : 0),
        switchMap((p) => {
          this.recordados.guardar('establecimientos', Object.fromEntries(p.keys.map((k) => [k, p.getAll(k)])));
          this.cargando.set(true);
          return this.api
            .get<Pagina<EstablecimientoResumen>>('/establecimientos', {
              q: p.get('q'),
              estado: p.get('estado'),
              categoria: p.get('categoria'),
              localidadId: p.get('localidadId'),
              ubicacion: p.getAll('ubicacion'),
              incluirInactivos: p.get('incluirInactivos'),
              page: p.get('pagina') ?? 0,
              size: p.get('tamanio') ?? 20,
              sort: 'nombre',
            })
            .pipe(
              catchError((e) => {
                this.avisos.error(e);
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
    this.router.navigate([], {
      queryParams: { pagina: e.pageIndex || null, tamanio: e.pageSize === 20 ? null : e.pageSize },
      queryParamsHandling: 'merge',
    });
  }
}
