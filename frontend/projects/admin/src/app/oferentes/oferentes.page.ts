import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { Router, RouterLink } from '@angular/router';
import { FiltrosRecordados } from '../core/filtros-recordados';
import { EMPTY, Subject, catchError, debounceTime, finalize, merge, startWith, switchMap } from 'rxjs';

import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { Avisos } from '../core/avisos';
import { Catalogos } from '../core/catalogos';
import { EstadoChip } from '../core/estado';
import { Oferente, Pagina } from '../core/modelos';
import { OferenteDialog } from './oferente-dialog';

@Component({
  selector: 'app-oferentes-page',
  imports: [
    ReactiveFormsModule, RouterLink, EstadoChip, MatTableModule, MatPaginatorModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatCheckboxModule, MatButtonModule, MatIconModule, MatProgressBarModule,
  ],
  template: `
    <section class="pagina">
      <div class="encabezado">
        <h1>Oferentes</h1>
        @if (puedeEditar) {
          <button mat-flat-button (click)="nuevo()"><mat-icon>add</mat-icon> Nuevo oferente</button>
        }
      </div>
      <form class="filtros" [formGroup]="filtros">
        <mat-form-field class="busqueda">
          <mat-label>Buscar por nombre</mat-label>
          <mat-icon matPrefix>search</mat-icon>
          <input matInput formControlName="q" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Tipo</mat-label>
          <mat-select formControlName="tipo">
            <mat-option [value]="null">Todos</mat-option>
            @for (t of tipos(); track t.id) { <mat-option [value]="t.id">{{ t.nombre }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-checkbox formControlName="incluirInactivos">Incluir dados de baja</mat-checkbox>
      </form>
      <div class="tarjeta">
        @if (cargando()) { <mat-progress-bar mode="indeterminate" /> }
        <table mat-table [dataSource]="pagina()?.contenido ?? []" class="tabla">
          <ng-container matColumnDef="nombre">
            <th mat-header-cell *matHeaderCellDef>Nombre</th>
            <td mat-cell *matCellDef="let o"><a class="nombre" [routerLink]="['/oferentes', o.id]">{{ o.nombre }}</a></td>
          </ng-container>
          <ng-container matColumnDef="tipo">
            <th mat-header-cell *matHeaderCellDef>Tipo</th>
            <td mat-cell *matCellDef="let o">{{ o.tipoOferente }}</td>
          </ng-container>
          <ng-container matColumnDef="estado">
            <th mat-header-cell *matHeaderCellDef><span class="cdk-visually-hidden">Estado</span></th>
            <td mat-cell *matCellDef="let o">@if (!o.activo) { <app-estado valor="inactivo" /> }</td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let o; columns: columnas" class="fila-link" [routerLink]="['/oferentes', o.id]"></tr>
        </table>
        @if (pagina()?.contenido?.length === 0) { <p class="vacio">No hay oferentes con esos filtros.</p> }
        <mat-paginator [length]="pagina()?.totalElementos ?? 0" [pageIndex]="pagina()?.pagina ?? 0" [pageSize]="tamanio"
                       [pageSizeOptions]="[20, 50, 100]" (page)="paginar($event)" />
      </div>
    </section>
  `,
  styles: `.nombre { font-weight: 600; color: inherit; text-decoration: none; } .nombre:hover { text-decoration: underline; }`,
})
export class OferentesPage {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  protected readonly puedeEditar = inject(AuthService).tieneRol('ADMIN', 'CARGADOR');

  protected readonly tipos = toSignal(inject(Catalogos).de('tipo-oferente'), { initialValue: [] });
  protected readonly columnas = ['nombre', 'tipo', 'estado'];
  protected readonly pagina = signal<Pagina<Oferente> | null>(null);
  protected readonly cargando = signal(false);
  protected tamanio = 20;
  private numero = 0;
  private readonly recargar = new Subject<void>();

  protected readonly filtros = inject(NonNullableFormBuilder).group({
    q: [''],
    tipo: [null as number | null],
    incluirInactivos: [false],
  });

  constructor() {
    // Al volver de un oferente (o desde el menú) se restauran los últimos filtros y la página.
    const recordados = inject(FiltrosRecordados);
    const g = recordados.leer('oferentes');
    if (g) {
      this.filtros.patchValue({ q: g['q'] ?? '', tipo: g['tipo'] ?? null, incluirInactivos: !!g['incluirInactivos'] });
      this.numero = g['pagina'] ?? 0;
      this.tamanio = g['tamanio'] ?? 20;
    }
    // startWith(undefined): la primera carga no vuelve a la página 0 (sí los cambios de filtro).
    merge(this.filtros.valueChanges.pipe(debounceTime(300), startWith(undefined)), this.recargar)
      .pipe(
        switchMap((cambio) => {
          if (cambio !== undefined) {
            this.numero = 0;
          }
          const f = this.filtros.getRawValue();
          recordados.guardar('oferentes', { ...f, pagina: this.numero || null, tamanio: this.tamanio === 20 ? null : this.tamanio });
          this.cargando.set(true);
          return this.api
            .get<Pagina<Oferente>>('/oferentes', { ...f, page: this.numero, size: this.tamanio, sort: 'nombre' })
            .pipe(
              catchError((e) => {
                this.avisos.error(e);
                return EMPTY;
              }),
              finalize(() => this.cargando.set(false)),
            );
        }),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe((p) => this.pagina.set(p));
  }

  protected paginar(e: PageEvent): void {
    this.numero = e.pageIndex;
    this.tamanio = e.pageSize;
    this.recargar.next();
  }

  protected nuevo(): void {
    this.dialog
      .open(OferenteDialog, { data: null })
      .afterClosed()
      .subscribe((o?: Oferente) => o && this.router.navigate(['/oferentes', o.id]));
  }
}
