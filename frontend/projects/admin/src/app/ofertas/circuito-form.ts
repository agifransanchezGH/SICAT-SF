import { Component, NgZone, OnInit, effect, inject, input, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';

import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { Confirmar } from '../core/confirmar';
import { Circuito, Establecimiento, Parada } from '../core/modelos';
import { OpcionRemota, SelectorRemoto } from '../core/selector-remoto';
import * as L from 'leaflet';
import { COLOR_CIRCUITO, SicatMapa, escapar, iconoParada } from 'shared';
import { avisarSiHayCambios } from '../core/cambios-pendientes';

const round = (n: number) => Math.round(n * 1e6) / 1e6;

/**
 * Circuito turístico: punto de partida, duración y paradas (establecimientos en orden). Las paradas se marcan
 * solas en el mapa con su número; el recorrido se dibuja tocando el mapa o uniendo las paradas en orden.
 */
@Component({
  selector: 'app-circuito-form',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, MatTooltipModule, SelectorRemoto, SicatMapa],
  template: `
    <form class="formulario" [formGroup]="form" (ngSubmit)="guardar()">
      <app-selector-remoto formControlName="lugarPartidaId" ruta="/lugares" etiqueta="Lugar de partida" [textoInicial]="circuito()?.lugarPartida" />
      <mat-form-field>
        <mat-label>Duración (minutos)</mat-label>
        <input matInput type="number" min="1" formControlName="duracionMin" />
      </mat-form-field>
      <div class="ancho-completo">
        <div class="cabecera-mapa">
          <strong>Recorrido</strong>
          <span class="suave">{{ vertices().length }} {{ vertices().length === 1 ? 'punto' : 'puntos' }}</span>
          <span class="espacio"></span>
          @if (editable()) {
            <button mat-button type="button" [disabled]="paradasConUbicacion().length < 2" (click)="trazarPorParadas()"
                    matTooltip="Arma el recorrido uniendo las paradas en el orden de la lista">
              <mat-icon>timeline</mat-icon> Unir paradas
            </button>
            <button mat-button type="button" [disabled]="!vertices().length" (click)="deshacer()"><mat-icon>undo</mat-icon> Deshacer</button>
            <button mat-button type="button" [disabled]="!vertices().length" (click)="borrarTrazado()"><mat-icon>delete</mat-icon> Borrar</button>
          }
        </div>
        <sicat-mapa class="mapa" (listo)="alListo($event)" />
        <p class="leyenda suave">
          <span class="muestra-parada" aria-hidden="true">1</span> Paradas, con su número de la lista
          <span class="muestra-recorrido" aria-hidden="true"></span> Recorrido
          @if (editable()) { <span class="ayuda">Tocá el mapa o una parada para sumar puntos al recorrido, en orden.</span> }
        </p>
      </div>
      @if (editable()) {
        <div class="ancho-completo botones">
          <button mat-flat-button type="submit" [disabled]="form.invalid || form.pristine || guardando()">Guardar circuito</button>
        </div>
      }
    </form>

    <div class="paradas">
      <h3>Paradas</h3>
      @if (paradas().length) {
        <ol class="lista">
          @for (p of paradas(); track p.establecimientoId; let i = $index) {
            <li>
              <span class="numero" aria-hidden="true">{{ i + 1 }}</span>
              <span class="nombre">
                {{ p.establecimiento }}
                @if (!p.coordenadas) { <span class="sin-ubicacion">Sin ubicación: no aparece en el mapa</span> }
              </span>
              @if (editable()) {
                <span class="acciones">
                  <button mat-icon-button type="button" [attr.aria-label]="'Subir ' + p.establecimiento" [disabled]="i === 0" (click)="mover(i, -1)"><mat-icon>arrow_upward</mat-icon></button>
                  <button mat-icon-button type="button" [attr.aria-label]="'Bajar ' + p.establecimiento" [disabled]="i === paradas().length - 1" (click)="mover(i, 1)"><mat-icon>arrow_downward</mat-icon></button>
                  <button mat-icon-button type="button" [attr.aria-label]="'Quitar ' + p.establecimiento" (click)="quitar(i)"><mat-icon>delete</mat-icon></button>
                </span>
              }
            </li>
          }
        </ol>
      } @else {
        <p class="vacio">Sin paradas. Agregá establecimientos y aparecen numerados en el mapa.</p>
      }
      @if (editable()) {
        <div class="nueva">
          <app-selector-remoto #nueva ruta="/establecimientos" etiqueta="Agregar parada (establecimiento)" (seleccion)="agregar($event)" />
          @if (paradasCambiadas()) { <span class="suave cambios" role="status">Hay cambios sin guardar en las paradas</span> }
          <span class="espacio"></span>
          <button mat-flat-button type="button" [disabled]="!paradasCambiadas() || guardando()" (click)="guardarParadas()">Guardar paradas</button>
        </div>
      }
    </div>
  `,
  styles: `
    .botones { display: flex; justify-content: flex-end; }
    .cabecera-mapa { display: flex; align-items: center; gap: 4px 12px; flex-wrap: wrap; margin-bottom: 6px; }
    .mapa { height: 380px; border-radius: 12px; overflow: hidden; border: 1px solid var(--sicat-borde); }
    .leyenda { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; margin: 6px 0 0; font-size: 13px; }
    .muestra-parada {
      display: inline-flex; align-items: center; justify-content: center; width: 18px; height: 18px; border-radius: 5px;
      background: var(--sicat-ok); color: #fff; font-size: 11px; font-weight: 600;
    }
    .leyenda .ayuda { flex-basis: 100%; }
    .muestra-recorrido { display: inline-block; width: 22px; height: 4px; border-radius: 2px; background: var(--sicat-ok); margin-left: 12px; }
    .paradas { padding: 0 16px 16px; }
    h3 { font: var(--mat-sys-title-medium); margin: 8px 0; }
    .lista { list-style: none; margin: 0; padding: 0; max-width: 760px; border-top: 1px solid var(--sicat-borde); }
    /* Número, nombre y acciones en columnas fijas: los botones quedan alineados en todas las filas. */
    .lista li {
      display: grid; grid-template-columns: 28px minmax(0, 1fr) auto; align-items: center; gap: 12px;
      min-height: 48px; padding: 2px 0; border-bottom: 1px solid var(--sicat-borde);
    }
    .numero {
      display: inline-flex; align-items: center; justify-content: center; width: 24px; height: 24px; border-radius: 6px;
      background: var(--sicat-ok); color: #fff; font-size: 12px; font-weight: 600; font-variant-numeric: tabular-nums;
    }
    .nombre { overflow-wrap: anywhere; }
    .sin-ubicacion { display: block; font-size: 12.5px; color: var(--sicat-alerta); }
    .acciones { display: flex; }
    .nueva { display: flex; gap: 8px 12px; align-items: center; flex-wrap: wrap; margin-top: 12px; }
    .nueva app-selector-remoto { flex: 1 1 320px; max-width: 520px; }
    .cambios { font-size: 13px; }
    .espacio { flex: 1; }
  `,
})
export class CircuitoForm implements OnInit {
  private readonly avisoDeCambios = avisarSiHayCambios('Circuito', () => this.form.dirty || this.paradasCambiadas());
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly confirmar = inject(Confirmar);
  private readonly zona = inject(NgZone);

