import { Component, OnInit, inject, input, signal } from '@angular/core';
import { AbstractControl, FormArray, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { DIAS, Horario } from '../core/modelos';

const desdeAntesDeHasta = (g: AbstractControl): ValidationErrors | null => {
  const { desde, hasta } = g.value as { desde: string; hasta: string };
  return desde && hasta && desde >= hasta ? { rango: true } : null;
};

/** Franjas horarias por día de la semana (de una oferta o de una visita guiada). */
@Component({
  selector: 'app-horarios-editor',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, MatIconModule],
  template: `
    <div class="editor">
      @for (fila of filas.controls; track fila; let i = $index) {
        <div class="fila" [formGroup]="fila">
          <mat-form-field>
            <mat-label>Día</mat-label>
            <mat-select formControlName="diaSemana">
              @for (d of dias; track d.valor) { <mat-option [value]="d.valor">{{ d.texto }}</mat-option> }
            </mat-select>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Desde</mat-label>
            <input matInput type="time" formControlName="desde" />
          </mat-form-field>
          <mat-form-field>
            <mat-label>Hasta</mat-label>
            <input matInput type="time" formControlName="hasta" />
          </mat-form-field>
          @if (fila.hasError('rango')) { <span class="error">"Desde" debe ser anterior a "Hasta"</span> }
          @if (editable()) {
            <div class="mover">
              <button mat-icon-button type="button" aria-label="Duplicar" title="Copiar al día siguiente" (click)="duplicar(i)"><mat-icon>content_copy</mat-icon></button>
              <button mat-icon-button type="button" aria-label="Quitar" (click)="quitar(i)"><mat-icon>delete</mat-icon></button>
            </div>
          }
        </div>
      } @empty {
        <p class="vacio">Sin horarios cargados.</p>
      }
      @if (editable()) {
        <div class="acciones-editor">
          <button mat-stroked-button type="button" (click)="agregar()"><mat-icon>add</mat-icon> Agregar franja</button>
          <span class="espacio"></span>
          <button mat-flat-button type="button" (click)="guardar()" [disabled]="filas.invalid || filas.pristine || guardando()">Guardar horarios</button>
        </div>
      }
      <p class="suave nota">Para horario cortado, cargá dos franjas el mismo día.</p>
    </div>
  `,
  styleUrl: './editores.scss',
  styles: `.error { color: var(--sicat-error); font-size: 13px; }`,
})
export class HorariosEditor implements OnInit {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly fb = inject(NonNullableFormBuilder);

  /** p. ej. /ofertas/3/horarios o /visitas-guiadas/2/horarios */
  readonly ruta = input.required<string>();
  readonly editable = input(true);

  protected readonly dias = DIAS.slice(1).map((texto, i) => ({ valor: i + 1, texto }));
  protected readonly filas = new FormArray<ReturnType<HorariosEditor['fila']>>([]);
  protected readonly guardando = signal(false);

  ngOnInit(): void {
    this.api.get<Horario[]>(this.ruta()).subscribe({ next: (l) => this.cargar(l), error: (e) => this.avisos.error(e) });
  }

  private fila(h?: Partial<Horario>) {
    return this.fb.group(
      {
        id: [h?.id ?? (null as number | null)],
        diaSemana: [h?.diaSemana ?? 1, Validators.required],
        desde: [h?.desde ?? '09:00', Validators.required],
        hasta: [h?.hasta ?? '18:00', Validators.required],
      },
      { validators: desdeAntesDeHasta },
    );
  }

  private cargar(lista: Horario[]): void {
    this.filas.clear();
    [...lista]
      .sort((a, b) => a.diaSemana - b.diaSemana || a.desde.localeCompare(b.desde))
      .forEach((h) => this.filas.push(this.fila(h)));
    if (!this.editable()) {
      this.filas.disable();
    }
    this.filas.markAsPristine();
  }

  protected agregar(): void {
    const ultimo = this.filas.length ? this.filas.at(this.filas.length - 1).getRawValue() : undefined;
    this.filas.push(this.fila(ultimo ? { ...ultimo, id: null } : undefined));
    this.filas.markAsDirty();
  }

  protected duplicar(i: number): void {
    const h = this.filas.at(i).getRawValue();
    this.filas.insert(i + 1, this.fila({ ...h, id: null, diaSemana: (h.diaSemana % 7) + 1 }));
    this.filas.markAsDirty();
  }

  protected quitar(i: number): void {
    this.filas.removeAt(i);
    this.filas.markAsDirty();
  }

  protected guardar(): void {
    this.guardando.set(true);
    this.api.put<Horario[]>(this.ruta(), this.filas.getRawValue()).subscribe({
      next: (l) => {
        this.guardando.set(false);
        this.cargar(l);
        this.avisos.ok('Horarios guardados');
      },
      error: (e) => {
        this.guardando.set(false);
        this.avisos.error(e);
      },
    });
  }
}
