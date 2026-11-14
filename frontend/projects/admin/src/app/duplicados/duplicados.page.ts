import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';

import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { Avisos } from '../core/avisos';
import { Confirmar } from '../core/confirmar';
import { EstadoChip } from '../core/estado';
import { GrupoDuplicados } from '../core/modelos';

/** Establecimientos activos con el mismo nombre normalizado, para revisar y dar de baja los repetidos. */
@Component({
  selector: 'app-duplicados-page',
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule, MatTooltipModule, EstadoChip],
  template: `
    <section class="pagina">
      <div class="encabezado">
        <h1>Posibles duplicados</h1>
        <button mat-stroked-button (click)="cargar()"><mat-icon>refresh</mat-icon> Actualizar</button>
      </div>
      <p class="suave">
        Establecimientos activos con el mismo nombre. Los marcados "misma dirección" son casi seguro el mismo; el resto pueden ser
        sucursales. Abrí cada uno para comparar y dá de baja el que sobre.
      </p>
      @if (cargando()) { <mat-progress-bar mode="indeterminate" /> }
      @for (g of grupos(); track g.clave) {
        <div class="tarjeta grupo">
          <div class="titulo">
            <strong>{{ g.establecimientos[0].nombre }}</strong>
            @if (g.mismaDireccion) { <span class="estado NO_ENCONTRADO">Misma dirección</span> }
            <span class="suave">{{ g.establecimientos.length }} registros</span>
          </div>
          @for (e of g.establecimientos; track e.id) {
            <div class="fila">
              <a [routerLink]="['/establecimientos', e.id]">#{{ e.id }} · {{ e.nombre }}</a>
              <span class="suave">{{ e.direccion || 'Sin dirección' }}</span>
              <app-estado [valor]="e.estadoPublicacion" />
              @if (esAdmin) {
                <button mat-icon-button matTooltip="Dar de baja este" (click)="darDeBaja(e.id, e.nombre)"><mat-icon>delete</mat-icon></button>
              }
            </div>
          }
        </div>
      } @empty {
        @if (!cargando()) { <p class="vacio tarjeta">No se encontraron posibles duplicados.</p> }
      }
    </section>
  `,
  styles: `
    .grupo { padding: 12px 16px; }
    .titulo { display: flex; gap: 12px; align-items: center; margin-bottom: 4px; }
    .fila { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; min-height: 40px; }
    .fila a { min-width: 280px; }
    .fila .suave { flex: 1; }
  `,
})
export class DuplicadosPage {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly confirmar = inject(Confirmar);
  protected readonly esAdmin = inject(AuthService).tieneRol('ADMIN');

  protected readonly grupos = signal<GrupoDuplicados[]>([]);
  protected readonly cargando = signal(false);

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.api.get<GrupoDuplicados[]>('/establecimientos/duplicados').subscribe({
      next: (g) => {
        this.grupos.set([...g].sort((a, b) => Number(b.mismaDireccion) - Number(a.mismaDireccion)));
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        this.avisos.error(e);
      },
    });
  }

  protected darDeBaja(id: number, nombre: string): void {
    this.confirmar.pedir('Dar de baja', `¿Dar de baja "${nombre}" (#${id})? Se puede reactivar desde su ficha.`, 'Dar de baja').subscribe((ok) => {
      if (ok) {
        this.api.delete(`/establecimientos/${id}`).subscribe({
          next: () => {
            this.avisos.ok('Establecimiento dado de baja');
            this.cargar();
          },
          error: (e) => this.avisos.error(e),
        });
      }
    });
  }
}
