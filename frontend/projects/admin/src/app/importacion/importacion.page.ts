import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { interval, startWith, switchMap, takeWhile } from 'rxjs';

import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { Confirmar } from '../core/confirmar';
import { EstadoGeocodificacion, PerfilImportacion, ResultadoImportacion } from '../core/modelos';

type Item = ResultadoImportacion['items'][number];

const ESTADOS_ITEM: Record<Item['estado'], string> = {
  NUEVO: 'Nuevo',
  DUPLICADO_EN_BASE: 'Ya existe',
  DUPLICADO_EN_ARCHIVO: 'Repetido en la planilla',
  ERROR: 'Con errores',
};

/** Migración desde las planillas Excel (ADR 0010): validar, importar y geocodificar. Solo ADMIN. */
@Component({
  selector: 'app-importacion-page',
  imports: [
    FormsModule, DatePipe, RouterLink, MatSelectModule, MatFormFieldModule, MatInputModule, MatButtonModule,
    MatButtonToggleModule, MatCheckboxModule, MatIconModule, MatProgressBarModule, MatTableModule,
  ],
  template: `
    <section class="pagina">
      <div class="encabezado"><h1>Importación desde planillas</h1></div>

      <div class="tarjeta bloque">
        <h2>1. Planilla</h2>
        <div class="fila">
          <mat-form-field>
            <mat-label>Tipo de planilla</mat-label>
            <mat-select [(ngModel)]="perfil">
              @for (p of perfiles(); track p.archivo) { <mat-option [value]="p.archivo">{{ p.archivo }} · {{ p.archivoEsperado }}</mat-option> }
            </mat-select>
          </mat-form-field>
          <input #archivoInput type="file" accept=".xlsx" hidden (change)="elegirArchivo(archivoInput.files)" />
          <button mat-stroked-button (click)="archivoInput.click()"><mat-icon>attach_file</mat-icon> {{ archivo()?.name ?? 'Elegir archivo .xlsx' }}</button>
          <button mat-flat-button [disabled]="!perfil || !archivo() || enviando()" (click)="validar()">Validar</button>
        </div>
        <p class="suave nota">Validar no guarda nada: muestra qué se importaría. Después confirmás con "Importar".</p>
        @if (enviando()) { <mat-progress-bar mode="indeterminate" /> }
      </div>

      @if (resultado(); as r) {
        <div class="tarjeta bloque">
          <h2>2. {{ r.ejecutado ? 'Resultado de la importación' : 'Vista previa' }}</h2>
          <div class="resumen">
            <span><strong>{{ r.total }}</strong> leídos</span>
            <span><strong>{{ r.nuevos }}</strong> nuevos</span>
            <span><strong>{{ r.duplicadosEnBase }}</strong> ya existen</span>
            <span><strong>{{ r.duplicadosEnArchivo }}</strong> repetidos en la planilla</span>
            <span><strong>{{ r.conErrores }}</strong> con errores</span>
            @if (r.ejecutado) { <span class="ok"><strong>{{ r.creados }}</strong> creados</span> }
          </div>
          @if (r.hojasIgnoradas.length) { <p class="suave">Hojas que no se importan: {{ r.hojasIgnoradas.join(', ') }}</p> }

          <mat-button-toggle-group [(ngModel)]="filtroEstado" aria-label="Filtrar">
            <mat-button-toggle value="">Todos</mat-button-toggle>
            @for (e of estadosItem; track e[0]) { <mat-button-toggle [value]="e[0]">{{ e[1] }}</mat-button-toggle> }
          </mat-button-toggle-group>

          <table mat-table [dataSource]="items()" class="tabla">
            <ng-container matColumnDef="fila">
              <th mat-header-cell *matHeaderCellDef>Hoja / fila</th>
              <td mat-cell *matCellDef="let i">{{ i.hoja }} · {{ i.fila }}</td>
            </ng-container>
            <ng-container matColumnDef="nombre">
              <th mat-header-cell *matHeaderCellDef>Establecimiento</th>
              <td mat-cell *matCellDef="let i">
                @if (i.establecimientoCreadoId || i.establecimientoExistenteId; as id) {
                  <a [routerLink]="['/establecimientos', id]">{{ i.nombre }}</a>
                } @else { {{ i.nombre }} }
                <div class="suave">{{ i.categoria }}{{ i.direccion ? ' · ' + i.direccion : '' }}</div>
              </td>
            </ng-container>
            <ng-container matColumnDef="estado">
              <th mat-header-cell *matHeaderCellDef>Estado</th>
              <td mat-cell *matCellDef="let i">{{ estados[i.estado] }}</td>
            </ng-container>
            <ng-container matColumnDef="detalle">
              <th mat-header-cell *matHeaderCellDef>Observaciones</th>
              <td mat-cell *matCellDef="let i">
                @for (e of i.errores ?? []; track e) { <div class="error">{{ e }}</div> }
                @for (a of i.advertencias ?? []; track a) { <div class="suave">{{ a }}</div> }
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="columnas"></tr>
            <tr mat-row *matRowDef="let i; columns: columnas"></tr>
          </table>

          @if (!r.ejecutado) {
            <div class="acciones">
              <button mat-flat-button [disabled]="!r.nuevos || enviando()" (click)="ejecutar()">
                <mat-icon>file_download_done</mat-icon> Importar {{ r.nuevos }} nuevos
              </button>
            </div>
          }
        </div>
      }

      <div class="tarjeta bloque">
        <h2>3. Geocodificación de direcciones</h2>
        <p class="suave nota">
          Busca las coordenadas de las direcciones en OpenStreetMap (una por segundo). Lo encontrado queda como "automático" y hay
          que confirmarlo en <a routerLink="/lugares">Lugares y ubicaciones</a> antes de publicar.
        </p>
        @if (geo(); as g) {
          @if (g.enCurso) {
            <mat-progress-bar mode="determinate" [value]="g.total ? (100 * g.procesados) / g.total : 0" />
            <p>Procesando {{ g.procesados }} de {{ g.total }}…</p>
          }
          <div class="resumen">
            <span><strong>{{ g.pendientes ?? 0 }}</strong> pendientes en la base</span>
            @if (g.inicio) {
              <span>Última corrida: {{ g.inicio | date: 'dd/MM HH:mm' }}</span>
              <span><strong>{{ g.encontrados }}</strong> encontradas</span>
              <span><strong>{{ g.aproximados }}</strong> aproximadas</span>
              <span><strong>{{ g.noEncontrados }}</strong> no encontradas</span>
            }
          </div>
          @if (g.error) { <p class="error">{{ g.error }}</p> }
          <div class="fila">
            <mat-checkbox [(ngModel)]="reintentar">Reintentar también las no encontradas y aproximadas</mat-checkbox>
            <button mat-flat-button [disabled]="g.enCurso" (click)="geocodificar()"><mat-icon>travel_explore</mat-icon> Geocodificar</button>
          </div>
        }
      </div>
    </section>
  `,
  styles: `
    .bloque { padding: 16px; display: flex; flex-direction: column; gap: 8px; }
    h2 { margin: 0; font: var(--mat-sys-title-medium); }
    .fila { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
    .fila mat-form-field { min-width: 320px; }
    .resumen { display: flex; gap: 20px; flex-wrap: wrap; }
    .nota { margin: 0; font-size: 13px; }
    .ok { color: var(--sicat-ok); }
    .error { color: var(--sicat-error); }
    .acciones { padding: 8px 0 0; }
    .tabla { margin-top: 8px; }
  `,
})
export class ImportacionPage {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly confirmar = inject(Confirmar);
  private readonly destroy = inject(DestroyRef);

