import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';

import { Establecimiento } from '../core/modelos';
import { EstablecimientoForm } from './establecimiento-form';
import { HistorialNavegacion } from '../core/navegacion';

@Component({
  selector: 'app-establecimiento-nuevo-page',
  imports: [EstablecimientoForm, MatButtonModule, MatIconModule],
  template: `
    <section class="pagina">
      <div class="encabezado">
        <button mat-icon-button type="button" aria-label="Volver" (click)="irAtras()"><mat-icon>arrow_back</mat-icon></button>
        <h1>Nuevo establecimiento</h1>
      </div>
      <p class="suave">Se crea como borrador. Después vas a poder cargar ofertas, contactos y características, y publicarlo.</p>
      <div class="tarjeta">
        <app-establecimiento-form (guardado)="creado($event)" />
      </div>
    </section>
  `,
})
export class EstablecimientoNuevoPage {
  private readonly historial = inject(HistorialNavegacion);

  /** A la página de la que se vino; si se entró directo, a /establecimientos. */
  protected irAtras(): void {
    this.historial.volver('/establecimientos');
  }

  private readonly router = inject(Router);

  protected creado(e: Establecimiento): void {
    this.router.navigate(['/establecimientos', e.id], { queryParams: { pestania: 'ofertas' } });
  }
}