  readonly ofertaId = input.required<number>();
  readonly circuito = input<Circuito | undefined>();
  readonly editable = input(true);

  private readonly nueva = viewChild<SelectorRemoto>('nueva');
  protected readonly guardando = signal(false);
  protected readonly paradas = signal<Parada[]>([]);
  protected readonly paradasCambiadas = signal(false);

  /** Vértices del recorrido como [lat, lng] (GeoJSON los guarda al revés: [lng, lat]). */
  protected readonly vertices = signal<L.LatLngTuple[]>([]);
  private linea?: L.Polyline;
  private puntosLinea = L.layerGroup();
  private capaParadas = L.layerGroup();
  private readonly mapa = signal<L.Map | null>(null);
  /** El primer encuadre se hace cuando llegan las paradas (si no hay recorrido que mostrar). */
  private encuadrado = false;

  constructor() {
    effect(() => this.dibujarParadas(this.paradas(), this.mapa()));
  }

  protected paradasConUbicacion(): Parada[] {
    return this.paradas().filter((p) => p.coordenadas);
  }

  protected alListo(m: L.Map): void {
    const t = this.circuito()?.trazado as GeoJSON.LineString | null | undefined;
    this.vertices.set((t?.coordinates ?? []).map(([lng, lat]) => [lat, lng] as L.LatLngTuple));
    this.linea = L.polyline([], { color: COLOR_CIRCUITO, weight: 4 }).addTo(m);
    this.puntosLinea.addTo(m);
    this.capaParadas.addTo(m);
    this.redibujar();
    if (this.vertices().length > 1) {
      m.fitBounds(this.linea.getBounds(), { padding: [30, 30] });
      this.encuadrado = true;
    }
    if (this.editable()) {
      m.on('click', (e: L.LeafletMouseEvent) =>
        this.zona.run(() => this.cambiarVertices([...this.vertices(), [round(e.latlng.lat), round(e.latlng.lng)]])),
      );
    }
    this.mapa.set(m);
  }

