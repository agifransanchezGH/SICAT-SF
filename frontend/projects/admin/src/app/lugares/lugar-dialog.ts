import { Component, NgZone, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { Catalogos } from '../core/catalogos';
import { ESTADOS_GEO, Lugar } from '../core/modelos';
import { SelectorRemoto } from '../core/selector-remoto';
import { POSICION_EN_COLA } from './cola';

/** Lo que devuelve el diálogo al cerrarse guardando. */
export type LugarGuardado = Lugar & { pasarAlSiguiente?: boolean; saltado?: boolean };
import { RouterLink } from '@angular/router';
import * as L from 'leaflet';
import { SANTA_FE, SicatMapa, iconoPin } from 'shared';

/** 6 decimales: unos 10 cm, de sobra para ubicar una puerta. */
const redondear = (n: number) => Math.round(n * 1e6) / 1e6;

/**
 * Alta y edición de un lugar. Las coordenadas se cargan a mano por ahora (el mapa llega en el paso 10):
 * al guardar coordenadas cargadas o corregidas por una persona, la ubicación queda VERIFICADO.
 */
@Component({
  selector: 'app-lugar-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, SelectorRemoto, SicatMapa, RouterLink],
  host: {
    '(keydown.control.enter)': 'atajo($event)',
    '(keydown.meta.enter)': 'atajo($event)',
  },
  template: `
    <h2 mat-dialog-title>
      {{ lugar ? 'Editar lugar' : 'Nuevo lugar' }}
      @if (cola) { <span class="posicion">{{ cola.posicion }} de {{ cola.total }}</span> }
    </h2>
    <mat-dialog-content>
      <form class="grilla" [formGroup]="form">
        <mat-form-field class="ancho-completo">
          <mat-label>Nombre</mat-label>
          <input matInput formControlName="nombre" />
          <mat-hint>Ej.: "Costanera Este" o el nombre del establecimiento</mat-hint>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Tipo de lugar</mat-label>
          <mat-select formControlName="tipoLugarId">
            <mat-option [value]="null">—</mat-option>
            @for (t of tipos(); track t.id) { <mat-option [value]="t.id">{{ t.nombre }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Localidad</mat-label>
          <mat-select formControlName="localidadId">
            <mat-option [value]="null">—</mat-option>
            @for (l of localidades(); track l.id) { <mat-option [value]="l.id">{{ l.nombre }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <app-selector-remoto class="ancho-completo" formControlName="padreId" ruta="/lugares" etiqueta="Dentro de (opcional)"
                             [textoInicial]="lugar?.padre" ayuda="Lugar que lo contiene, p. ej. un parque" />
        <mat-form-field>
          <mat-label>Calle</mat-label>
          <input matInput formControlName="calle" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Altura</mat-label>
          <input matInput type="number" formControlName="altura" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Km (rutas)</mat-label>
          <input matInput type="number" step="0.1" formControlName="km" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Esquina / entre calles</mat-label>
          <input matInput formControlName="calleCruce" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Complemento</mat-label>
          <input matInput formControlName="complemento" />
          <mat-hint>Piso, local, etc.</mat-hint>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Latitud</mat-label>
          <input matInput type="number" step="0.000001" formControlName="lat" placeholder="-31.63" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Longitud</mat-label>
          <input matInput type="number" step="0.000001" formControlName="lng" placeholder="-60.70" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Estado de la ubicación</mat-label>
          <mat-select formControlName="estadoGeo">
            <mat-option [value]="null">—</mat-option>
            @for (e of estadosGeo; track e.valor) { <mat-option [value]="e.valor">{{ e.texto }}</mat-option> }
          </mat-select>
          <mat-hint>Para publicar tiene que estar "Verificado"</mat-hint>
        </mat-form-field>
        @if (lugar?.establecimientos?.length) {
          <div class="ancho-completo relacionados">
            <strong>Establecimientos en este lugar:</strong>
            @for (e of lugar!.establecimientos!; track e.id) {
              <a [routerLink]="['/establecimientos', e.id]" mat-dialog-close>{{ e.nombre }}</a>{{ e.activo ? '' : ' (de baja)' }}{{ $last ? '' : ' · ' }}
            }
          </div>
        }
        <div class="ancho-completo">
          <p class="suave ayuda-mapa">Tocá el mapa o arrastrá el pin para marcar la ubicación exacta.</p>
          <sicat-mapa class="mapa" [centro]="centroInicial" [zoom]="lugar?.coordenadas ? 17 : 13" (listo)="alListo($event)" />
        </div>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      @if (cola) {
        <!-- En una cola: guarda como verificado (con el pin donde quedó) y abre el siguiente -->
        <button mat-button (click)="saltar()" [disabled]="guardando()">Saltar</button>
        <button mat-stroked-button (click)="guardar()" [disabled]="form.invalid || guardando()">Guardar</button>
        <button mat-flat-button aria-keyshortcuts="Control+Enter" (click)="confirmarYSiguiente()"
                [disabled]="!puedeConfirmar() || guardando()">
          Confirmar y siguiente <kbd aria-hidden="true">Ctrl+Enter</kbd>
        </button>
      } @else {
        @if (lugar && lugar.coordenadas && lugar.estadoGeo !== 'VERIFICADO') {
          <button mat-stroked-button (click)="verificar()" [disabled]="guardando()">Confirmar ubicación</button>
        }
        <button mat-flat-button (click)="guardar()" [disabled]="form.invalid || guardando()">Guardar</button>
      }
    </mat-dialog-actions>
  `,
  styles: `
    .grilla { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 8px 12px; padding-top: 4px; }
    .ancho-completo { grid-column: 1 / -1; }
    .ayuda-mapa { margin: 4px 0 6px; font-size: 13px; }
    .relacionados { font-size: 14px; padding: 4px 0; }
    .mapa { height: 280px; border-radius: 8px; overflow: hidden; border: 1px solid var(--sicat-borde); }
    .posicion { margin-left: 8px; font-size: 14px; font-weight: 500; color: var(--sicat-texto-suave); font-variant-numeric: tabular-nums; }
    kbd { margin-left: 6px; padding: 1px 5px; border-radius: 4px; background: rgb(255 255 255 / 18%); font-family: inherit; font-size: 11px; font-weight: 600; line-height: 1.4; }
  `,
})
export class LugarDialog {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly ref = inject(MatDialogRef<LugarDialog, LugarGuardado>);
  protected readonly cola = inject(POSICION_EN_COLA, { optional: true });
  private readonly catalogos = inject(Catalogos);
  private readonly zona = inject(NgZone);
  protected readonly lugar = inject<Lugar | null>(MAT_DIALOG_DATA);

  protected readonly tipos = toSignal(this.catalogos.de('tipo-lugar'), { initialValue: [] });
  protected readonly localidades = toSignal(this.catalogos.de('localidad'), { initialValue: [] });
  protected readonly estadosGeo = ESTADOS_GEO;
  protected readonly guardando = signal(false);

  private readonly l = this.lugar;
  protected readonly form = inject(NonNullableFormBuilder).group({
    nombre: [this.l?.nombre ?? '', [Validators.required, Validators.maxLength(300)]],
    tipoLugarId: [this.l?.tipoLugarId ?? (null as number | null)],
    localidadId: [this.l?.localidadId ?? (null as number | null)],
    padreId: [this.l?.padreId ?? (null as number | null)],
    calle: [this.l?.calle ?? ''],
    altura: [this.l?.altura ?? (null as number | null), Validators.min(0)],
    km: [this.l?.km ?? (null as number | null), Validators.min(0)],
    calleCruce: [this.l?.calleCruce ?? ''],
    complemento: [this.l?.complemento ?? ''],
    lat: [this.l?.coordenadas?.lat ?? (null as number | null), [Validators.min(-90), Validators.max(90)]],
    lng: [this.l?.coordenadas?.lng ?? (null as number | null), [Validators.min(-180), Validators.max(180)]],
    estadoGeo: [(this.l?.estadoGeo ?? null) as string | null],
  });

  protected readonly centroInicial: L.LatLngTuple = this.l?.coordenadas
    ? [this.l.coordenadas.lat, this.l.coordenadas.lng]
    : SANTA_FE;
  private pin?: L.Marker;

  constructor() {
    // Coordenadas escritas o marcadas por una persona: se dan por verificadas.
    const marcar = () => {
      this.form.controls.estadoGeo.setValue('VERIFICADO');
      this.moverPin();
    };
    this.form.controls.lat.valueChanges.subscribe(marcar);
    this.form.controls.lng.valueChanges.subscribe(marcar);
    this.form.valueChanges.subscribe(() => {
      const { lat, lng } = this.form.getRawValue();
      this.coordenadas.set(lat != null && lng != null ? { lat, lng } : null);
      this.invalido.set(this.form.invalid);
    });
  }

  protected alListo(m: L.Map): void {
    this.pin = L.marker(this.centroInicial, { icon: iconoPin(), draggable: true, autoPan: true });
    if (this.l?.coordenadas) {
      this.pin.addTo(m);
    }
    const fijar = (p: L.LatLng) =>
      this.zona.run(() => this.form.patchValue({ lat: redondear(p.lat), lng: redondear(p.lng) }));
    this.pin.on('dragend', () => fijar(this.pin!.getLatLng()));
    m.on('click', (e: L.LeafletMouseEvent) => {
      this.pin!.setLatLng(e.latlng).addTo(m);
      fijar(e.latlng);
    });
    this.mapa = m;
  }

  private mapa?: L.Map;

  /** Cuando se escriben las coordenadas a mano, el pin las sigue. */
  private moverPin(): void {
    const { lat, lng } = this.form.getRawValue();
    if (this.pin && this.mapa && lat != null && lng != null && Math.abs(lat) <= 90 && Math.abs(lng) <= 180) {
      const actual = this.pin.getLatLng();
      if (actual.lat !== lat || actual.lng !== lng || !this.mapa.hasLayer(this.pin)) {
        this.pin.setLatLng([lat, lng]).addTo(this.mapa);
        this.mapa.panTo([lat, lng]);
      }
    }
  }

  protected verificar(): void {
    this.form.controls.estadoGeo.setValue('VERIFICADO');
    this.guardar();
  }

  private readonly coordenadas = signal(this.l?.coordenadas ?? null);
  /** Hay coordenadas en el formulario (las originales o un pin recién marcado). */
  protected readonly puedeConfirmar = computed(() => this.coordenadas() != null && !this.invalido());
  private readonly invalido = signal(this.form.invalid);

  protected confirmarYSiguiente(): void {
    if (!this.puedeConfirmar() || this.guardando()) {
      return;
    }
    this.form.controls.estadoGeo.setValue('VERIFICADO');
    this.guardar(true);
  }

  /** Pasa al siguiente de la cola sin guardar nada (p. ej. una dirección que hay que averiguar). */
  protected saltar(): void {
    this.ref.close({ ...this.l!, pasarAlSiguiente: true, saltado: true });
  }

  /** Ctrl+Enter (Cmd+Enter en Mac): en una cola, confirma y pasa al siguiente. */
  protected atajo(evento: Event): void {
    if (this.cola) {
      evento.preventDefault();
      this.confirmarYSiguiente();
    }
  }

  protected guardar(pasarAlSiguiente = false): void {
    const f = this.form.getRawValue();
    const coordenadas = f.lat != null && f.lng != null ? { lat: f.lat, lng: f.lng } : null;
    const cuerpo = {
      nombre: f.nombre.trim(),
      tipoLugarId: f.tipoLugarId,
      localidadId: f.localidadId,
      padreId: f.padreId,
      calle: f.calle || null,
      altura: f.altura,
      km: f.km,
      calleCruce: f.calleCruce || null,
      complemento: f.complemento || null,
      coordenadas,
      geometria: this.l?.geometria ?? null,
      estadoGeo: coordenadas ? f.estadoGeo : f.estadoGeo === 'VERIFICADO' ? null : f.estadoGeo,
    };
    this.guardando.set(true);
    const pedido = this.l ? this.api.put<Lugar>(`/lugares/${this.l.id}`, cuerpo) : this.api.post<Lugar>('/lugares', cuerpo);
    pedido.subscribe({
      next: (r) => {
        if (!pasarAlSiguiente) {
          this.avisos.ok('Lugar guardado'); // al pasar al siguiente avisa la pantalla, con el nombre del próximo
        }
        this.ref.close({ ...r, pasarAlSiguiente });
      },
      error: (e) => {
        this.guardando.set(false);
        this.avisos.error(e);
      },
    });
  }
}
