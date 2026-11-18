import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { forkJoin } from 'rxjs';

import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { Catalogos } from '../core/catalogos';
import { Oferta, Ref } from '../core/modelos';
import { avisarSiHayCambios } from '../core/cambios-pendientes';

/** Datos de la oferta: categoría principal, adicionales, acceso, público y frecuencia. */
@Component({
  selector: 'app-oferta-datos',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule],
  template: `
    <form class="formulario" [formGroup]="form" (ngSubmit)="guardar()">
      <mat-form-field class="ancho-completo">
        <mat-label>Categoría principal</mat-label>
        <mat-select formControlName="categoriaId">
          @for (c of catalogos.categorias(); track c.id) { <mat-option [value]="c.id">{{ c.nombre }}</mat-option> }
        </mat-select>
      </mat-form-field>
      <mat-form-field class="ancho-completo">
        <mat-label>Categorías adicionales</mat-label>
        <mat-select formControlName="adicionales" multiple>
          @for (c of catalogos.categorias(); track c.id) { <mat-option [value]="c.id">{{ c.nombre }}</mat-option> }
        </mat-select>
        <mat-hint>Para que también aparezca al filtrar por esas categorías</mat-hint>
      </mat-form-field>
      <mat-form-field>
        <mat-label>Acceso</mat-label>
        <input matInput formControlName="acceso" placeholder="Libre y gratuito, con entrada…" />
      </mat-form-field>
      <mat-form-field>
        <mat-label>Público admitido</mat-label>
        <input matInput formControlName="publicoAdmitido" placeholder="Todo público, mayores de 18…" />
      </mat-form-field>
      <mat-form-field>
        <mat-label>Frecuencia</mat-label>
        <input matInput formControlName="frecuencia" placeholder="Todo el año, temporada de verano…" />
      </mat-form-field>
      <mat-form-field>
        <mat-label>Fecha de relevamiento</mat-label>
        <input matInput type="date" formControlName="fechaRelevamiento" />
      </mat-form-field>
      @if (editable()) {
        <div class="ancho-completo botones">
          <button mat-flat-button type="submit" [disabled]="form.invalid || form.pristine || guardando()">Guardar</button>
        </div>
      }
    </form>
  `,
  styles: `.botones { display: flex; justify-content: flex-end; }`,
})
export class OfertaDatos implements OnInit {
  private readonly avisoDeCambios = avisarSiHayCambios('Datos de la oferta', () => this.form.dirty);
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  protected readonly catalogos = inject(Catalogos);

  readonly oferta = input.required<Oferta>();
  readonly editable = input(true);
  readonly guardado = output<Oferta>();

  protected readonly guardando = signal(false);
  protected readonly form = inject(NonNullableFormBuilder).group({
    categoriaId: [0, Validators.required],
    adicionales: [[] as number[]],
    acceso: ['', Validators.maxLength(200)],
    publicoAdmitido: ['', Validators.maxLength(200)],
    frecuencia: ['', Validators.maxLength(200)],
    fechaRelevamiento: [''],
  });

  ngOnInit(): void {
    this.catalogos.arbolCategorias().subscribe();
    const o = this.oferta();
    this.form.reset({
      categoriaId: o.categoriaId,
      adicionales: (o.categoriasAdicionales ?? []).map((c) => c.id),
      acceso: o.acceso ?? '',
      publicoAdmitido: o.publicoAdmitido ?? '',
      frecuencia: o.frecuencia ?? '',
      fechaRelevamiento: o.fechaRelevamiento ?? '',
    });
    if (!this.editable()) {
      this.form.disable();
    }
  }

  protected guardar(): void {
    const f = this.form.getRawValue();
    const o = this.oferta();
    const cuerpo = {
      categoriaId: f.categoriaId,
      acceso: f.acceso || null,
      publicoAdmitido: f.publicoAdmitido || null,
      frecuencia: f.frecuencia || null,
      fechaRelevamiento: f.fechaRelevamiento || null,
      activo: o.activo,
    };
    this.guardando.set(true);
    forkJoin([
      this.api.put<Oferta>(`/ofertas/${o.id}`, cuerpo),
      this.api.put<Ref[]>(`/ofertas/${o.id}/categorias`, { ids: f.adicionales.filter((id) => id !== f.categoriaId) }),
    ]).subscribe({
      next: ([r, adicionales]) => {
        this.guardando.set(false);
        this.form.markAsPristine();
        this.avisos.ok('Oferta guardada');
        this.guardado.emit({ ...o, ...r, categoriasAdicionales: adicionales });
      },
      error: (e) => {
        this.guardando.set(false);
        this.avisos.error(e);
      },
    });
  }
}
