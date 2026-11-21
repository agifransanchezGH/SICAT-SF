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

import { avisarSiHayCambios } from '../core/cambios-pendientes';

/** Datos generales de un establecimiento: se usa en el alta y en la pestaña "Datos" de la ficha. */
@Component({
  selector: 'app-establecimiento-form',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatCheckboxModule, MatButtonModule, MatIconModule, SelectorRemoto],
  template: `
    <form class="secciones" [formGroup]="form" (ngSubmit)="guardar()">
      <section class="seccion" aria-labelledby="sec-identificacion">
        <div class="seccion-cabeza">
          <h2 id="sec-identificacion">Identificación</h2>
          <p class="seccion-ayuda">Cómo se llama y quién es el titular: la persona o empresa responsable.</p>
        </div>
        <div class="campos">
          <mat-form-field class="ancho-completo">
            <mat-label>Nombre</mat-label>
            <input matInput formControlName="nombre" />
          </mat-form-field>
          <app-selector-remoto class="ancho-completo" formControlName="oferenteId" ruta="/oferentes" etiqueta="Oferente (titular)"
                               [textoInicial]="establecimiento()?.oferente?.nombre" />
        </div>
      </section>

      <section class="seccion" aria-labelledby="sec-ubicacion">
        <div class="seccion-cabeza">
          <h2 id="sec-ubicacion">Ubicación</h2>
          <p class="seccion-ayuda">El lugar donde funciona. Para publicarlo necesita coordenadas verificadas.</p>
        </div>
        <div class="campos">
          <div class="ancho-completo lugar">
            <app-selector-remoto #selectorLugar formControlName="lugarId" ruta="/lugares" etiqueta="Lugar"
                                 [textoInicial]="establecimiento()?.lugar?.nombre" [detalle]="direccionLugar"
                                 ayuda="Buscá por nombre o calle" />
            @if (editable()) {
              <div class="lugar-botones">
                <button mat-stroked-button type="button" (click)="nuevoLugar()"><mat-icon>add_location</mat-icon> Nuevo lugar</button>
                @if (form.controls.lugarId.value) {
                  <button mat-stroked-button type="button" (click)="editarLugar()"><mat-icon>edit_location</mat-icon> Editar lugar</button>
                }
              </div>
            }
          </div>
          @if (establecimiento()?.lugar; as l) {
            <p class="ancho-completo direccion">
              <mat-icon aria-hidden="true">place</mat-icon>
              <span>
                {{ l.direccion || 'Sin dirección' }}{{ l.localidad ? ', ' + l.localidad : '' }}
                <span class="coordenadas">{{ l.coordenadas ? l.coordenadas.lat.toFixed(5) + ', ' + l.coordenadas.lng.toFixed(5) : 'Sin coordenadas' }}</span>
              </span>
            </p>
          }
        </div>
      </section>

      <section class="seccion" aria-labelledby="sec-textos">
        <div class="seccion-cabeza">
          <h2 id="sec-textos">Textos</h2>
          <p class="seccion-ayuda">La descripción se muestra en el sitio público. Las notas internas quedan solo para el equipo.</p>
        </div>
        <div class="campos">
          <mat-form-field class="ancho-completo">
            <mat-label>Descripción (pública)</mat-label>
            <textarea matInput rows="5" formControlName="descripcion"></textarea>
          </mat-form-field>
          <mat-form-field class="ancho-completo">
            <mat-label>Notas internas</mat-label>
            <textarea matInput rows="3" formControlName="notasInternas"></textarea>
          </mat-form-field>
        </div>
      </section>

      <section class="seccion" aria-labelledby="sec-funcionamiento">
        <div class="seccion-cabeza">
          <h2 id="sec-funcionamiento">Funcionamiento</h2>
          <p class="seccion-ayuda">Si está abierto, cerrado por un tiempo o cuándo reabre.</p>
        </div>
        <div class="campos">
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
          <mat-form-field>
            <mat-label>Año de inauguración</mat-label>
            <input matInput type="number" formControlName="anioInauguracion" />
          </mat-form-field>
          <mat-checkbox class="ancho-completo" formControlName="destacado">Destacado en el sitio público</mat-checkbox>
        </div>
      </section>

      @if (editable()) {
        <div class="barra-guardar">
          @if (form.dirty) { <span class="estado-cambios" role="status">Hay cambios sin guardar</span> }
          @if (enCola()) {
            <button mat-stroked-button type="button" (click)="guardar(true)" [disabled]="form.invalid || form.pristine || guardando()">
              Guardar y pasar al siguiente
            </button>
          }
          <button mat-flat-button type="submit" [disabled]="form.invalid || form.pristine || guardando()">
            {{ establecimiento() ? 'Guardar cambios' : 'Crear establecimiento' }}
          </button>
        </div>
      }
    </form>
  `,
  styles: `
    .lugar { display: flex; gap: 8px 12px; align-items: flex-start; flex-wrap: wrap; }
    .lugar app-selector-remoto { flex: 1 1 320px; min-width: 0; }
    /* Los botones se alinean con la caja del campo (52px a densidad -1), no con su ayuda. */
    .lugar-botones { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; min-height: 52px; }
    .direccion { display: flex; gap: 6px; align-items: flex-start; margin: 0 0 8px; font-size: 13.5px; color: var(--sicat-tinta); }
    .direccion mat-icon { flex: none; font-size: 18px; width: 18px; height: 18px; color: var(--sicat-texto-suave); }
    .coordenadas { display: block; color: var(--sicat-texto-suave); font-variant-numeric: tabular-nums; }
    mat-checkbox { margin: 0 0 8px -8px; }
  `,
})
export class EstablecimientoForm implements OnInit {
  private readonly avisoDeCambios = avisarSiHayCambios('Datos', () => this.form.dirty);
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly dialog = inject(MatDialog);

