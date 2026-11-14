import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Mapa de establecimientos y circuitos publicados (Leaflet + OpenStreetMap): llega en el paso 10. */
@Component({
  selector: 'app-mapa-page',
  imports: [RouterLink],
  template: `
    <div class="contenido">
      <p class="tarjeta vacio">El mapa llega pronto. Mientras tanto, <a routerLink="/">buscá en el listado</a>.</p>
    </div>
  `,
})
export class MapaPage {}
