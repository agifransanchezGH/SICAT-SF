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
    <div class="lista-editable" style="--columnas: minmax(150px, 1.3fr) minmax(120px, 1fr) minmax(130px, 1fr) minmax(170px, 1.3fr) 76px 48px">
      <p class="lista-ayuda">Personas de contacto para la Dirección de Turismo. Son de uso interno: no se publican.</p>
      @if (filas.length) {
        <div class="te-cabecera" aria-hidden="true">
          <span>Nombre</span><span>Cargo</span><span>Teléfono</span><span>Correo</span><span>Activo</span><span></span>
        </div>
      }
      @for (fila of filas.controls; track fila; let i = $index) {
        <div class="te-fila" [formGroup]="fila" role="group" [attr.aria-label]="'Referente ' + (i + 1)">
          <div data-etiqueta="Nombre">
            <mat-form-field><input matInput formControlName="nombre" [attr.aria-label]="'Nombre del referente ' + (i + 1)" /></mat-form-field>
          </div>
          <div data-etiqueta="Cargo">
            <mat-form-field><input matInput formControlName="cargo" [attr.aria-label]="'Cargo del referente ' + (i + 1)" /></mat-form-field>
          </div>
          <div data-etiqueta="Teléfono">
            <mat-form-field><input matInput type="tel" formControlName="telefono" [attr.aria-label]="'Teléfono del referente ' + (i + 1)" /></mat-form-field>
          </div>
          <div data-etiqueta="Correo">
            <mat-form-field><input matInput type="email" formControlName="correo" [attr.aria-label]="'Correo del referente ' + (i + 1)" /></mat-form-field>
          </div>
          <div class="te-control">
            <mat-checkbox formControlName="activo" [aria-label]="'Referente ' + (i + 1) + ' activo'"><span class="solo-angosto">Activo</span></mat-checkbox>
          </div>
          <div class="te-control te-acciones">
            @if (editable()) {
              <button mat-icon-button type="button" [attr.aria-label]="'Quitar referente ' + (i + 1)" (click)="quitar(i)"><mat-icon>delete</mat-icon></button>
            }
          </div>
        </div>
      } @empty {
        <p class="te-vacio">Sin referentes cargados.</p>
      }
      @if (editable()) {
        <div class="te-pie">
          <button mat-button type="button" class="te-agregar" (click)="agregar()"><mat-icon>add</mat-icon> Agregar referente</button>
          @if (filas.dirty) { <span class="suave cambios" role="status">Hay cambios sin guardar</span> }
          <button mat-flat-button type="button" (click)="guardar()" [disabled]="filas.invalid || filas.pristine || guardando()">Guardar referentes</button>
        </div>
      }
    </div>
  `,
  styleUrl: '../comun/editores.scss',
  styles: `.cambios { font-size: 13px; }`,
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
