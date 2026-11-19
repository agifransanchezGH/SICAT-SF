import { Component, DestroyRef, effect, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { debounceTime } from 'rxjs';

import { Api } from '../core/api';
import { Avisos, erroresDe } from '../core/avisos';
import { CambiosEnDatos } from '../core/contadores';
import { Confirmar } from '../core/confirmar';
import { EstadoChip } from '../core/estado';
import { Establecimiento, EstadoPublicacionInfo, Requisito } from '../core/modelos';

/**
 * Barra fija de la ficha: estado de publicación, qué falta (cada faltante es un acceso a dónde se arregla) y el
 * botón Publicar/Despublicar. Se actualiza sola después de cualquier guardado en cualquier pestaña.
 */
@Component({
  selector: 'app-checklist-publicacion',
  imports: [MatButtonModule, MatIconModule, MatProgressSpinnerModule, EstadoChip],
  template: `
    <section class="barra" aria-labelledby="checklist-titulo">
      <h2 id="checklist-titulo" class="cdk-visually-hidden">Publicación</h2>
      @if (info(); as i) {
        <div class="estado">
          <app-estado [valor]="i.estado" />
          @if (i.estado === 'PUBLICADO') {
            <span class="texto">Se ve en el sitio público.</span>
          } @else if (i.puedePublicarse) {
            <span class="texto listo"><mat-icon aria-hidden="true">check_circle</mat-icon> Cumple todo para publicarse.</span>
          } @else {
            <span class="texto">Para publicar falta{{ i.requisitos.length === 1 ? '' : 'n' }}:</span>
          }
        </div>
        @if (i.estado !== 'PUBLICADO' && i.requisitos.length) {
          <ul class="faltantes" aria-label="Requisitos que faltan">
            @for (r of i.requisitos; track r.codigo) {
              <li>
                <button type="button" class="faltante" [title]="r.mensaje" (click)="irA.emit(r.codigo)">
                  <mat-icon aria-hidden="true">close</mat-icon>
                  <span>{{ r.texto }}</span>
                  <span class="cdk-visually-hidden">: {{ r.mensaje }}. {{ accionDe(r.codigo) }}</span>
                  <mat-icon class="ir" aria-hidden="true">arrow_forward</mat-icon>
                </button>
              </li>
            }
          </ul>
        }
        @if (editable()) {
          <div class="accion">
            @if (i.estado === 'PUBLICADO') {
              <button mat-stroked-button type="button" [disabled]="enviando()" (click)="despublicar()">
                <mat-icon>visibility_off</mat-icon> Despublicar
              </button>
            } @else {
              <button mat-flat-button type="button" disabledInteractive [disabled]="!i.puedePublicarse || enviando()"
                      [attr.aria-describedby]="i.puedePublicarse ? null : 'checklist-titulo'" (click)="publicar()">
                <mat-icon>public</mat-icon> Publicar
              </button>
            }
          </div>
        }
      } @else if (error()) {
        <p class="texto" role="alert">No se pudo leer el estado de publicación.
          <button type="button" class="reintentar" (click)="cargar()">Reintentar</button></p>
      } @else {
        <mat-progress-spinner mode="indeterminate" diameter="20" aria-label="Cargando estado de publicación" />
      }
    </section>
  `,
  styles: `
    :host { position: sticky; top: 0; z-index: 3; display: block; margin-bottom: 12px; }
    .barra {
      display: flex; flex-wrap: wrap; align-items: center; gap: 8px 16px; min-height: 52px; padding: 8px 12px 8px 16px;
      background: var(--mat-sys-surface); border: 1px solid var(--sicat-borde); border-radius: 12px;
      box-shadow: 0 2px 8px rgb(22 38 42 / 8%);
    }
    .estado { display: flex; align-items: center; gap: 8px; }
    .texto { font-size: 14px; color: var(--sicat-tinta); }
    .listo { color: var(--sicat-ok); display: inline-flex; align-items: center; gap: 4px; }
    .listo mat-icon { font-size: 18px; width: 18px; height: 18px; }
    .faltantes { list-style: none; margin: 0; padding: 0; display: flex; flex-wrap: wrap; gap: 6px; }
    .faltante {
      display: inline-flex; align-items: center; gap: 4px; min-height: 32px; padding: 0 8px 0 6px;
      border: 1px solid color-mix(in srgb, var(--sicat-error) 70%, white); border-radius: 16px;
      background: color-mix(in srgb, var(--sicat-error) 7%, white); color: var(--sicat-error);
      font: inherit; font-size: 13.5px; font-weight: 600; cursor: pointer;
    }
    .faltante mat-icon { font-size: 16px; width: 16px; height: 16px; }
    .faltante .ir { opacity: 0.7; }
    .faltante:hover { background: color-mix(in srgb, var(--sicat-error) 13%, white); }
    .faltante:focus-visible { outline: 2px solid var(--sicat-error); outline-offset: 2px; }
    .accion { margin-left: auto; }
    .reintentar { border: 0; background: none; color: var(--sicat-rio); font: inherit; text-decoration: underline; cursor: pointer; }
  `,
})
export class ChecklistPublicacion {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly confirmar = inject(Confirmar);

  readonly establecimiento = input.required<Establecimiento>();
  readonly editable = input(true);
  /** Se pidió ir a arreglar un requisito: la ficha cambia de pestaña o abre lo que corresponda. */
  readonly irA = output<Requisito>();
  readonly cambio = output<Establecimiento>();

  protected readonly info = signal<EstadoPublicacionInfo | null>(null);
  protected readonly error = signal(false);
  protected readonly enviando = signal(false);

  constructor() {
    // Al cambiar de establecimiento (o al guardar sus datos) se vuelve a leer.
    effect(() => {
      this.establecimiento();
      this.cargar();
    });
    // Después de cualquier escritura (ofertas, lugar, contactos…) puede haber cambiado lo que falta.
    inject(CambiosEnDatos).ocurrio.pipe(debounceTime(300), takeUntilDestroyed(inject(DestroyRef)))
      .subscribe(() => this.cargar());
  }

  cargar(): void {
    this.error.set(false);
    this.api.get<EstadoPublicacionInfo>(`/establecimientos/${this.establecimiento().id}/publicacion`).subscribe({
      next: (i) => this.info.set(i),
      error: () => this.error.set(true),
    });
  }

  protected accionDe(r: Requisito): string {
    return {
      ESTABLECIMIENTO_ACTIVO: 'Se reactiva desde Más acciones.',
      OFERENTE_ACTIVO: 'Abre el oferente.',
      LUGAR: 'Va al campo Ubicación.',
      COORDENADAS: 'Abre la ubicación en el mapa.',
      UBICACION_VERIFICADA: 'Abre la ubicación en el mapa.',
      OFERTA_ACTIVA: 'Va a la pestaña Ofertas.',
    }[r];
  }

  protected publicar(): void {
    if (!this.info()?.puedePublicarse) {
      return;
    }
    this.enviar('publicar', 'Establecimiento publicado');
  }

  protected despublicar(): void {
    this.confirmar
      .pedir('Despublicar', 'Deja de mostrarse en el sitio público y queda oculto. Se puede volver a publicar cuando quieras.', 'Despublicar')
      .subscribe((ok) => ok && this.enviar('despublicar', 'Establecimiento despublicado'));
  }

  private enviar(cual: 'publicar' | 'despublicar', mensaje: string): void {
    this.enviando.set(true);
    this.api.post<Establecimiento>(`/establecimientos/${this.establecimiento().id}/${cual}`).subscribe({
      next: (e) => {
        this.enviando.set(false);
        this.avisos.ok(mensaje);
        this.cambio.emit(e);
      },
      error: (err) => {
        this.enviando.set(false);
        if (erroresDe(err).length) {
          this.cargar();
        }
        this.avisos.error(err);
      },
    });
  }
}
