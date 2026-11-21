import { DatePipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { Component, OnInit, inject, input, signal } from '@angular/core';

import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { Auditoria } from '../core/modelos';

export interface CambioCampo {
  campo: string;
  antes: unknown;
  despues: unknown;
}

/** Campos que cambiaron entre datosAnteriores y datosNuevos de un registro de auditoría. */
export function cambios(a: Auditoria): CambioCampo[] {
  const antes = a.datosAnteriores ?? {};
  const despues = a.datosNuevos ?? {};
  const campos = new Set([...Object.keys(antes), ...Object.keys(despues)]);
  return [...campos]
    .filter((c) => JSON.stringify(antes[c]) !== JSON.stringify(despues[c]))
    .map((campo) => ({ campo, antes: antes[campo], despues: despues[campo] }));
}

export function mostrar(v: unknown): string {
  if (v === null || v === undefined) {
    return '—';
  }
  return typeof v === 'object' ? JSON.stringify(v) : String(v);
}

export const ACCIONES: Record<string, string> = { ALTA: 'Alta', MODIFICACION: 'Modificación', ELIMINACION: 'Eliminación' };

/** Historial de cambios de un registro (auditoría), del más reciente al más antiguo. */
@Component({
  selector: 'app-historial',
  imports: [DatePipe, MatIconModule],
  template: `
    <ol class="historial" aria-label="Historial de cambios, del más reciente al más antiguo">
      @for (a of registros(); track a.id) {
        <li class="registro" [class]="'registro ' + a.accion">
          <div class="cabeza">
            <span class="accion">{{ acciones[a.accion] }}</span>
            <time class="cuando" [attr.datetime]="a.fechaHora">{{ a.fechaHora | date: 'dd/MM/yyyy HH:mm' }}</time>
            <span class="quien">{{ a.usuario?.nombreCompleto ?? a.origen }}</span>
          </div>
          @if (a.accion === 'MODIFICACION') {
            <dl class="cambios">
              @for (c of cambios(a); track c.campo) {
                <div class="cambio">
                  <dt>{{ c.campo }}</dt>
                  <dd>
                    <span class="antes">{{ mostrar(c.antes) }}</span>
                    <mat-icon aria-hidden="true">arrow_forward</mat-icon><span class="cdk-visually-hidden">pasó a</span>
                    <span class="despues">{{ mostrar(c.despues) }}</span>
                  </dd>
                </div>
              }
            </dl>
          }
        </li>
      } @empty {
        <li class="vacio">{{ cargado() ? 'Sin cambios registrados.' : 'Cargando…' }}</li>
      }
    </ol>
  `,
  styles: `
    .historial { list-style: none; margin: 0; padding: 20px 24px; display: flex; flex-direction: column; }
    /* Línea de tiempo: un punto por registro sobre una guía continua. */
    .registro { position: relative; padding: 0 0 20px 24px; }
    .registro::before {
      content: ''; position: absolute; left: 4px; top: 6px; bottom: 0; width: 1px; background: var(--sicat-borde);
    }
    .registro:last-child::before { display: none; }
    .registro::after {
      content: ''; position: absolute; left: 0; top: 4px; width: 9px; height: 9px; border-radius: 50%;
      background: var(--mat-sys-surface); border: 2px solid var(--sicat-rio); box-sizing: border-box;
    }
    .registro.ALTA::after { background: var(--sicat-rio); }
    .registro.ELIMINACION::after { border-color: var(--sicat-error); }
    .cabeza { display: flex; flex-wrap: wrap; align-items: baseline; gap: 2px 10px; line-height: 1.35; }
    .accion { font-weight: 600; }
    .cuando, .quien { font-size: 13px; color: var(--sicat-texto-suave); }
    .cuando { font-variant-numeric: tabular-nums; }
    .cambios { margin: 8px 0 0; display: grid; gap: 4px; font-size: 13.5px; }
    .cambio { display: grid; grid-template-columns: minmax(110px, 180px) minmax(0, 1fr); gap: 2px 12px; }
    dt { color: var(--sicat-texto-suave); font-stretch: var(--sicat-ancho-denso); overflow-wrap: anywhere; }
    dd { margin: 0; display: flex; flex-wrap: wrap; align-items: baseline; gap: 2px 6px; overflow-wrap: anywhere; min-width: 0; }
    dd mat-icon { font-size: 14px; width: 14px; height: 14px; align-self: center; color: var(--sicat-texto-suave); }
    .antes { color: var(--sicat-texto-suave); text-decoration: line-through; text-decoration-color: var(--sicat-borde); }
    .despues { font-weight: 600; }
    @media (max-width: 560px) { .cambio { grid-template-columns: 1fr; } }
  `,
})
export class Historial implements OnInit {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);

  readonly tabla = input.required<string>();
  readonly clave = input.required<string | number>();

  protected readonly registros = signal<Auditoria[]>([]);
  protected readonly cargado = signal(false);
  protected readonly acciones = ACCIONES;
  protected readonly cambios = cambios;
  protected readonly mostrar = mostrar;

  ngOnInit(): void {
    this.api.get<Auditoria[]>(`/auditoria/${this.tabla()}/${this.clave()}`).subscribe({
      next: (r) => {
        this.registros.set(r);
        this.cargado.set(true);
      },
      error: (e) => {
        this.cargado.set(true);
        this.avisos.error(e);
      },
    });
  }
}
