import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormArray, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { Referente } from '../core/modelos';
import { avisarSiHayCambios } from '../core/cambios-pendientes';

/** Personas de contacto internas del establecimiento (no salen al sitio público). */
@Component({
  selector: 'app-referentes-editor',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatCheckboxModule, MatButtonModule, MatIconModule],
  template: `
    <div class="editor">
      @for (fila of filas.controls; track fila; let i = $index) {
        <div class="fila" [formGroup]="fila">
          <mat-form-field><mat-label>Nombre</mat-label><input matInput formControlName="nombre" /></mat-form-field>
          <mat-form-field><mat-label>Cargo</mat-label><input matInput formControlName="cargo" /></mat-form-field>
          <mat-form-field><mat-label>Teléfono</mat-label><input matInput formControlName="telefono" /></mat-form-field>
          <mat-form-field><mat-label>Correo</mat-label><input matInput type="email" formControlName="correo" /></mat-form-field>
          <mat-checkbox formControlName="activo">Activo</mat-checkbox>
          @if (editable()) {
            <button mat-icon-button type="button" aria-label="Quitar" (click)="quitar(i)"><mat-icon>delete</mat-icon></button>
          }
        </div>
      } @empty {
        <p class="vacio">Sin referentes.</p>
      }
      @if (editable()) {
        <div class="acciones-editor">
          <button mat-stroked-button type="button" (click)="agregar()"><mat-icon>add</mat-icon> Agregar referente</button>
          <span class="espacio"></span>
          <button mat-flat-button type="button" (click)="guardar()" [disabled]="filas.invalid || filas.pristine || guardando()">Guardar referentes</button>
        </div>
      }
      <p class="suave nota">Uso interno de la Dirección de Turismo: no se publican.</p>
    </div>
  `,
  styleUrl: '../comun/editores.scss',
})
export class ReferentesEditor implements OnInit {
  private readonly avisoDeCambios = avisarSiHayCambios('Referentes', () => this.filas.dirty);
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly fb = inject(NonNullableFormBuilder);

  readonly establecimientoId = input.required<number>();
  readonly editable = input(true);

  protected readonly filas = new FormArray<ReturnType<ReferentesEditor['fila']>>([]);
  protected readonly guardando = signal(false);

  private get ruta(): string {
    return `/establecimientos/${this.establecimientoId()}/referentes`;
  }

  ngOnInit(): void {
    this.api.get<Referente[]>(this.ruta).subscribe({ next: (l) => this.cargar(l), error: (e) => this.avisos.error(e) });
  }

  private fila(r?: Referente) {
    return this.fb.group({
      id: [r?.id ?? (null as number | null)],
      nombre: [r?.nombre ?? '', [Validators.required, Validators.maxLength(200)]],
      cargo: [r?.cargo ?? ''],
      telefono: [r?.telefono ?? ''],
      correo: [r?.correo ?? '', Validators.email],
      activo: [r?.activo ?? true],
    });
  }

  private cargar(l: Referente[]): void {
    this.filas.clear();
    l.forEach((r) => this.filas.push(this.fila(r)));
    if (!this.editable()) {
      this.filas.disable();
    }
    this.filas.markAsPristine();
  }

  protected agregar(): void {
    this.filas.push(this.fila());
    this.filas.markAsDirty();
  }

  protected quitar(i: number): void {
    this.filas.removeAt(i);
    this.filas.markAsDirty();
  }

  protected guardar(): void {
    const cuerpo = this.filas
      .getRawValue()
      .map((r) => ({ ...r, cargo: r.cargo || null, telefono: r.telefono || null, correo: r.correo || null }));
    this.guardando.set(true);
    this.api.put<Referente[]>(this.ruta, cuerpo).subscribe({
      next: (l) => {
        this.guardando.set(false);
        this.cargar(l);
        this.avisos.ok('Referentes guardados');
      },
      error: (e) => {
        this.guardando.set(false);
        this.avisos.error(e);
      },
    });
  }
}
