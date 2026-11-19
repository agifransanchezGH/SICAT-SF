import { LiveAnnouncer } from '@angular/cdk/a11y';
import { Component, Injectable, computed, inject, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { EMPTY, Observable, catchError, filter, map, switchMap, tap } from 'rxjs';

import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { Confirmar } from '../core/confirmar';

/** Respuesta de los endpoints en lote (POST …/lote/…). */
export interface ResultadoLote {
  procesados: number;
  exitosos: number;
  fallidos: number;
  resultados: { id: number; nombre: string | null; ok: boolean; errores: string[] }[];
}

/** "3 publicados, 2 no se pudieron publicar". */
export function resumenLote(r: ResultadoLote, participio: string, infinitivo: string): string {
  const ok = `${r.exitosos} ${r.exitosos === 1 ? participio : participio + 's'}`;
  return r.fallidos ? `${ok}, ${r.fallidos} no se ${r.fallidos === 1 ? 'pudo' : 'pudieron'} ${infinitivo}` : ok;
}

// ------------------------------------------------------------------ barra de selección

/**
 * Barra que aparece con la selección: cuántos hay, "Quitar selección" y las acciones (contenido proyectado).
 * Queda pegada abajo mientras se recorre la tabla; los botones miden 44 px de alto.
 */
@Component({
  selector: 'app-barra-seleccion',
  template: `
    <div class="barra" role="region" aria-label="Acciones sobre la selección">
      <span class="cantidad" aria-live="polite">{{ texto() }}</span>
      <button type="button" class="quitar" (click)="limpiar.emit()">Quitar selección</button>
      <span class="espacio"></span>
      <ng-content />
    </div>
  `,
  styles: `
    :host { position: sticky; bottom: 0; z-index: 2; display: block; }
    .barra {
      display: flex; flex-wrap: wrap; align-items: center; gap: 8px 12px; padding: 10px 16px;
      background: var(--sicat-tinta); color: #ffffff; border-radius: 0 0 12px 12px;
    }
    .cantidad { font-weight: 700; font-variant-numeric: tabular-nums; }
    .quitar {
      min-height: 44px; padding: 0 12px; border: 0; border-radius: 22px; background: transparent;
      color: inherit; font: inherit; text-decoration: underline; text-underline-offset: 3px; cursor: pointer;
    }
    .quitar:hover { background: rgb(255 255 255 / 10%); }
    .quitar:focus-visible { outline: 2px solid #ffffff; outline-offset: 2px; }
    .espacio { flex: 1; }
    ::ng-deep app-barra-seleccion .mdc-button { min-height: 44px; }
  `,
})
export class BarraSeleccion {
  readonly cantidad = input.required<number>();
  /** Texto en singular y plural, con la concordancia: ["ubicación seleccionada", "ubicaciones seleccionadas"]. */
  readonly nombres = input.required<[string, string]>();
  readonly limpiar = output<void>();
  protected readonly texto = computed(() => {
    const n = this.cantidad();
    return `${n} ${n === 1 ? this.nombres()[0] : this.nombres()[1]}`;
  });
}

// ------------------------------------------------------------------ diálogo de resultado

interface DatosResultado {
  titulo: string;
  resultado: ResultadoLote;
  participio: string;
  infinitivo: string;
}

/** Resultado de una acción en lote: primero los que no se pudieron, cada uno con su motivo. */
@Component({
  selector: 'app-resultado-lote',
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>{{ d.titulo }}</h2>
    <mat-dialog-content>
      <p class="resumen">{{ resumen }}</p>
      @if (fallidos.length) {
        <h3>No se {{ fallidos.length === 1 ? 'pudo' : 'pudieron' }} {{ d.infinitivo }}</h3>
        <ul class="lista">
          @for (r of fallidos; track r.id) {
            <li class="fallido">
              <mat-icon aria-hidden="true">error_outline</mat-icon>
              <div>
                <strong>{{ r.nombre ?? 'Id ' + r.id }}</strong>
                <ul class="motivos">@for (m of r.errores; track m) { <li>{{ m }}</li> }</ul>
              </div>
            </li>
          }
        </ul>
      }
      @if (exitosos.length) {
        <h3>{{ exitosos.length === 1 ? 'Listo' : 'Listos' }}</h3>
        <ul class="lista">
          @for (r of exitosos; track r.id) {
            <li class="ok"><mat-icon aria-hidden="true">check_circle</mat-icon> {{ r.nombre }}</li>
          }
        </ul>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-flat-button mat-dialog-close cdkFocusInitial>Cerrar</button>
    </mat-dialog-actions>
  `,
  styles: `
    .resumen { font-weight: 600; margin-top: 0; }
    h3 { font-size: 14px; margin: 16px 0 6px; color: var(--sicat-texto-suave); }
    .lista { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 6px; }
    .lista li { display: flex; gap: 8px; align-items: flex-start; }
    .lista mat-icon { flex: none; font-size: 20px; width: 20px; height: 20px; }
    .fallido mat-icon { color: var(--sicat-error); }
    .ok mat-icon { color: var(--sicat-ok); }
    .motivos { margin: 2px 0 0; padding-left: 18px; color: var(--sicat-texto-suave); font-size: 13.5px; }
  `,
})
class ResultadoLoteDialog {
  protected readonly d = inject<DatosResultado>(MAT_DIALOG_DATA);
  protected readonly fallidos = this.d.resultado.resultados.filter((r) => !r.ok);
  protected readonly exitosos = this.d.resultado.resultados.filter((r) => r.ok);
  protected readonly resumen = resumenLote(this.d.resultado, this.d.participio, this.d.infinitivo) + '.';
}

// ------------------------------------------------------------------ flujo completo

export interface PedidoLote {
  /** Título y texto de la confirmación previa (con la cantidad). */
  titulo: string;
  mensaje: string;
  boton: string;
  /** Ruta del endpoint: '/establecimientos/lote/publicar'. */
  ruta: string;
  ids: number[];
  /** "publicado" / "publicar": para "3 publicados, 1 no se pudo publicar". */
  participio: string;
  infinitivo: string;
}

/** Confirmar → enviar → mostrar el resultado uno por uno y anunciarlo. Emite el resultado (nada si se cancela o falla). */
@Injectable({ providedIn: 'root' })
export class AccionesEnLote {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly confirmar = inject(Confirmar);
  private readonly dialog = inject(MatDialog);
  private readonly anunciar = inject(LiveAnnouncer);

  ejecutar(p: PedidoLote): Observable<ResultadoLote> {
    return this.confirmar.pedir(p.titulo, p.mensaje, p.boton).pipe(
      filter((ok) => ok === true),
      switchMap(() =>
        this.api.post<ResultadoLote>(p.ruta, { ids: p.ids }).pipe(
          catchError((e) => {
            this.avisos.error(e);
            return EMPTY;
          }),
        ),
      ),
      tap((r) => {
        this.anunciar.announce(resumenLote(r, p.participio, p.infinitivo), 'polite');
        this.dialog.open(ResultadoLoteDialog, {
          data: { titulo: p.titulo, resultado: r, participio: p.participio, infinitivo: p.infinitivo },
          width: '560px',
        });
      }),
      map((r) => r),
    );
  }
}
