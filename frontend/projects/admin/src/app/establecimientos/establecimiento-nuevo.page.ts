import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';

import { Establecimiento } from '../core/modelos';
import { EstablecimientoForm } from './establecimiento-form';

@Component({
  selector: 'app-establecimiento-nuevo-page',
  imports: [EstablecimientoForm, RouterLink, MatButtonModule, MatIconModule],
  template: `
    <section class="pagina">
      <div class="encabezado">
        <a mat-icon-button routerLink="/establecimientos" aria-label="Volver"><mat-icon>arrow_back</mat-icon></a>
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
  private readonly router = inject(Router);

  protected creado(e: Establecimiento): void {
    this.router.navigate(['/establecimientos', e.id], { queryParams: { pestania: 'ofertas' } });
  }
}
