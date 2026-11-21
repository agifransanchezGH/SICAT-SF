import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { Api } from '../core/api';
import { Avisos, erroresDe } from '../core/avisos';
import { Confirmar } from '../core/confirmar';
import { EstadoChip } from '../core/estado';
import { Establecimiento, EstadoPublicacion, EstadoPublicacionInfo } from '../core/modelos';

/** Estado de publicación, requisitos que faltan y acciones publicar / despublicar (ADR 0008). */
@Component({
  selector: 'app-publicacion-panel',
  imports: [MatButtonModule, MatIconModule, EstadoChip],
  template: `
    @if (info(); as i) {
      <div class="secciones">
        <section class="seccion" aria-labelledby="pub-estado">
          <div class="seccion-cabeza">
            <h2 id="pub-estado">Estado en el sitio</h2>
            <p class="seccion-ayuda">Si un establecimiento publicado deja de cumplir los requisitos, pasa a "Oculto" solo.</p>
          </div>
          <div class="estado-fila">
            <app-estado [valor]="i.estado" />
            <span class="explicacion">{{ explicacion(i.estado) }}</span>
            @if (editable()) {
              @if (i.estado !== 'PUBLICADO') {
                <button mat-flat-button [disabled]="!i.puedePublicarse || enviando()" (click)="publicar()">
                  <mat-icon>public</mat-icon> Publicar
                </button>
              } @else {
                <button mat-stroked-button [disabled]="enviando()" (click)="despublicar()">
                  <mat-icon>visibility_off</mat-icon> Despublicar
                </button>
              }
            }
          </div>
        </section>

        <section class="seccion" aria-labelledby="pub-requisitos">
          <div class="seccion-cabeza">
            <h2 id="pub-requisitos">Requisitos</h2>
            <p class="seccion-ayuda">
              Ubicación con coordenadas verificadas, oferente y establecimiento activos, y al menos una oferta activa con
              categoría activa.
            </p>
          </div>
          @if (i.faltantes.length) {
            <div>
              <p class="titulo-lista">Para publicarlo falta:</p>
              <ul class="faltantes">
                @for (f of i.faltantes; track f) {
                  <li><mat-icon aria-hidden="true">cancel</mat-icon><span>{{ f }}</span></li>
                }
              </ul>
            </div>
          } @else {
            <p class="ok"><mat-icon aria-hidden="true">check_circle</mat-icon> Cumple todos los requisitos para publicarse.</p>
          }
        </section>
      </div>
    } @else {
      <p class="vacio">Cargando estado de publicación…</p>
    }
  `,
  styles: `
    .estado-fila { display: flex; align-items: center; gap: 8px 16px; flex-wrap: wrap; padding: 4px 0 8px; }
    .explicacion { flex: 1 1 240px; color: var(--sicat-tinta); }
    .titulo-lista { margin: 4px 0 8px; font-weight: 600; }
    .faltantes { list-style: none; padding: 0; margin: 0 0 8px; display: flex; flex-direction: column; gap: 6px; }
    .faltantes li { display: flex; gap: 8px; align-items: flex-start; }
    .faltantes mat-icon { flex: none; font-size: 20px; width: 20px; height: 20px; color: var(--sicat-error); }
    .ok { display: flex; gap: 8px; align-items: center; margin: 4px 0 8px; color: var(--sicat-ok); font-weight: 600; }
    .ok mat-icon { font-size: 20px; width: 20px; height: 20px; }
  `,
})
export class PublicacionPanel implements OnInit {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly confirmar = inject(Confirmar);

  readonly establecimientoId = input.required<number>();
  readonly editable = input(true);
  readonly cambio = output<Establecimiento>();

  protected readonly info = signal<EstadoPublicacionInfo | null>(null);
  protected readonly enviando = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  protected explicacion(estado: EstadoPublicacion): string {
    return {
      BORRADOR: 'Todavía no se muestra en el sitio público.',
      PUBLICADO: 'Se muestra en el sitio público.',
      OCULTO: 'Estuvo publicado y ahora no se muestra en el sitio.',
    }[estado];
  }

  cargar(): void {
    this.api.get<EstadoPublicacionInfo>(`/establecimientos/${this.establecimientoId()}/publicacion`).subscribe({
      next: (i) => this.info.set(i),
      error: (e) => this.avisos.error(e),
    });
  }

  protected publicar(): void {
    this.accion('publicar', 'Establecimiento publicado');
  }

  protected despublicar(): void {
    this.confirmar
      .pedir('Despublicar', 'Deja de mostrarse en el sitio público y queda oculto. Se puede volver a publicar cuando quieras.', 'Despublicar')
      .subscribe((ok) => ok && this.accion('despublicar', 'Establecimiento despublicado'));
  }

  private accion(cual: 'publicar' | 'despublicar', mensaje: string): void {
    this.enviando.set(true);
    this.api.post<Establecimiento>(`/establecimientos/${this.establecimientoId()}/${cual}`).subscribe({
      next: (e) => {
        this.enviando.set(false);
        this.avisos.ok(mensaje);
        this.cambio.emit(e);
        this.cargar();
      },
      error: (err) => {
        this.enviando.set(false);
        const faltantes = erroresDe(err);
        if (faltantes.length) {
          this.info.update((i) => (i ? { ...i, puedePublicarse: false, faltantes } : i));
        }
        this.avisos.error(err);
      },
    });
  }
}
