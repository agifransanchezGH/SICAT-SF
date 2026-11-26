import { Component, NgZone, effect, inject, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import * as L from 'leaflet';
import { SANTA_FE, SicatMapa, iconoPin } from 'shared';

import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { Coordenadas, Lugar } from '../core/modelos';
import { CalidadChip, cuerpoLugar } from './calidad';

/** 6 decimales: unos 10 cm. */
const redondear = (n: number) => Math.round(n * 1e6) / 1e6;

/**
 * Panel derecho de la cola de ubicaciones: el lugar elegido en el mapa, con su señal de calidad. Se confirma tal
 * cual o después de mover el pin (arrastrándolo o tocando el mapa), sin abrir un diálogo. Ctrl+Enter confirma.
 */
@Component({
  selector: 'app-panel-ubicacion',
  imports: [MatButtonModule, MatIconModule, RouterLink, SicatMapa, CalidadChip],
  host: {
    '(keydown.control.enter)': 'atajo($event)',
    '(keydown.meta.enter)': 'atajo($event)',
  },
  template: `
    <section class="panel" aria-labelledby="panel-ubicacion-titulo">
      @if (lugar(); as l) {
        <header class="cabecera">
          <h2 id="panel-ubicacion-titulo">{{ l.nombre }}</h2>
          <app-calidad [calidad]="l.calidad" />
        </header>
        <div class="detalle">
          <p class="direccion">{{ direccion(l) || 'Sin dirección cargada' }}</p>
          @if (l.calidad?.motivos?.length) {
            <ul class="motivos">@for (m of l.calidad!.motivos; track m) { <li>{{ m }}</li> }</ul>
          }
          @if (l.establecimientos?.length) {
            <p class="establecimientos">
              En este lugar:
              @for (e of l.establecimientos!; track e.id) {
                <a [routerLink]="['/establecimientos', e.id]">{{ e.nombre }}</a>{{ $last ? '' : ', ' }}
              }
            </p>
          }
        </div>
      }
      <div class="mapa-caja">
        <sicat-mapa class="mapa" [centro]="centroInicial" [zoom]="13" (listo)="alListo($event)" />
        @if (lugar() && !pin()) {
          <p class="aviso-mapa" role="status">Sin coordenadas: tocá el mapa donde está el lugar.</p>
        }
      </div>
      @if (lugar(); as l) {
        <p class="estado-pin" aria-live="polite">
          @if (movido()) { Pin movido: {{ pin()!.lat }}, {{ pin()!.lng }}. }
          @else if (pin()) { Pin en la ubicación guardada. }
        </p>
        @if (editable()) {
          <div class="acciones">
            <button mat-flat-button type="button" aria-keyshortcuts="Control+Enter" [disabled]="!pin() || guardando()" (click)="confirmar()">
              <mat-icon>check</mat-icon> {{ movido() ? 'Guardar aquí y confirmar' : 'Confirmar ubicación' }}
              <kbd aria-hidden="true">Ctrl+Enter</kbd>
            </button>
            @if (movido()) {
              <button mat-stroked-button type="button" (click)="deshacer()"><mat-icon>undo</mat-icon> Volver al punto guardado</button>
            }
            <button mat-button type="button" (click)="saltar.emit()">Saltar</button>
            <button mat-button type="button" (click)="editar.emit(l)"><mat-icon>edit</mat-icon> Editar datos</button>
            @if (eliminable() && !l.establecimientos?.length) {
              <button mat-button type="button" class="eliminar" (click)="eliminar.emit(l)"><mat-icon>delete</mat-icon> Eliminar lugar</button>
            }
          </div>
        }
      } @else {
        <p class="vacio">Elegí una ubicación de la lista para verla en el mapa.</p>
      }
    </section>
  `,
  styles: `
    :host { display: block; }
    /* El detalle se muestra entero (la tarjeta crece); el mapa se ajusta al alto de la ventana para dejar los botones a la vista. */
    .panel { display: flex; flex-direction: column; gap: 8px; }
    .cabecera { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
    .detalle { display: flex; flex-direction: column; gap: 8px; }
    h2 { margin: 0; font-size: 18px; font-stretch: var(--sicat-ancho-nombre); }
    .direccion { margin: 0; color: var(--sicat-tinta); }
    .motivos { margin: 0; padding-left: 18px; color: var(--sicat-texto-suave); font-size: 13.5px; }
    .establecimientos { margin: 0; font-size: 14px; }
    /* Alto de la ventana menos barra superior, cabecera, detalle típico y botones (~360px); entre 220 y 520px. */
    .mapa-caja { position: relative; }
    .mapa { display: block; height: clamp(220px, calc(100dvh - 360px), 520px); border-radius: 12px; overflow: hidden; border: 1px solid var(--sicat-borde); }
    .aviso-mapa {
      position: absolute; left: 12px; right: 12px; bottom: 12px; margin: 0; padding: 8px 12px; z-index: 500;
      border-radius: 8px; background: var(--mat-sys-surface); border: 1px solid var(--sicat-borde); font-size: 14px;
    }
    .estado-pin { margin: 0; min-height: 20px; font-size: 13px; color: var(--sicat-texto-suave); font-variant-numeric: tabular-nums; }
    .acciones { display: flex; flex-wrap: wrap; gap: 8px; }
    .acciones button { min-height: 44px; }
    /* Separada de las acciones de revisión: es la única que no se deshace. */
    .acciones .eliminar { margin-left: auto; color: var(--sicat-error); }
    kbd { margin-left: 6px; padding: 1px 5px; border-radius: 4px; background: rgb(255 255 255 / 18%); font-family: inherit; font-size: 11px; font-weight: 600; }
  `,
})
export class PanelUbicacion {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly zona = inject(NgZone);

  readonly lugar = input<Lugar | null>(null);
  readonly editable = input(true);
  /** Muestra "Eliminar lugar" (solo administradores; y solo si ningún establecimiento usa el lugar). */
  readonly eliminable = input(false);
  readonly eliminar = output<Lugar>();
  /** Se guardó como VERIFICADO (con el pin donde quedó). */
  readonly confirmado = output<Lugar>();
  readonly saltar = output<void>();
  readonly editar = output<Lugar>();

  protected readonly centroInicial = SANTA_FE;
  /** Posición actual del pin (null: el lugar no tiene coordenadas y todavía no se marcó). */
  protected readonly pin = signal<Coordenadas | null>(null);
  protected readonly movido = signal(false);
  protected readonly guardando = signal(false);

  private mapa?: L.Map;
  private marcador?: L.Marker;

  constructor() {
    effect(() => {
      const l = this.lugar();
      this.mostrar(l);
    });
  }

  protected alListo(m: L.Map): void {
    this.mapa = m;
    this.marcador = L.marker(SANTA_FE, { icon: iconoPin(), draggable: true, autoPan: true, keyboard: true, title: 'Ubicación' });
    this.marcador.on('dragend', () => this.fijar(this.marcador!.getLatLng()));
    m.on('click', (e: L.LeafletMouseEvent) => this.fijar(e.latlng));
    this.mostrar(this.lugar());
  }

  private fijar(p: L.LatLng): void {
    this.zona.run(() => {
      if (!this.lugar() || !this.editable()) {
        return;
      }
      this.marcador!.setLatLng(p).addTo(this.mapa!);
      this.pin.set({ lat: redondear(p.lat), lng: redondear(p.lng) });
      this.movido.set(true);
    });
  }

  private mostrar(l: Lugar | null): void {
    this.movido.set(false);
    this.pin.set(l?.coordenadas ?? null);
    if (!this.mapa || !this.marcador) {
      return;
    }
    if (l?.coordenadas) {
      const p: L.LatLngTuple = [l.coordenadas.lat, l.coordenadas.lng];
      this.marcador.setLatLng(p).addTo(this.mapa);
      this.mapa.setView(p, 17);
    } else {
      this.marcador.remove();
      this.mapa.setView(SANTA_FE, 13);
    }
  }

  protected deshacer(): void {
    this.mostrar(this.lugar());
  }

  protected atajo(evento: Event): void {
    evento.preventDefault();
    this.confirmar();
  }

  protected confirmar(): void {
    const l = this.lugar();
    const p = this.pin();
    if (!l || !p || this.guardando() || !this.editable()) {
      return;
    }
    this.guardando.set(true);
    this.api.put<Lugar>(`/lugares/${l.id}`, cuerpoLugar(l, p, 'VERIFICADO')).subscribe({
      next: (r) => {
        this.guardando.set(false);
        this.confirmado.emit(r);
      },
      error: (e) => {
        this.guardando.set(false);
        this.avisos.error(e);
      },
    });
  }

  protected direccion(l: Lugar): string {
    const calle = [l.calle, l.altura].filter((x) => x != null && x !== '').join(' ');
    const km = l.km != null ? `km ${l.km}` : '';
    return [calle, km, l.calleCruce && `esq. ${l.calleCruce}`, l.complemento, l.localidad].filter(Boolean).join(', ');
  }
}
