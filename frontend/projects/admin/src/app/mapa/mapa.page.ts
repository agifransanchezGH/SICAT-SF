import { Component } from '@angular/core';

/** Vista de mapa interna (Leaflet + OpenStreetMap): llega en el paso 10. */
@Component({
  selector: 'app-mapa-page',
  template: `
    <section class="pagina">
      <div class="encabezado"><h1>Mapa</h1></div>
      <p class="tarjeta vacio">El mapa de establecimientos y la edición de ubicaciones sobre el mapa llegan en el próximo paso.</p>
    </section>
  `,
})
export class MapaPage {}
