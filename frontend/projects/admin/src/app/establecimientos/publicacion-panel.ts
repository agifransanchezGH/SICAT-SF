import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { Api } from '../core/api';
import { Avisos, erroresDe } from '../core/avisos';
import { Confirmar } from '../core/confirmar';
import { EstadoChip } from '../core/estado';
import { Establecimiento, EstadoPublicacionInfo } from '../core/modelos';

/** Estado de publicación, requisitos que faltan y acciones publicar / despublicar (ADR 0008). */
@Component({
  selector: 'app-publicacion-panel',
  imports: [MatButtonModule, MatIconModule, EstadoChip],
  template: `
    <div class="panel">
      @if (info(); as i) {
        <p>Estado actual: <app-estado [valor]="i.estado" /></p>
        @if (i.faltantes.length) {
          <p><strong>Para publicarlo falta:</strong></p>
          <ul class="faltantes">
            @for (f of i.faltantes; track f) { <li><mat-icon inline>close</mat-icon> {{ f }}</li> }
          </ul>
        } @else {
          <p class="ok"><mat-icon inline>check_circle</mat-icon> Cumple todos los requisitos para publicarse.</p>
        }
        @if (editable()) {
          <div class="botones">
            @if (i.estado !== 'PUBLICADO') {
              <button mat-flat-button [disabled]="!i.puedePublicarse || enviando()" (click)="publicar()">
                <mat-icon>public</mat-icon> Publicar
              </button>
            } @else {
              <button mat-stroked-button [disabled]="enviando()" (click)="despublicar()">
                <mat-icon>visibility_off</mat-icon> Despublicar
              </button>
            }
          </div>
        }
        <p class="suave nota">
          Requisitos: ubicación con coordenadas verificadas, oferente y establecimiento activos, y al menos una oferta activa
          con categoría activa. Si un establecimiento publicado deja de cumplirlos, pasa a "Oculto".
        </p>
      }
    </div>
  `,
  styles: `
    .panel { padding: 16px; }
    .faltantes { list-style: none; padding: 0; margin: 0 0 12px; display: flex; flex-direction: column; gap: 4px; }
    .faltantes mat-icon { color: var(--sicat-error); vertical-align: -2px; }
    .ok { color: var(--sicat-ok); }
    .ok mat-icon { vertical-align: -2px; }
    .botones { display: flex; gap: 8px; margin: 12px 0; }
    .nota { font-size: 13px; }
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
