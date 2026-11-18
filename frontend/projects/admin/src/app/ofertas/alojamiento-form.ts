import { Component, OnInit, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { Alojamiento } from '../core/modelos';
import { avisarSiHayCambios } from '../core/cambios-pendientes';

const CAMPOS: { clave: keyof Alojamiento; texto: string }[] = [
  { clave: 'cantHabitaciones', texto: 'Habitaciones' },
  { clave: 'totalPlazas', texto: 'Plazas totales' },
  { clave: 'habAccesibles', texto: 'Habitaciones accesibles' },
  { clave: 'plazasAccesibles', texto: 'Plazas accesibles' },
  { clave: 'personalPermanente', texto: 'Personal permanente' },
  { clave: 'personalNoPermanente', texto: 'Personal no permanente' },
];

/** Datos de alojamiento (hotel, cabañas, camping…): categoría en estrellas, habitaciones y plazas. */
@Component({
  selector: 'app-alojamiento-form',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule],
  template: `
    <form class="formulario" [formGroup]="form" (ngSubmit)="guardar()">
      <mat-form-field>
        <mat-label>Estrellas</mat-label>
        <mat-select formControlName="estrellas">
          <mat-option [value]="null">Sin categorizar</mat-option>
          @for (n of [1, 2, 3, 4, 5]; track n) { <mat-option [value]="n">{{ n }} ★</mat-option> }
        </mat-select>
      </mat-form-field>
      @for (c of campos; track c.clave) {
        <mat-form-field>
          <mat-label>{{ c.texto }}</mat-label>
          <input matInput type="number" min="0" [formControlName]="c.clave" />
        </mat-form-field>
      }
      @if (editable()) {
        <div class="ancho-completo botones">
          <button mat-flat-button type="submit" [disabled]="form.invalid || form.pristine || guardando()">Guardar alojamiento</button>
        </div>
      }
    </form>
  `,
  styles: `.botones { display: flex; justify-content: flex-end; }`,
})
export class AlojamientoForm implements OnInit {
  private readonly avisoDeCambios = avisarSiHayCambios('Alojamiento', () => this.form.dirty && Object.values(this.form.getRawValue()).some((v) => v != null));
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);

  readonly ofertaId = input.required<number>();
  readonly alojamiento = input<Alojamiento | undefined>();
  readonly editable = input(true);

  protected readonly campos = CAMPOS;
  protected readonly guardando = signal(false);
  private readonly n = () => [null as number | null, Validators.min(0)];
  protected readonly form = inject(NonNullableFormBuilder).group({
    estrellas: [null as number | null],
    cantHabitaciones: this.n(),
    totalPlazas: this.n(),
    habAccesibles: this.n(),
    plazasAccesibles: this.n(),
    personalPermanente: this.n(),
    personalNoPermanente: this.n(),
  });

  ngOnInit(): void {
    const a = this.alojamiento();
    if (a) {
      this.form.reset(a);
    } else {
      this.form.markAsDirty();
    }
    if (!this.editable()) {
      this.form.disable();
    }
  }

  protected guardar(): void {
    this.guardando.set(true);
    this.api.put<Alojamiento>(`/ofertas/${this.ofertaId()}/alojamiento`, this.form.getRawValue()).subscribe({
      next: (a) => {
        this.guardando.set(false);
        this.form.reset(a);
        this.avisos.ok('Alojamiento guardado');
      },
      error: (e) => {
        this.guardando.set(false);
        this.avisos.error(e);
      },
    });
  }
}
