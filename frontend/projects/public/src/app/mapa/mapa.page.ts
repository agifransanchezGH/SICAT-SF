import { Component, DestroyRef, NgZone, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import * as L from 'leaflet';
import { EMPTY, catchError, combineLatest, filter, switchMap } from 'rxjs';
import { SicatMapa, agruparPorPunto, escapar, iconoGrupo, nombreLegible, popupDeGrupo } from 'shared';
import { toObservable } from '@angular/core/rxjs-interop';

import { ApiPublicaService } from '../core/api-publica.service';
import { FiltrosBusqueda } from '../core/filtros-busqueda';

/** Mapa del sitio público: establecimientos publicados (mismos filtros que el listado) y circuitos. */
@Component({
  selector: 'app-mapa-page',
  imports: [RouterLink, SicatMapa, FiltrosBusqueda, MatButtonModule, MatIconModule, MatSlideToggleModule],
  template: `
    <section class="barra-filtros">
      <div class="contenido">
        <app-filtros-busqueda />
        <div class="pie-filtros">
          <span class="suave">{{ cantidad() === 1 ? '1 lugar' : cantidad() + ' lugares' }} en el mapa</span>
          @if (hayCircuitos()) {
            <mat-slide-toggle [checked]="verCircuitos()" (change)="alternarCircuitos($event.checked)">Circuitos</mat-slide-toggle>
          }
          <span class="espacio"></span>
          <a mat-stroked-button routerLink="/" queryParamsHandling="merge"><mat-icon>list</mat-icon> Ver en lista</a>
        </div>
      </div>
    </section>
    <sicat-mapa class="mapa" (listo)="alListo($event)" />
  `,
  styles: `
    :host { display: flex; flex-direction: column; height: calc(100vh - 110px); min-height: 480px; }
    .barra-filtros { background: var(--mat-sys-surface); border-bottom: 1px solid var(--sicat-borde); }
    .barra-filtros .contenido { padding-top: 12px; padding-bottom: 8px; }
    .pie-filtros { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; margin-top: 4px; }
    .espacio { flex: 1; }
    .mapa { flex: 1; }
  `,
})
export class MapaPage {
  private readonly api = inject(ApiPublicaService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly zona = inject(NgZone);
  private readonly destroy = inject(DestroyRef);

  protected readonly cantidad = signal(0);
  protected readonly hayCircuitos = signal(false);
  protected readonly verCircuitos = signal(true);
  private readonly mapa = signal<L.Map | null>(null);
  private readonly puntos = L.featureGroup();
  private readonly lineas = L.featureGroup();
  private primeraVez = true;

  constructor() {
    inject(Title).setTitle('Mapa de lugares, Santa Fe');
    combineLatest([toObservable(this.mapa).pipe(filter((m): m is L.Map => m !== null)), this.route.queryParamMap])
      .pipe(
        switchMap(([, p]) =>
          this.api.mapa({ q: p.get('q'), categoria: p.get('categoria'), localidad: p.get('localidad') }).pipe(catchError(() => EMPTY)),
        ),
        takeUntilDestroyed(this.destroy),
      )
      .subscribe((geo) => this.dibujarPuntos(geo));
  }

  protected alListo(m: L.Map): void {
    this.puntos.addTo(m);
    this.lineas.addTo(m);
    m.on('popupopen', (e: L.PopupEvent) => {
      // Los enlaces del popup navegan con el router (sin recargar la página).
      e.popup.getElement()?.querySelectorAll<HTMLAnchorElement>('a[data-id]').forEach((a) =>
        a.addEventListener('click', (ev) => {
          ev.preventDefault();
          this.zona.run(() => this.router.navigate(['/establecimientos', a.dataset['id']]));
        }),
      );
    });
    this.mapa.set(m);
    this.api.circuitos().pipe(catchError(() => EMPTY), takeUntilDestroyed(this.destroy)).subscribe((geo) => {
      L.geoJSON(geo, {
        style: { color: '#3f6a2e', weight: 4, opacity: 0.85 },
        onEachFeature: (f, capa) => {
          const p = f.properties ?? {};
          const paradas = (p['paradas'] ?? []) as { nombre: string }[];
          capa.bindPopup(
            `<strong>Circuito</strong>${p['duracionMin'] ? ` · ${escapar(p['duracionMin'])} min` : ''}` +
              (paradas.length ? `<ol style="margin:4px 0 0;padding-left:18px">${paradas.map((x) => `<li>${escapar(x.nombre)}</li>`).join('')}</ol>` : '') +
              (p['establecimientoId'] ? `<a href="/establecimientos/${p['establecimientoId']}" data-id="${p['establecimientoId']}">Ver detalle</a>` : ''),
          );
        },
      }).addTo(this.lineas);
      this.hayCircuitos.set(geo.features.length > 0);
    });
  }

  protected alternarCircuitos(ver: boolean): void {
    this.verCircuitos.set(ver);
    const m = this.mapa();
    if (m) {
      if (ver) {
        this.lineas.addTo(m);
      } else {
        this.lineas.remove();
      }
    }
  }

  private dibujarPuntos(geo: GeoJSON.FeatureCollection<GeoJSON.Point>): void {
    const m = this.mapa()!;
    this.puntos.clearLayers();
    const bloque = (p: Record<string, unknown>) => {
      const cats = ((p['categorias'] ?? []) as string[]).join(' · ');
      return (
        `<strong>${escapar(nombreLegible(p['nombre'] as string))}</strong>` +
        (cats ? `<div style="color:#52646a;margin-top:2px">${escapar(cats)}</div>` : '') +
        `<div style="margin-top:6px"><a href="/establecimientos/${escapar(p['id'])}" data-id="${escapar(p['id'])}">Ver ficha</a></div>`
      );
    };
    // Varios establecimientos en el mismo punto: un solo marcador con la cantidad y un popup con todos.
    for (const g of agruparPorPunto(geo)) {
      const destacado = g.items.some((p) => p['destacado']);
      const color = destacado ? '#b5832a' : '#1d5c63';
      const capa =
        g.items.length === 1
          ? L.circleMarker(g.latlng, { radius: destacado ? 9 : 7, color: '#fff', weight: 2, fillColor: color, fillOpacity: 0.95 })
          : L.marker(g.latlng, { icon: iconoGrupo(g.items.length, color) });
      capa.bindPopup(g.items.length === 1 ? bloque(g.items[0]) : popupDeGrupo(g.items.map(bloque))).addTo(this.puntos);
    }
    this.cantidad.set(geo.features.length);
    // Encuadra los resultados al cambiar los filtros (no en cada paneo).
    if (geo.features.length) {
      m.fitBounds(this.puntos.getBounds(), { padding: [40, 40], maxZoom: this.primeraVez ? 15 : 16 });
    }
    this.primeraVez = false;
  }
}
