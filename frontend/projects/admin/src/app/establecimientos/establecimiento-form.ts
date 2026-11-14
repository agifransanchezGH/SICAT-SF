import { Component, inject, input, OnInit, output, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { Establecimiento, Lugar } from '../core/modelos';
import { SelectorRemoto } from '../core/selector-remoto';
import { LugarDialog } from '../lugares/lugar-dialog';

/** Datos generales de un establecimiento: se usa en el alta y en la pestaña "Datos" de la ficha. */
@Component({
  selector: 'app-establecimiento-form',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatCheckboxModule, MatButtonModule, MatIconModule, SelectorRemoto],
  template: `
    <form class="formulario" [formGroup]="form" (ngSubmit)="guardar()">
      <mat-form-field class="ancho-completo">
        <mat-label>Nombre</mat-label>
        <input matInput formControlName="nombre" />
      </mat-form-field>

      <app-selector-remoto class="ancho-completo" formControlName="oferenteId" ruta="/oferentes" etiqueta="Oferente (titular)"
                           [textoInicial]="establecimiento()?.oferente?.nombre" ayuda="Persona o empresa responsable" />

      <div class="ancho-completo lugar">
        <app-selector-remoto #selectorLugar formControlName="lugarId" ruta="/lugares" etiqueta="Ubicación (lugar)"
                             [textoInicial]="establecimiento()?.lugar?.nombre" [detalle]="direccionLugar"
                             ayuda="Buscá por nombre o calle" />
        @if (editable()) {
          <button mat-stroked-button type="button" (click)="nuevoLugar()"><mat-icon>add_location</mat-icon> Nuevo lugar</button>
          @if (form.controls.lugarId.value) {
            <button mat-stroked-button type="button" (click)="editarLugar()"><mat-icon>edit_location</mat-icon> Editar lugar</button>
          }
        }
      </div>
      @if (establecimiento()?.lugar; as l) {
        <p class="ancho-completo suave direccion">
          {{ l.direccion || 'Sin dirección' }}{{ l.localidad ? ' · ' + l.localidad : '' }}
          {{ l.coordenadas ? ' · ' + l.coordenadas.lat.toFixed(5) + ', ' + l.coordenadas.lng.toFixed(5) : ' · sin coordenadas' }}
        </p>
      }

      <mat-form-field class="ancho-completo">
        <mat-label>Descripción</mat-label>
        <textarea matInput rows="4" formControlName="descripcion"></textarea>
      </mat-form-field>
      <mat-form-field>
        <mat-label>Año de inauguración</mat-label>
        <input matInput type="number" formControlName="anioInauguracion" />
      </mat-form-field>
      <mat-form-field>
        <mat-label>Estado de funcionamiento</mat-label>
        <input matInput formControlName="estadoFuncionamiento" placeholder="Abierto, cerrado temporalmente…" />
      </mat-form-field>
      <mat-form-field>
        <mat-label>Motivo del estado</mat-label>
        <input matInput formControlName="motivoEstado" />
      </mat-form-field>
      <mat-form-field>
        <mat-label>Fecha de reapertura</mat-label>
        <input matInput type="date" formControlName="fechaReapertura" />
      </mat-form-field>
      <mat-checkbox formControlName="destacado">Destacado en el sitio público</mat-checkbox>

      @if (editable()) {
        <div class="ancho-completo botones">
          <button mat-flat-button type="submit" [disabled]="form.invalid || form.pristine || guardando()">
            {{ establecimiento() ? 'Guardar cambios' : 'Crear establecimiento' }}
          </button>
        </div>
      }
    </form>
  `,
  styles: `
    .lugar { display: flex; gap: 8px; align-items: flex-start; flex-wrap: wrap; }
    .lugar app-selector-remoto { flex: 1 1 320px; }
    .lugar button { margin-top: 8px; }
    .direccion { margin: -4px 0 8px; }
    .botones { display: flex; justify-content: flex-end; }
  `,
})
export class EstablecimientoForm implements OnInit {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly dialog = inject(MatDialog);

  readonly establecimiento = input<Establecimiento | null>(null);
  readonly editable = input(true);
  readonly guardado = output<Establecimiento>();

  private readonly selectorLugar = viewChild.required<SelectorRemoto>('selectorLugar');
  protected readonly guardando = signal(false);
  protected readonly direccionLugar = (l: Lugar) =>
    [l.calle && `${l.calle} ${l.altura ?? ''}`.trim(), l.localidad].filter(Boolean).join(', ') || undefined;

  protected readonly form = inject(NonNullableFormBuilder).group({
    nombre: ['', [Validators.required, Validators.maxLength(300)]],
    oferenteId: [null as number | null, Validators.required],
    lugarId: [null as number | null],
    descripcion: ['', Validators.maxLength(5000)],
    anioInauguracion: [null as number | null, [Validators.min(1500), Validators.max(2100)]],
    estadoFuncionamiento: [''],
    motivoEstado: [''],
    fechaReapertura: [''],
    destacado: [false],
  });

  ngOnInit(): void {
    const e = this.establecimiento();
    if (e) {
      this.form.reset({
        nombre: e.nombre,
        oferenteId: e.oferente.id,
        lugarId: e.lugar?.id ?? null,
        descripcion: e.descripcion ?? '',
        anioInauguracion: e.anioInauguracion,
        estadoFuncionamiento: e.estadoFuncionamiento ?? '',
        motivoEstado: e.motivoEstado ?? '',
        fechaReapertura: e.fechaReapertura ?? '',
        destacado: e.destacado,
      });
    }
    if (!this.editable()) {
      this.form.disable();
    }
  }

  protected nuevoLugar(): void {
    const nombre = this.form.controls.nombre.value;
    this.dialog
      .open(LugarDialog, { data: nombre ? ({ nombre } as Lugar) : null, width: '720px' })
      .afterClosed()
      .subscribe((l?: Lugar) => this.fijarLugar(l));
  }

  protected editarLugar(): void {
    const id = this.form.controls.lugarId.value;
    this.api.get<Lugar>(`/lugares/${id}`).subscribe({
      next: (l) =>
        this.dialog.open(LugarDialog, { data: l, width: '720px' }).afterClosed().subscribe((r?: Lugar) => this.fijarLugar(r)),
      error: (e) => this.avisos.error(e),
    });
  }

  private fijarLugar(l?: Lugar): void {
    if (l) {
      this.selectorLugar().fijar(l.id, l.nombre);
      this.form.markAsDirty();
    }
  }

  protected guardar(): void {
    const f = this.form.getRawValue();
    const e = this.establecimiento();
    const cuerpo = {
      oferenteId: f.oferenteId,
      lugarId: f.lugarId,
      nombre: f.nombre.trim(),
      descripcion: f.descripcion || null,
      anioInauguracion: f.anioInauguracion,
      estadoFuncionamiento: f.estadoFuncionamiento || null,
      motivoEstado: f.motivoEstado || null,
      fechaReapertura: f.fechaReapertura || null,
      destacado: f.destacado,
      activo: e ? e.activo : null,
    };
    this.guardando.set(true);
    const pedido = e
      ? this.api.put<Establecimiento>(`/establecimientos/${e.id}`, cuerpo)
      : this.api.post<Establecimiento>('/establecimientos', cuerpo);
    pedido.subscribe({
      next: (r) => {
        this.guardando.set(false);
        this.form.markAsPristine();
        this.avisos.ok(e ? 'Cambios guardados' : 'Establecimiento creado');
        this.guardado.emit(r);
      },
      error: (err) => {
        this.guardando.set(false);
        this.avisos.error(err);
      },
    });
  }
}
