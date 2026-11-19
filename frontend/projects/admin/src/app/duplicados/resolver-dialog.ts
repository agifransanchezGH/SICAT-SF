import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatProgressBarModule } from '@angular/material/progress-bar';

import { Api } from '../core/api';
import { mensajeDeError } from '../core/avisos';
import { DuplicadoItem, GrupoDuplicados } from '../core/modelos';

/** POST /establecimientos/duplicados/resolucion/previa */
export interface PreviaResolucion {
  conservarId: number;
  contactos: number;
  caracteristicas: number;
  origenes: {
    id: number;
    nombre: string;
    contactos: { tipo: string; valor: string; esPublico: boolean }[];
    caracteristicas: { grupo: string; nombre: string; cantidad: number | null }[];
  }[];
}

export interface DatosResolver {
  grupo: GrupoDuplicados;
  queda: DuplicadoItem;
}

/** Lo que se eligió en el diálogo (undefined si se canceló). */
export interface Resolucion {
  copiarContactos: boolean;
  copiarCaracteristicas: boolean;
}

/**
 * Confirmación de "Quedarme con este": quiénes se dan de baja y, si al que queda le faltan contactos o
 * características de los demás, cuáles se le copiarían (las casillas arrancan tildadas; se copian, no se mueven).
 */
@Component({
  selector: 'app-resolver-dialog',
  imports: [FormsModule, MatDialogModule, MatButtonModule, MatCheckboxModule, MatProgressBarModule],
  template: `
    <h2 mat-dialog-title>Quedarme con "{{ d.queda.nombre }}" (#{{ d.queda.id }})</h2>
    <mat-dialog-content>
      <p>
        Se {{ otros.length === 1 ? 'da' : 'dan' }} de baja {{ otros.length === 1 ? 'el otro' : 'los otros ' + otros.length }}
        ({{ ids }}).
        @if (publicados) {
          {{ publicados === 1 ? 'Uno está publicado y deja' : publicados + ' están publicados y dejan' }} de verse en el sitio.
        }
        Se pueden reactivar desde su ficha.
      </p>

      @if (cargando()) {
        <mat-progress-bar mode="indeterminate" aria-label="Buscando qué le falta al que queda" />
      } @else if (error()) {
        <p class="error" role="alert">No se pudo calcular qué le falta: {{ error() }}. Podés seguir solo con la baja.</p>
      } @else if (previa(); as p) {
        @if (!p.contactos && !p.caracteristicas) {
          <p class="suave">El que queda ya tiene todos los contactos y características de los demás.</p>
        } @else {
          <h3>Pasarle lo que le falta</h3>
          <p class="suave">Se copian al que queda (los dados de baja conservan los suyos). No se repite lo que ya tiene, aunque esté escrito distinto.</p>
          @if (p.contactos) {
            <mat-checkbox [(ngModel)]="copiarContactos">
              Copiar {{ p.contactos }} {{ p.contactos === 1 ? 'contacto' : 'contactos' }}
            </mat-checkbox>
            <ul class="detalle" [class.apagado]="!copiarContactos">
              @for (o of p.origenes; track o.id) {
                @for (c of o.contactos; track $index) {
                  <li>{{ c.tipo }}: {{ c.valor }}{{ c.esPublico ? '' : ' (interno)' }} <span class="suave">· de #{{ o.id }}</span></li>
                }
              }
            </ul>
          }
          @if (p.caracteristicas) {
            <mat-checkbox [(ngModel)]="copiarCaracteristicas">
              Agregar {{ p.caracteristicas }} {{ p.caracteristicas === 1 ? 'característica' : 'características' }}
            </mat-checkbox>
            <ul class="detalle" [class.apagado]="!copiarCaracteristicas">
              @for (o of p.origenes; track o.id) {
                @for (c of o.caracteristicas; track $index) {
                  <li>{{ c.grupo }}: {{ c.nombre }}{{ c.cantidad != null ? ' (' + c.cantidad + ')' : '' }} <span class="suave">· de #{{ o.id }}</span></li>
                }
              }
            </ul>
          }
        }
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="undefined">Cancelar</button>
      <button mat-flat-button [disabled]="cargando()" (click)="aceptar()">{{ boton() }}</button>
    </mat-dialog-actions>
  `,
  styles: `
    h3 { font-size: 15px; margin: 16px 0 4px; }
    .detalle { margin: 2px 0 10px; padding-left: 40px; font-size: 13.5px; max-height: 160px; overflow: auto; }
    .apagado { opacity: 0.55; text-decoration: line-through; }
    .error { color: var(--sicat-error); }
    mat-checkbox { display: block; }
  `,
})
export class ResolverDialog {
  private readonly api = inject(Api);
  private readonly ref = inject(MatDialogRef<ResolverDialog, Resolucion>);
  protected readonly d = inject<DatosResolver>(MAT_DIALOG_DATA);

  protected readonly otros = this.d.grupo.establecimientos.filter((e) => e.id !== this.d.queda.id);
  protected readonly ids = this.otros.map((e) => `#${e.id}`).join(', ');
  protected readonly publicados = this.otros.filter((e) => e.estadoPublicacion === 'PUBLICADO').length;
  protected readonly previa = signal<PreviaResolucion | null>(null);
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected copiarContactos = true;
  protected copiarCaracteristicas = true;

  constructor() {
    this.api
      .post<PreviaResolucion>('/establecimientos/duplicados/resolucion/previa', {
        conservarId: this.d.queda.id,
        grupo: { clave: this.d.grupo.clave, integrantes: this.d.grupo.establecimientos.map((e) => e.id) },
      })
      .subscribe({
        next: (p) => {
          this.previa.set(p);
          this.cargando.set(false);
        },
        error: (e) => {
          this.error.set(mensajeDeError(e));
          this.cargando.set(false);
        },
      });
  }

  protected boton(): string {
    const p = this.previa();
    const copia = !!p && ((this.copiarContactos && p.contactos > 0) || (this.copiarCaracteristicas && p.caracteristicas > 0));
    const baja = this.otros.length === 1 ? 'dar de baja el otro' : `dar de baja ${this.otros.length}`;
    return copia ? `Copiar y ${baja}` : baja.charAt(0).toUpperCase() + baja.slice(1);
  }

  protected aceptar(): void {
    const p = this.previa();
    this.ref.close({
      copiarContactos: !!p && p.contactos > 0 && this.copiarContactos,
      copiarCaracteristicas: !!p && p.caracteristicas > 0 && this.copiarCaracteristicas,
    });
  }
}