  private dibujarParadas(paradas: Parada[], m: L.Map | null): void {
    if (!m) {
      return;
    }
    this.capaParadas.clearLayers();
    const puntos: L.LatLngTuple[] = [];
    paradas.forEach((p, i) => {
      if (p.coordenadas) {
        const punto: L.LatLngTuple = [p.coordenadas.lat, p.coordenadas.lng];
        puntos.push(punto);
        const marcador = L.marker(punto, { icon: iconoParada(i + 1), keyboard: false })
          .bindTooltip(`${i + 1}. ${escapar(p.establecimiento)}`)
          .addTo(this.capaParadas);
        // Tocar una parada suma al recorrido su punto exacto (en vez del lugar aproximado donde cayó el clic).
        if (this.editable()) {
          marcador.on('click', () => this.zona.run(() => this.cambiarVertices([...this.vertices(), punto])));
        }
      }
    });
    if (!this.encuadrado && puntos.length) {
      this.encuadrado = true;
      if (puntos.length === 1) {
        m.setView(puntos[0], 16);
      } else {
        m.fitBounds(L.latLngBounds(puntos), { padding: [40, 40] });
      }
    }
  }

  protected deshacer(): void {
    this.cambiarVertices(this.vertices().slice(0, -1));
  }

  protected borrarTrazado(): void {
    this.cambiarVertices([]);
  }

  /** Reemplaza el recorrido por la línea que une las paradas en el orden de la lista. */
  protected trazarPorParadas(): void {
    const puntos = this.paradasConUbicacion().map((p) => [p.coordenadas!.lat, p.coordenadas!.lng] as L.LatLngTuple);
    const aplicar = () => {
      this.cambiarVertices(puntos);
      const m = this.mapa();
      if (m && this.linea && puntos.length > 1) {
        m.fitBounds(this.linea.getBounds(), { padding: [30, 30] });
      }
    };
    if (!this.vertices().length) {
      aplicar();
      return;
    }
    this.confirmar
      .pedir('Unir paradas', 'Reemplaza el recorrido dibujado por la línea que une las paradas en orden. Después podés ajustarlo o deshacerlo antes de guardar.', 'Reemplazar')
      .subscribe((ok) => ok && aplicar());
  }

  private cambiarVertices(v: L.LatLngTuple[]): void {
    this.vertices.set(v);
    this.form.markAsDirty();
    this.redibujar();
  }