  protected readonly perfiles = signal<PerfilImportacion[]>([]);
  protected readonly archivo = signal<File | null>(null);
  protected readonly resultado = signal<ResultadoImportacion | null>(null);
  protected readonly enviando = signal(false);
  protected readonly geo = signal<EstadoGeocodificacion | null>(null);
  protected readonly estados: Record<string, string> = ESTADOS_ITEM;
  protected readonly estadosItem = Object.entries(ESTADOS_ITEM);
  protected readonly columnas = ['fila', 'nombre', 'estado', 'detalle'];
  protected perfil = '';
  protected reintentar = false;
  private readonly filtro = signal('');

  get filtroEstado(): string {
    return this.filtro();
  }
  set filtroEstado(v: string) {
    this.filtro.set(v);
  }

  protected readonly items = computed(() => {
    const f = this.filtro();
    const l = this.resultado()?.items ?? [];
    return f ? l.filter((i) => i.estado === f) : l;
  });

  constructor() {
    this.api.get<PerfilImportacion[]>('/importacion/perfiles').subscribe({
      next: (p) => this.perfiles.set(p),
      error: (e) => this.avisos.error(e),
    });
    this.seguirGeocodificacion();
  }

  protected elegirArchivo(l: FileList | null): void {
    this.archivo.set(l?.[0] ?? null);
    this.resultado.set(null);
  }

  protected validar(): void {
    this.subir('validar');
  }

  protected ejecutar(): void {
    const r = this.resultado()!;
    this.confirmar
      .pedir('Importar', `Se crean ${r.nuevos} establecimientos en borrador. Los duplicados y los que tienen errores no se importan.`, 'Importar')
      .subscribe((ok) => ok && this.subir('ejecutar'));
  }

  private subir(accion: 'validar' | 'ejecutar'): void {
    this.enviando.set(true);
    this.api.subir<ResultadoImportacion>(`/importacion/${this.perfil}/${accion}`, this.archivo()!).subscribe({
      next: (r) => {
        this.enviando.set(false);
        this.resultado.set(r);
        this.filtro.set('');
        if (r.ejecutado) {
          this.avisos.ok(`${r.creados} establecimientos importados`);
          this.seguirGeocodificacion();
        }
      },
      error: (e) => {
        this.enviando.set(false);
        this.avisos.error(e);
      },
    });
  }

  protected geocodificar(): void {
    const estados = this.reintentar ? ['PENDIENTE', 'NO_ENCONTRADO', 'APROXIMADO'] : ['PENDIENTE'];
    this.api.post<EstadoGeocodificacion>('/importacion/geocodificar', {}, { limite: 5000, estados }).subscribe({
      next: (g) => {
        this.geo.set(g);
        this.seguirGeocodificacion();
      },
      error: (e) => this.avisos.error(e),
    });
  }

  /** Consulta el avance cada 3 s mientras haya una corrida en curso. */
  private seguirGeocodificacion(): void {
    interval(3000)
      .pipe(
        startWith(0),
        switchMap(() => this.api.get<EstadoGeocodificacion>('/importacion/geocodificacion')),
        takeWhile((g) => g.enCurso, true),
        takeUntilDestroyed(this.destroy),
      )
      .subscribe({ next: (g) => this.geo.set(g), error: (e) => this.avisos.error(e) });
  }
}
