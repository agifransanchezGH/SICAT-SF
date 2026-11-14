import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { Avisos } from '../core/avisos';
import { CATALOGOS, Catalogos, NombreCatalogo } from '../core/catalogos';
import { Confirmar } from '../core/confirmar';
import { EstadoChip } from '../core/estado';
import { CatalogoItem } from '../core/modelos';

/** ABM de los catálogos simples. Solo ADMIN modifica; el resto los consulta. */
@Component({
  selector: 'app-catalogos-page',
  imports: [
    FormsModule, ReactiveFormsModule, EstadoChip, MatListModule, MatTableModule, MatFormFieldModule, MatInputModule,
    MatCheckboxModule, MatButtonModule, MatIconModule, MatTooltipModule,
  ],
  template: `
    <section class="pagina">
      <div class="encabezado"><h1>Catálogos</h1></div>
      <div class="dos-columnas">
        <mat-nav-list class="tarjeta lista">
          @for (c of catalogos; track c.nombre) {
            <a mat-list-item [activated]="c.nombre === actual()" (click)="elegir(c.nombre)">{{ c.titulo }}</a>
          }
        </mat-nav-list>

        <div class="tarjeta">
          <div class="cabecera">
            <h2>{{ titulo() }}</h2>
            <mat-checkbox [(ngModel)]="incluirInactivos" (change)="cargar()">Ver dados de baja</mat-checkbox>
          </div>
          @if (esAdmin) {
            <form class="nuevo" [formGroup]="form" (ngSubmit)="guardar()">
              <mat-form-field><mat-label>Nombre</mat-label><input matInput formControlName="nombre" /></mat-form-field>
              @if (actual() === 'localidad') {
                <mat-form-field><mat-label>Provincia</mat-label><input matInput formControlName="provincia" /></mat-form-field>
              }
              @if (actual() === 'distincion') {
                <mat-form-field><mat-label>Organismo otorgante</mat-label><input matInput formControlName="organismoOtorgante" /></mat-form-field>
              }
              @if (actual() === 'caracteristica') {
                <mat-form-field><mat-label>Grupo</mat-label><input matInput formControlName="grupo" placeholder="Servicios, Accesibilidad…" /></mat-form-field>
                <mat-checkbox formControlName="admiteCantidad">Admite cantidad</mat-checkbox>
              }
              <button mat-flat-button type="submit" [disabled]="form.invalid">{{ editando() ? 'Guardar' : 'Agregar' }}</button>
              @if (editando()) { <button mat-button type="button" (click)="cancelar()">Cancelar</button> }
            </form>
          }
          <table mat-table [dataSource]="items()" class="tabla">
            <ng-container matColumnDef="nombre">
              <th mat-header-cell *matHeaderCellDef>Nombre</th>
              <td mat-cell *matCellDef="let i">{{ i.nombre }} @if (i.activo === false) { <app-estado valor="inactivo" /> }</td>
            </ng-container>
            <ng-container matColumnDef="extra">
              <th mat-header-cell *matHeaderCellDef>{{ extraTitulo() }}</th>
              <td mat-cell *matCellDef="let i">{{ i.provincia ?? i.organismoOtorgante ?? i.grupo ?? '' }}{{ i.admiteCantidad ? ' · con cantidad' : '' }}</td>
            </ng-container>
            <ng-container matColumnDef="acciones">
              <th mat-header-cell *matHeaderCellDef></th>
              <td mat-cell *matCellDef="let i" class="acciones-celda">
                @if (esAdmin) {
                  <button mat-icon-button matTooltip="Editar" (click)="editar(i)"><mat-icon>edit</mat-icon></button>
                  @if (i.activo === false) {
                    <button mat-icon-button matTooltip="Reactivar" (click)="reactivar(i)"><mat-icon>restore</mat-icon></button>
                  } @else {
                    <button mat-icon-button matTooltip="Dar de baja" (click)="darDeBaja(i)"><mat-icon>delete</mat-icon></button>
                  }
                }
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="columnas"></tr>
            <tr mat-row *matRowDef="let i; columns: columnas"></tr>
          </table>
        </div>
      </div>
    </section>
  `,
  styles: `
    .dos-columnas { display: grid; grid-template-columns: 260px 1fr; gap: 16px; align-items: start; }
    .lista { padding: 4px 0; }
    .cabecera { display: flex; align-items: center; gap: 12px; padding: 12px 16px 0; }
    .cabecera h2 { margin: 0 auto 0 0; font: var(--mat-sys-title-large); }
    .nuevo { display: flex; gap: 8px 12px; align-items: center; flex-wrap: wrap; padding: 12px 16px; }
    .nuevo mat-form-field { flex: 1 1 200px; }
    .acciones-celda { white-space: nowrap; text-align: right; }
    @media (max-width: 800px) { .dos-columnas { grid-template-columns: 1fr; } }
  `,
})
export class CatalogosPage {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly confirmar = inject(Confirmar);
  private readonly cache = inject(Catalogos);
  protected readonly esAdmin = inject(AuthService).tieneRol('ADMIN');

