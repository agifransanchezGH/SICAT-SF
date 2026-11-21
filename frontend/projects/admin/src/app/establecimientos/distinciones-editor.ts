import { Component, OnInit, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormArray, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { Catalogos } from '../core/catalogos';
import { DistincionAsignada } from '../core/modelos';

import { avisarSiHayCambios } from '../core/cambios-pendientes';

/** Premios y certificaciones del establecimiento (catálogo "distincion"). */
@Component({
  selector: 'app-distinciones-editor',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule],
  template: `
    <div class="lista-editable" style="--columnas: minmax(220px, 2fr) 100px minmax(200px, 1.6fr) 48px">
      <p class="lista-ayuda">Premios y certificaciones. La URL sirve para comprobar que la distinción es real.</p>
      @if (filas.length) {
        <div class="te-cabecera" aria-hidden="true">
          <span>Distinción</span><span>Año</span><span>URL de verificación</span><span></span>
        </div>
      }
      @for (fila of filas.controls; track fila; let i = $index) {
        <div class="te-fila" [formGroup]="fila" role="group" [attr.aria-label]="'Distinción ' + (i + 1)">
          <div data-etiqueta="Distinción">
            <mat-form-field>
              <mat-select formControlName="distincionId" [aria-label]="'Distinción ' + (i + 1)">
                @for (d of catalogo(); track d.id) {
                  <mat-option [value]="d.id">{{ d.nombre }}{{ d.organismoOtorgante ? ' · ' + d.organismoOtorgante : '' }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
          </div>
          <div data-etiqueta="Año">
            <mat-form-field><input matInput type="number" formControlName="anioOtorgamiento" [attr.aria-label]="'Año de la distinción ' + (i + 1)" /></mat-form-field>
          </div>
          <div data-etiqueta="URL de verificación">
            <mat-form-field><input matInput type="url" formControlName="urlVerificacion" [attr.aria-label]="'URL de verificación de la distinción ' + (i + 1)" /></mat-form-field>
          </div>
          <div class="te-control te-acciones">
            @if (editable()) {
              <button mat-icon-button type="button" [attr.aria-label]="'Quitar distinción ' + (i + 1)" (click)="quitar(i)"><mat-icon>delete</mat-icon></button>
            }
          </div>
        </div>
      } @empty {
        <p class="te-vacio">Sin distinciones cargadas.</p>
      }
      @if (editable()) {
        <div class="te-pie">
          <button mat-button type="button" class="te-agregar" (click)="agregar()"><mat-icon>add</mat-icon> Agregar distinción</button>
          @if (filas.dirty) { <span class="suave cambios" role="status">Hay cambios sin guardar</span> }
          <button mat-flat-button type="button" (click)="guardar()" [disabled]="filas.invalid || filas.pristine || guardando()">Guardar distinciones</button>
        </div>
      }
    </div>
  `,
  styleUrl: '../comun/editores.scss',
  styles: `.cambios { font-size: 13px; }`,
})
export class DistincionesEditor implements OnInit {
  private readonly avisoDeCambios = avisarSiHayCambios('Distinciones', () => this.filas.dirty);
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly fb = inject(NonNullableFormBuilder);

  readonly establecimientoId = input.required<number>();
  readonly editable = input(true);

  protected readonly catalogo = toSignal(inject(Catalogos).de('distincion'), { initialValue: [] });
  protected readonly filas = new FormArray<ReturnType<DistincionesEditor['fila']>>([]);
  protected readonly guardando = signal(false);

  private get ruta(): string {
    return `/establecimientos/${this.establecimientoId()}/distinciones`;
  }

  ngOnInit(): void {
    this.api.get<DistincionAsignada[]>(this.ruta).subscribe({ next: (l) => this.cargar(l), error: (e) => this.avisos.error(e) });
  }

  private fila(d?: DistincionAsignada) {
    return this.fb.group({
      distincionId: [d?.distincionId ?? (null as number | null), Validators.required],
      anioOtorgamiento: [d?.anioOtorgamiento ?? (null as number | null), [Validators.min(1900), Validators.max(2100)]],
      urlVerificacion: [d?.urlVerificacion ?? '', Validators.pattern(/^https?:\/\/.+/)],
    });
  }

  private cargar(l: DistincionAsignada[]): void {
    this.filas.clear();
    l.forEach((d) => this.filas.push(this.fila(d)));
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
    const cuerpo = this.filas.getRawValue().map((d) => ({ ...d, urlVerificacion: d.urlVerificacion || null }));
    this.guardando.set(true);
    this.api.put<DistincionAsignada[]>(this.ruta, cuerpo).subscribe({
      next: (l) => {
        this.guardando.set(false);
        this.cargar(l);
        this.avisos.ok('Distinciones guardadas');
      },
      error: (e) => {
        this.guardando.set(false);
        this.avisos.error(e);
      },
    });
  }
}
