import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { EMPTY, Subject, catchError, debounceTime, finalize, merge, startWith, switchMap } from 'rxjs';

import { ACCIONES, cambios, mostrar } from '../comun/historial';
import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { Auditoria, Pagina } from '../core/modelos';

const TABLAS = [
  'establecimiento', 'oferente', 'oferta', 'lugar', 'canal_contacto', 'horario', 'alojamiento', 'circuito', 'salon',
  'visita_guiada', 'categoria', 'caracteristica', 'usuario',
];

/** Búsqueda en la auditoría de cambios (ADR 0006). Solo ADMIN. */
@Component({
  selector: 'app-auditoria-page',
  imports: [
    ReactiveFormsModule, DatePipe, MatTableModule, MatPaginatorModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatProgressBarModule,
  ],
  template: `
    <section class="pagina">
      <div class="encabezado"><h1>Auditoría</h1></div>
      <form class="filtros" [formGroup]="filtros">
        <mat-form-field>
          <mat-label>Tabla</mat-label>
          <mat-select formControlName="tabla">
            <mat-option value="">Todas</mat-option>
            @for (t of tablas; track t) { <mat-option [value]="t">{{ t }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field><mat-label>Id del registro</mat-label><input matInput formControlName="clave" /></mat-form-field>
        <mat-form-field>
          <mat-label>Acción</mat-label>
          <mat-select formControlName="accion">
            <mat-option value="">Todas</mat-option>
            @for (a of acciones; track a[0]) { <mat-option [value]="a[0]">{{ a[1] }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Origen</mat-label>
          <mat-select formControlName="origen">
            <mat-option value="">Todos</mat-option>
            <mat-option value="APP">Panel</mat-option>
            <mat-option value="SISTEMA">Sistema</mat-option>
            <mat-option value="MIGRACION">Migración</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-form-field><mat-label>Desde</mat-label><input matInput type="date" formControlName="desde" /></mat-form-field>
        <mat-form-field><mat-label>Hasta</mat-label><input matInput type="date" formControlName="hasta" /></mat-form-field>
      </form>
      <div class="tarjeta">
        @if (cargando()) { <mat-progress-bar mode="indeterminate" /> }
        <table mat-table [dataSource]="pagina()?.contenido ?? []" class="tabla" multiTemplateDataRows>
          <ng-container matColumnDef="fecha">
            <th mat-header-cell *matHeaderCellDef>Fecha</th>
            <td mat-cell *matCellDef="let a">{{ a.fechaHora | date: 'dd/MM/yyyy HH:mm:ss' }}</td>
          </ng-container>
          <ng-container matColumnDef="registro">
            <th mat-header-cell *matHeaderCellDef>Registro</th>
            <td mat-cell *matCellDef="let a">{{ a.tabla }} #{{ a.clave }}</td>
          </ng-container>
          <ng-container matColumnDef="accion">
            <th mat-header-cell *matHeaderCellDef>Acción</th>
            <td mat-cell *matCellDef="let a">{{ textoAccion(a.accion) }}</td>
          </ng-container>
          <ng-container matColumnDef="usuario">
            <th mat-header-cell *matHeaderCellDef>Usuario</th>
            <td mat-cell *matCellDef="let a">{{ a.usuario?.nombreCompleto ?? a.origen }}</td>
          </ng-container>
          <ng-container matColumnDef="detalle">
            <td mat-cell *matCellDef="let a" [attr.colspan]="columnas.length" class="detalle">
              @if (abierto() === a.id) {
                <ul>
                  @for (c of cambios(a); track c.campo) {
                    <li><code>{{ c.campo }}</code>: {{ mostrar(c.antes) }} → <strong>{{ mostrar(c.despues) }}</strong></li>
                  }
                </ul>
              }
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let a; columns: columnas" class="fila-link" (click)="abierto.set(abierto() === a.id ? null : a.id)"></tr>
          <tr mat-row *matRowDef="let a; columns: ['detalle']" class="fila-detalle" [class.oculta]="abierto() !== a.id"></tr>
        </table>
        @if (pagina()?.contenido?.length === 0) { <p class="vacio">Sin registros con esos filtros.</p> }
        <mat-paginator [length]="pagina()?.totalElementos ?? 0" [pageIndex]="pagina()?.pagina ?? 0" [pageSize]="tamanio"
                       [pageSizeOptions]="[20, 50, 100]" (page)="paginar($event)" />
      </div>
    </section>
  `,
  styles: `
    .fila-detalle.oculta { display: none; }
    .detalle ul { margin: 4px 0 12px; padding-left: 18px; font-size: 14px; word-break: break-word; }
  `,
})
export class AuditoriaPage {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);

  protected readonly tablas = TABLAS;
  protected readonly acciones = Object.entries(ACCIONES);
  protected readonly columnas = ['fecha', 'registro', 'accion', 'usuario'];
  protected readonly pagina = signal<Pagina<Auditoria> | null>(null);
  protected readonly cargando = signal(false);
  protected readonly abierto = signal<number | null>(null);
  protected readonly cambios = cambios;
  protected readonly mostrar = mostrar;
  protected tamanio = 20;
  private numero = 0;
  private readonly recargar = new Subject<void>();

  protected readonly filtros = inject(NonNullableFormBuilder).group({
    tabla: [''],
    clave: [''],
    accion: [''],
    origen: [''],
    desde: [''],
    hasta: [''],
  });

  constructor() {
    merge(this.filtros.valueChanges.pipe(debounceTime(300), startWith(null)), this.recargar)
      .pipe(
        switchMap((cambio) => {
          if (cambio !== undefined) {
            this.numero = 0;
          }
          this.cargando.set(true);
          return this.api
            .get<Pagina<Auditoria>>('/auditoria', { ...this.filtros.getRawValue(), page: this.numero, size: this.tamanio })
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

  protected textoAccion(a: string): string {
    return ACCIONES[a] ?? a;
  }

  protected paginar(e: PageEvent): void {
    this.numero = e.pageIndex;
    this.tamanio = e.pageSize;
    this.recargar.next();
  }
}
