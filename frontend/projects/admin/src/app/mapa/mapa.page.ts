import { Component, DestroyRef, NgZone, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { Router } from '@angular/router';
import * as L from 'leaflet';
import { EMPTY, catchError, combineLatest, debounceTime, filter, startWith, switchMap } from 'rxjs';
import { SicatMapa, agruparPorPunto, escapar, iconoGrupo, popupDeGrupo } from 'shared';

import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { Catalogos } from '../core/catalogos';
import { ESTADOS_PUBLICACION } from '../core/modelos';

const COLOR_ESTADO: Record<string, string> = { PUBLICADO: '#2e7d32', BORRADOR: '#607d8b', OCULTO: '#c62828' };
const TEXTO_ESTADO: Record<string, string> = { PUBLICADO: 'Publicado', BORRADOR: 'Borrador', OCULTO: 'Oculto' };
const TEXTO_GEO: Record<string, string> = {
  AUTOMATICO: 'Ubicación automática: revisar',
  APROXIMADO: 'Ubicación aproximada: revisar',
  VERIFICADO: 'Ubicación verificada',
};

/** Mapa interno: todos los establecimientos activos, coloreados por estado, y los circuitos con trazado. */
@Component({
  selector: 'app-mapa-page',
  imports: [ReactiveFormsModule, SicatMapa, MatFormFieldModule, MatInputModule, MatSelectModule, MatSlideToggleModule, MatIconModule],
  template: `
    <section class="pagina">
      <div class="encabezado"><h1>Mapa</h1></div>
      <form class="filtros" [formGroup]="filtros">
        <mat-form-field class="busqueda">
          <mat-label>Buscar por nombre</mat-label>
          <mat-icon matPrefix>search</mat-icon>
          <input matInput formControlName="q" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Estado</mat-label>
          <mat-select formControlName="estado">
            <mat-option value="">Todos</mat-option>
            @for (e of estados; track e) { <mat-option [value]="e">{{ textoEstado[e] }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Categoría</mat-label>
          <mat-select formControlName="categoria">
            <mat-option value="">Todas</mat-option>
            @for (c of catalogos.categorias(); track c.id) { <mat-option [value]="'' + c.id">{{ c.nombre }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-slide-toggle formControlName="circuitos">Circuitos</mat-slide-toggle>
      </form>
      <div class="leyenda suave">
        <span>{{ cantidad() }} en el mapa</span>
        @for (e of estados; track e) { <span><i [style.background]="colores[e]"></i> {{ textoEstado[e] }}</span> }
        <span><i class="aro"></i> Ubicación sin verificar</span>
        <span><i class="grupo">2</i> Varios en el mismo lugar</span>
      </div>
      <sicat-mapa class="tarjeta mapa" (listo)="mapa.set($event)" />
    </section>
  `,
  styles: `
    .pagina { max-width: none; height: calc(100vh - 64px); box-sizing: border-box; }
    .mapa { flex: 1; min-height: 420px; overflow: hidden; }
    .leyenda { display: flex; gap: 16px; flex-wrap: wrap; font-size: 13px; margin-top: -8px; }
    .leyenda i { display: inline-block; width: 12px; height: 12px; border-radius: 50%; vertical-align: -1px; }
    .leyenda i.aro { background: #fff; border: 3px solid #f9a825; width: 8px; height: 8px; }
    .leyenda i.grupo { background: #37474f; color: #fff; width: 16px; height: 16px; font: 600 10px/16px sans-serif; text-align: center; font-style: normal; vertical-align: -3px; }
  `,
})
export class MapaPage {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly router = inject(Router);
  private readonly zona = inject(NgZone);
  protected readonly catalogos = inject(Catalogos);

  protected readonly estados = ESTADOS_PUBLICACION;
  protected readonly colores = COLOR_ESTADO;
  protected readonly textoEstado = TEXTO_ESTADO;
  protected readonly mapa = signal<L.Map | null>(null);
  protected readonly cantidad = signal(0);
  private readonly puntos = L.featureGroup();
  private readonly lineas = L.featureGroup();

  protected readonly filtros = inject(NonNullableFormBuilder).group({ q: '', estado: '', categoria: '', circuitos: true });

  constructor() {
    const destroy = inject(DestroyRef);
    this.catalogos.arbolCategorias().subscribe();
    const mapaListo = toObservable(this.mapa).pipe(filter((m): m is L.Map => m !== null));

    mapaListo.pipe(takeUntilDestroyed(destroy)).subscribe((m) => this.preparar(m));

    combineLatest([mapaListo, this.filtros.valueChanges.pipe(debounceTime(300), startWith(this.filtros.getRawValue()))])
      .pipe(
        switchMap(([, f]) =>
          this.api
            .get<GeoJSON.FeatureCollection<GeoJSON.Point>>('/mapa/establecimientos', { q: f.q, estado: f.estado, categoria: f.categoria })
            .pipe(
              catchError((e) => {
                this.avisos.error(e);
                return EMPTY;
              }),
            ),
        ),
        takeUntilDestroyed(destroy),
      )
      .subscribe((geo) => this.dibujar(geo));

    this.filtros.controls.circuitos.valueChanges.pipe(takeUntilDestroyed(destroy)).subscribe((ver) => {
      const m = this.mapa();
      if (m) {
        if (ver) {
          this.lineas.addTo(m);
        } else {
          this.lineas.remove();
        }
      }
    });
  }

  private preparar(m: L.Map): void {
    this.puntos.addTo(m);
    this.lineas.addTo(m);
    m.on('popupopen', (e: L.PopupEvent) =>
      e.popup.getElement()?.querySelectorAll<HTMLAnchorElement>('a[data-ruta]').forEach((a) =>
        a.addEventListener('click', (ev) => {
          ev.preventDefault();
          this.zona.run(() => this.router.navigateByUrl(a.dataset['ruta']!));
        }),
      ),
    );
    this.api.get<GeoJSON.FeatureCollection<GeoJSON.LineString>>('/mapa/circuitos').subscribe({
      next: (geo) =>
        L.geoJSON(geo, {
          style: { color: '#7b1fa2', weight: 4, opacity: 0.8 },
          onEachFeature: (f, capa) => {
            const p = f.properties ?? {};
            capa.bindPopup(
              `<strong>Circuito de ${escapar(p['establecimiento'])}</strong>` +
                (p['duracionMin'] ? `<div>${escapar(p['duracionMin'])} min</div>` : '') +
                `<div style="margin-top:6px"><a href="/ofertas/${p['id']}" data-ruta="/ofertas/${p['id']}">Editar circuito</a></div>`,
            );
          },
        }).addTo(this.lineas),
      error: (e) => this.avisos.error(e),
    });
  }

  private dibujar(geo: GeoJSON.FeatureCollection<GeoJSON.Point>): void {
    this.puntos.clearLayers();
    const bloque = (p: Record<string, string>) =>
      `<strong>${escapar(p['nombre'])}</strong>` +
      `<div>${TEXTO_ESTADO[p['estadoPublicacion']] ?? ''}</div>` +
      (p['estadoGeo'] ? `<div style="color:#5a626e">${escapar(TEXTO_GEO[p['estadoGeo']] ?? p['estadoGeo'])}</div>` : '') +
      `<div style="margin-top:6px"><a href="/establecimientos/${escapar(p['id'])}" data-ruta="/establecimientos/${escapar(p['id'])}">Abrir ficha</a></div>`;
    // Varios establecimientos en el mismo punto: un solo marcador con la cantidad y un popup con todos.
    for (const g of agruparPorPunto<Record<string, string>>(geo)) {
      const sinVerificar = g.items.some((p) => p['estadoGeo'] && p['estadoGeo'] !== 'VERIFICADO');
      const estados = new Set(g.items.map((p) => p['estadoPublicacion']));
      // Con estados distintos el grupo va en gris oscuro; el popup detalla el de cada uno.
      const color = estados.size === 1 ? (COLOR_ESTADO[g.items[0]['estadoPublicacion']] ?? '#607d8b') : '#37474f';
      const capa =
        g.items.length === 1
          ? L.circleMarker(g.latlng, {
              radius: 7,
              color: sinVerificar ? '#f9a825' : '#fff',
              weight: sinVerificar ? 3 : 2,
              fillColor: color,
              fillOpacity: 0.95,
            })
          : L.marker(g.latlng, { icon: iconoGrupo(g.items.length, color, sinVerificar ? '#f9a825' : '#fff') });
      capa.bindPopup(g.items.length === 1 ? bloque(g.items[0]) : popupDeGrupo(g.items.map(bloque))).addTo(this.puntos);
    }
    this.cantidad.set(geo.features.length);
    // Sin filtros queda centrado en la ciudad (hay puntos en localidades vecinas); con filtros, encuadra los resultados.
    const f = this.filtros.getRawValue();
    if ((f.q || f.estado || f.categoria) && geo.features.length) {
      this.mapa()!.fitBounds(this.puntos.getBounds(), { padding: [30, 30], maxZoom: 16 });
    }
  }
}
