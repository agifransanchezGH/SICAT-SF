import { Component, NgZone, OnInit, inject, input, signal, viewChild } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { Circuito, Parada } from '../core/modelos';
import { OpcionRemota, SelectorRemoto } from '../core/selector-remoto';
import * as L from 'leaflet';
import { SicatMapa } from 'shared';

const round = (n: number) => Math.round(n * 1e6) / 1e6;

/** Circuito turístico: punto de partida, duración y paradas (establecimientos en orden). El recorrido se dibuja tocando el mapa. */
@Component({
  selector: 'app-circuito-form',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, SelectorRemoto, SicatMapa],
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
          <span class="suave">{{ vertices().length }} puntos{{ editable() ? ' · tocá el mapa para agregar puntos en orden' : '' }}</span>
          <span class="espacio"></span>
          @if (editable()) {
            <button mat-button type="button" [disabled]="!vertices().length" (click)="deshacer()"><mat-icon>undo</mat-icon> Deshacer</button>
            <button mat-button type="button" [disabled]="!vertices().length" (click)="borrarTrazado()"><mat-icon>delete</mat-icon> Borrar</button>
          }
        </div>
        <sicat-mapa class="mapa" (listo)="alListo($event)" />
      </div>
      @if (editable()) {
        <div class="ancho-completo botones">
          <button mat-flat-button type="submit" [disabled]="form.invalid || form.pristine || guardando()">Guardar circuito</button>
        </div>
      }
    </form>

    <div class="paradas">
      <h3>Paradas</h3>
      <ol>
        @for (p of paradas(); track p.establecimientoId; let i = $index) {
          <li>
            <span class="nombre">{{ p.establecimiento }}</span>
            @if (editable()) {
              <button mat-icon-button aria-label="Subir" [disabled]="i === 0" (click)="mover(i, -1)"><mat-icon>arrow_upward</mat-icon></button>
              <button mat-icon-button aria-label="Bajar" [disabled]="i === paradas().length - 1" (click)="mover(i, 1)"><mat-icon>arrow_downward</mat-icon></button>
              <button mat-icon-button aria-label="Quitar" (click)="quitar(i)"><mat-icon>delete</mat-icon></button>
            }
          </li>
        } @empty {
          <p class="vacio">Sin paradas.</p>
        }
      </ol>
      @if (editable()) {
        <div class="nueva">
          <app-selector-remoto #nueva ruta="/establecimientos" etiqueta="Agregar parada (establecimiento)" (seleccion)="agregar($event)" />
          <span class="espacio"></span>
          <button mat-flat-button [disabled]="!paradasCambiadas() || guardando()" (click)="guardarParadas()">Guardar paradas</button>
        </div>
      }
    </div>
  `,
  styles: `
    .botones { display: flex; justify-content: flex-end; }
    .cabecera-mapa { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 6px; }
    .mapa { height: 340px; border-radius: 8px; overflow: hidden; border: 1px solid var(--sicat-borde); }
    .paradas { padding: 0 16px 16px; }
    h3 { font: var(--mat-sys-title-medium); margin: 8px 0; }
    ol { margin: 0; padding-left: 24px; }
    li { padding: 2px 0; }
    li .nombre { display: inline-block; min-width: 240px; }
    .nueva { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; margin-top: 8px; }
    .nueva app-selector-remoto { flex: 1 1 320px; max-width: 520px; }
    .espacio { flex: 1; }
  `,
})
export class CircuitoForm implements OnInit {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
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
  private mapa?: L.Map;

  protected alListo(m: L.Map): void {
    this.mapa = m;
    const t = this.circuito()?.trazado as GeoJSON.LineString | null | undefined;
    this.vertices.set((t?.coordinates ?? []).map(([lng, lat]) => [lat, lng] as L.LatLngTuple));
    this.linea = L.polyline([], { color: '#7b1fa2', weight: 4 }).addTo(m);
    this.puntosLinea.addTo(m);
    this.redibujar();
    if (this.vertices().length > 1) {
      m.fitBounds(this.linea.getBounds(), { padding: [30, 30] });
    }
    if (this.editable()) {
      m.on('click', (e: L.LeafletMouseEvent) =>
        this.zona.run(() => this.cambiarVertices([...this.vertices(), [round(e.latlng.lat), round(e.latlng.lng)]])),
      );
    }
  }

  protected deshacer(): void {
    this.cambiarVertices(this.vertices().slice(0, -1));
  }

  protected borrarTrazado(): void {
    this.cambiarVertices([]);
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
        color: '#7b1fa2',
        fillColor: i === 0 ? '#7b1fa2' : '#fff',
        fillOpacity: 1,
        weight: 2,
      })
        .bindTooltip(i === 0 ? 'Inicio' : `Punto ${i + 1}`)
        .addTo(this.puntosLinea),
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
    this.guardando.set(true);
    const v = this.vertices();
    if (v.length === 1) {
      this.avisos.ok('El recorrido necesita al menos dos puntos');
      return;
    }
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

  protected agregar(o: OpcionRemota): void {
    if (!this.paradas().some((p) => p.establecimientoId === o.id)) {
      this.paradas.update((l) => [...l, { orden: l.length + 1, establecimientoId: o.id, establecimiento: o.nombre }]);
      this.paradasCambiadas.set(true);
    }
    setTimeout(() => this.nueva()?.limpiar());
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
