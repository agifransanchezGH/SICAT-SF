import { DatePipe } from '@angular/common';
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
  imports: [DatePipe],
  template: `
    <div class="historial">
      @for (a of registros(); track a.id) {
        <article>
          <header>
            <strong>{{ acciones[a.accion] }}</strong>
            <span class="suave">· {{ a.fechaHora | date: 'dd/MM/yyyy HH:mm' }} · {{ a.usuario?.nombreCompleto ?? a.origen }}</span>
          </header>
          @if (a.accion === 'MODIFICACION') {
            <ul>
              @for (c of cambios(a); track c.campo) {
                <li><code>{{ c.campo }}</code>: {{ mostrar(c.antes) }} → <strong>{{ mostrar(c.despues) }}</strong></li>
              }
            </ul>
          }
        </article>
      } @empty {
        <p class="vacio">{{ cargado() ? 'Sin cambios registrados.' : 'Cargando…' }}</p>
      }
    </div>
  `,
  styles: `
    .historial { padding: 16px; display: flex; flex-direction: column; gap: 12px; }
    article { border-left: 3px solid var(--sicat-borde); padding-left: 12px; }
    ul { margin: 4px 0 0; padding-left: 18px; font-size: 14px; word-break: break-word; }
    code { font-size: 13px; }
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