  readonly establecimiento = input<Establecimiento | null>(null);
  readonly editable = input(true);
  readonly guardado = output<Establecimiento>();
  /** La ficha se abrió desde una cola: se ofrece "Guardar y pasar al siguiente". */
  readonly enCola = input(false);
  readonly pasarAlSiguiente = output<void>();

  private readonly selectorLugar = viewChild.required<SelectorRemoto>('selectorLugar');
  protected readonly guardando = signal(false);
  protected readonly direccionLugar = (l: Lugar) =>
    [l.calle && `${l.calle} ${l.altura ?? ''}`.trim(), l.localidad].filter(Boolean).join(', ') || undefined;

  protected readonly form = inject(NonNullableFormBuilder).group({
    nombre: ['', [Validators.required, Validators.maxLength(300)]],
    oferenteId: [null as number | null, Validators.required],
    lugarId: [null as number | null],
    descripcion: ['', Validators.maxLength(5000)],
    notasInternas: ['', Validators.maxLength(20000)],
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
        notasInternas: e.notasInternas ?? '',
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

  /** El checklist pidió "un lugar asignado": se lleva el foco al buscador de lugar. */
  enfocarLugar(): void {
    const campo = document.querySelector<HTMLInputElement>('app-establecimiento-form [formcontrolname="lugarId"] input');
    campo?.scrollIntoView({ block: 'center' });
    campo?.focus();
  }

  private fijarLugar(l?: Lugar): void {
    if (l) {
      this.selectorLugar().fijar(l.id, l.nombre);
      this.form.markAsDirty();
    }
  }

  protected guardar(yPasarAlSiguiente = false): void {
    const f = this.form.getRawValue();
    const e = this.establecimiento();
    const cuerpo = {
      oferenteId: f.oferenteId,
      lugarId: f.lugarId,
      nombre: f.nombre.trim(),
      descripcion: f.descripcion || null,
      notasInternas: f.notasInternas || null,
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
        if (yPasarAlSiguiente) {
          this.pasarAlSiguiente.emit();
        }
      },
      error: (err) => {
        this.guardando.set(false);
        this.avisos.error(err);
      },
    });
  }
}
