import { Component, inject, signal } from '@angular/core';
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

/**
 * Alta y edición de un lugar. Las coordenadas se cargan a mano por ahora (el mapa llega en el paso 10):
 * al guardar coordenadas cargadas o corregidas por una persona, la ubicación queda VERIFICADO.
 */
@Component({
  selector: 'app-lugar-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule, SelectorRemoto],
  template: `
    <h2 mat-dialog-title>{{ lugar ? 'Editar lugar' : 'Nuevo lugar' }}</h2>
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
        @if (lugar?.coordenadas) {
          <p class="ancho-completo suave">
            <a [href]="'https://www.openstreetmap.org/?mlat=' + lugar!.coordenadas!.lat + '&mlon=' + lugar!.coordenadas!.lng + '#map=18/' + lugar!.coordenadas!.lat + '/' + lugar!.coordenadas!.lng"
               target="_blank" rel="noopener">Ver la ubicación actual en OpenStreetMap</a>
          </p>
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      @if (lugar && lugar.coordenadas && lugar.estadoGeo !== 'VERIFICADO') {
        <button mat-stroked-button (click)="verificar()" [disabled]="guardando()">Confirmar ubicación</button>
      }
      <button mat-flat-button (click)="guardar()" [disabled]="form.invalid || guardando()">Guardar</button>
    </mat-dialog-actions>
  `,
  styles: `
    .grilla { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 8px 12px; padding-top: 4px; }
    .ancho-completo { grid-column: 1 / -1; }
  `,
})
export class LugarDialog {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly ref = inject(MatDialogRef<LugarDialog, Lugar>);
  private readonly catalogos = inject(Catalogos);
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

  constructor() {
    // Coordenadas escritas por una persona: se dan por verificadas.
    const marcar = () => this.form.controls.estadoGeo.setValue('VERIFICADO');
    this.form.controls.lat.valueChanges.subscribe(marcar);
    this.form.controls.lng.valueChanges.subscribe(marcar);
  }

  protected verificar(): void {
    this.form.controls.estadoGeo.setValue('VERIFICADO');
    this.guardar();
  }

  protected guardar(): void {
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
        this.avisos.ok('Lugar guardado');
        this.ref.close(r);
      },
      error: (e) => {
        this.guardando.set(false);
        this.avisos.error(e);
      },
    });
  }
}
