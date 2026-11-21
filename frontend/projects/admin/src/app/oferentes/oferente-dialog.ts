import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { Catalogos } from '../core/catalogos';
import { Oferente } from '../core/modelos';

@Component({
  selector: 'app-oferente-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ oferente ? 'Editar oferente' : 'Nuevo oferente' }}</h2>
    <mat-dialog-content>
      <p class="intro">La persona, empresa u organismo responsable de uno o más establecimientos.</p>
      <form id="form-oferente" class="campos" [formGroup]="form" (ngSubmit)="guardar()">
        <mat-form-field>
          <mat-label>Nombre o razón social</mat-label>
          <input matInput formControlName="nombre" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Tipo de oferente</mat-label>
          <mat-select formControlName="tipoOferenteId">
            @for (t of tipos(); track t.id) { <mat-option [value]="t.id">{{ t.nombre }}</mat-option> }
          </mat-select>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-flat-button type="submit" form="form-oferente" [disabled]="form.invalid || guardando()">
        {{ oferente ? 'Guardar cambios' : 'Crear oferente' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .intro { margin: 0 0 16px; font-size: 14px; color: var(--sicat-texto-suave); max-width: 46ch; }
    .campos { display: flex; flex-direction: column; gap: 4px; min-width: min(440px, 80vw); }
  `,
})
export class OferenteDialog {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly ref = inject(MatDialogRef<OferenteDialog, Oferente>);
  protected readonly oferente = inject<Oferente | null>(MAT_DIALOG_DATA);

  protected readonly tipos = toSignal(inject(Catalogos).de('tipo-oferente'), { initialValue: [] });
  protected readonly guardando = signal(false);
  protected readonly form = inject(NonNullableFormBuilder).group({
    nombre: [this.oferente?.nombre ?? '', [Validators.required, Validators.maxLength(300)]],
    tipoOferenteId: [this.oferente?.tipoOferenteId ?? (null as number | null), Validators.required],
  });

  protected guardar(): void {
    const f = this.form.getRawValue();
    const cuerpo = { nombre: f.nombre.trim(), tipoOferenteId: f.tipoOferenteId, activo: this.oferente?.activo ?? null };
    this.guardando.set(true);
    const pedido = this.oferente
      ? this.api.put<Oferente>(`/oferentes/${this.oferente.id}`, cuerpo)
      : this.api.post<Oferente>('/oferentes', cuerpo);
    pedido.subscribe({
      next: (o) => {
        this.avisos.ok('Oferente guardado');
        this.ref.close(o);
      },
      error: (e) => {
        this.guardando.set(false);
        this.avisos.error(e);
      },
    });
  }
}
