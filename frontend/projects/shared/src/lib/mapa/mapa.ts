import { AfterViewInit, Component, ElementRef, NgZone, OnDestroy, inject, input, output, viewChild } from '@angular/core';
import * as L from 'leaflet';

/** Centro de la ciudad de Santa Fe (Plaza 25 de Mayo). */
export const SANTA_FE: L.LatLngTuple = [-31.6488, -60.7087];

/**
 * Mapa base (Leaflet + OpenStreetMap). Crea el mapa y lo entrega en "listo";
 * cada pantalla agrega sus capas. Usa canvas para que cientos de puntos no pesen.
 */
@Component({
  selector: 'sicat-mapa',
  template: `<div #contenedor class="contenedor"></div>`,
  styles: `
    :host { display: block; position: relative; min-height: 240px; }
    .contenedor { position: absolute; inset: 0; border-radius: inherit; z-index: 0; }
  `,
})
export class SicatMapa implements AfterViewInit, OnDestroy {
  private readonly zona = inject(NgZone);
  private readonly contenedor = viewChild.required<ElementRef<HTMLDivElement>>('contenedor');

  readonly centro = input<L.LatLngExpression>(SANTA_FE);
  readonly zoom = input(14);
  readonly listo = output<L.Map>();

  private mapa?: L.Map;
  private observador?: ResizeObserver;

  ngAfterViewInit(): void {
    // Fuera de la zona de Angular: los eventos del mapa (mover, zoom) no disparan detección de cambios.
    this.zona.runOutsideAngular(() => {
      const mapa = L.map(this.contenedor().nativeElement, { preferCanvas: true, zoomControl: true })
        .setView(this.centro(), this.zoom());
      L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      }).addTo(mapa);
      this.mapa = mapa;
      this.observador = new ResizeObserver(() => mapa.invalidateSize());
      this.observador.observe(this.contenedor().nativeElement);
    });
    this.listo.emit(this.mapa!);
  }

  ngOnDestroy(): void {
    this.observador?.disconnect();
    this.mapa?.remove();
  }
}

/** Ícono de "pin" en CSS (sin imágenes), para marcadores arrastrables o destacados. */
export function iconoPin(color = '#d32f2f'): L.DivIcon {
  return L.divIcon({
    className: 'sicat-pin',
    html: `<span style="display:block;width:22px;height:22px;border-radius:50% 50% 50% 0;background:${color};
      transform:rotate(-45deg);border:2px solid #fff;box-shadow:0 1px 4px rgb(0 0 0 / 40%)"></span>`,
    iconSize: [22, 22],
    iconAnchor: [11, 22],
    popupAnchor: [0, -22],
  });
}

/** Escapa texto para usarlo dentro del HTML de un popup. */
export function escapar(t: unknown): string {
  return String(t ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]!);
}