  protected readonly catalogos = CATALOGOS;
  protected readonly actual = signal<NombreCatalogo>('tipo-oferente');
  protected readonly titulo = computed(() => CATALOGOS.find((c) => c.nombre === this.actual())!.titulo);
  protected readonly extraTitulo = computed(
    () => ({ localidad: 'Provincia', distincion: 'Organismo', caracteristica: 'Grupo' })[this.actual() as string] ?? '',
  );
  protected readonly columnas = ['nombre', 'extra', 'acciones'];
  protected readonly items = signal<CatalogoItem[]>([]);
  protected readonly editando = signal<CatalogoItem | null>(null);
  protected incluirInactivos = false;

  protected readonly form = inject(NonNullableFormBuilder).group({
    nombre: ['', [Validators.required, Validators.maxLength(200)]],
    provincia: [''],
    organismoOtorgante: [''],
    grupo: [''],
    admiteCantidad: [false],
  });

  constructor() {
    this.cargar();
  }

  protected elegir(n: NombreCatalogo): void {
    this.actual.set(n);
    this.cancelar();
    this.cargar();
  }

  protected cargar(): void {
    this.api.get<CatalogoItem[]>(`/catalogos/${this.actual()}`, { incluirInactivos: this.incluirInactivos }).subscribe({
      next: (l) => this.items.set(l),
      error: (e) => this.avisos.error(e),
    });
  }

  protected editar(i: CatalogoItem): void {
    this.editando.set(i);
    this.form.reset({
      nombre: i.nombre,
      provincia: i.provincia ?? '',
      organismoOtorgante: i.organismoOtorgante ?? '',
      grupo: i.grupo ?? '',
      admiteCantidad: i.admiteCantidad ?? false,
    });
  }

  protected cancelar(): void {
    this.editando.set(null);
    this.form.reset();
  }

  private cuerpo(activo: boolean | null) {
    const f = this.form.getRawValue();
    const n = this.actual();
    return {
      nombre: f.nombre.trim(),
      provincia: n === 'localidad' ? f.provincia || null : null,
      organismoOtorgante: n === 'distincion' ? f.organismoOtorgante || null : null,
      grupo: n === 'caracteristica' ? f.grupo || null : null,
      admiteCantidad: n === 'caracteristica' ? f.admiteCantidad : null,
      activo,
    };
  }

  protected guardar(): void {
    const e = this.editando();
    const pedido = e
      ? this.api.put(`/catalogos/${this.actual()}/${e.id}`, this.cuerpo(null))
      : this.api.post(`/catalogos/${this.actual()}`, this.cuerpo(null));
    pedido.subscribe({
      next: () => {
        this.avisos.ok(e ? 'Ítem modificado' : 'Ítem agregado');
        this.listo();
      },
      error: (err) => this.avisos.error(err),
    });
  }

  protected reactivar(i: CatalogoItem): void {
    this.editar(i);
    this.api.put(`/catalogos/${this.actual()}/${i.id}`, this.cuerpo(true)).subscribe({
      next: () => {
        this.avisos.ok('Ítem reactivado');
        this.listo();
      },
      error: (err) => this.avisos.error(err),
    });
  }

  protected darDeBaja(i: CatalogoItem): void {
    this.confirmar.pedir('Dar de baja', `"${i.nombre}" deja de ofrecerse en los formularios. Lo ya cargado se conserva.`, 'Dar de baja').subscribe((ok) => {
      if (ok) {
        this.api.delete(`/catalogos/${this.actual()}/${i.id}`).subscribe({
          next: () => {
            this.avisos.ok('Ítem dado de baja');
            this.listo();
          },
          error: (err) => this.avisos.error(err),
        });
      }
    });
  }

  private listo(): void {
    this.cache.invalidar(this.actual());
    this.cancelar();
    this.cargar();
  }
}