  private redibujar(): void {
    const v = this.vertices();
    this.linea?.setLatLngs(v);
    this.puntosLinea.clearLayers();
    v.forEach((p, i) =>
      L.circleMarker(p, {
        radius: i === 0 ? 7 : 4,
        color: COLOR_CIRCUITO,
        fillColor: i === 0 ? COLOR_CIRCUITO : '#fff',
        fillOpacity: 1,
        weight: 2,
        interactive: false,
      }).addTo(this.puntosLinea),
    );
  }

  protected readonly form = inject(NonNullableFormBuilder).group({
    lugarPartidaId: [null as number | null],
    duracionMin: [null as number | null, Validators.min(1)],
  });

  ngOnInit(): void {
    const c = this.circuito();
    if (c) {
      this.form.reset({ lugarPartidaId: c.lugarPartidaId, duracionMin: c.duracionMin });
      this.api.get<Parada[]>(`/ofertas/${this.ofertaId()}/circuito/paradas`).subscribe({
        next: (p) => this.paradas.set(p),
        error: (e) => this.avisos.error(e),
      });
    } else {
      this.form.markAsDirty();
    }
    if (!this.editable()) {
      this.form.disable();
    }
  }

  protected guardar(): void {
    const v = this.vertices();
    if (v.length === 1) {
      this.avisos.ok('El recorrido necesita al menos dos puntos');
      return;
    }
    this.guardando.set(true);
    const trazado = v.length ? { type: 'LineString', coordinates: v.map(([lat, lng]) => [lng, lat]) } : null;
    const cuerpo = { ...this.form.getRawValue(), trazado };
    this.api.put<Circuito>(`/ofertas/${this.ofertaId()}/circuito`, cuerpo).subscribe({
      next: () => {
        this.guardando.set(false);
        this.form.markAsPristine();
        this.avisos.ok('Circuito guardado');
      },
      error: (e) => {
        this.guardando.set(false);
        this.avisos.error(e);
      },
    });
  }

  /** Agrega la parada y busca la ubicación del establecimiento para marcarla en el mapa enseguida. */
  protected agregar(o: OpcionRemota): void {
    setTimeout(() => this.nueva()?.limpiar());
    if (this.paradas().some((p) => p.establecimientoId === o.id)) {
      return;
    }
    this.paradas.update((l) => [...l, { orden: l.length + 1, establecimientoId: o.id, establecimiento: o.nombre, coordenadas: null }]);
    this.paradasCambiadas.set(true);
    this.api.get<Establecimiento>(`/establecimientos/${o.id}`).subscribe({
      next: (e) => {
        const c = e.lugar?.coordenadas ?? null;
        if (c) {
          this.paradas.update((l) => l.map((p) => (p.establecimientoId === o.id ? { ...p, coordenadas: c } : p)));
        } else {
          this.avisos.ok(`${o.nombre} no tiene ubicación cargada: se agrega a la lista pero no aparece en el mapa`);
        }
      },
      error: (e) => this.avisos.error(e),
    });
  }

  protected mover(i: number, d: number): void {
    this.paradas.update((l) => {
      const r = [...l];
      [r[i], r[i + d]] = [r[i + d], r[i]];
      return r;
    });
    this.paradasCambiadas.set(true);
  }

  protected quitar(i: number): void {
    this.paradas.update((l) => l.filter((_, j) => j !== i));
    this.paradasCambiadas.set(true);
  }

  protected guardarParadas(): void {
    this.guardando.set(true);
    const cuerpo = this.paradas().map((p) => ({ establecimientoId: p.establecimientoId }));
    this.api.put<Parada[]>(`/ofertas/${this.ofertaId()}/circuito/paradas`, cuerpo).subscribe({
      next: (p) => {
        this.guardando.set(false);
        this.paradas.set(p);
        this.paradasCambiadas.set(false);
        this.avisos.ok('Paradas guardadas');
      },
      error: (e) => {
        this.guardando.set(false);
        this.avisos.error(e);
      },
    });
  }
}
