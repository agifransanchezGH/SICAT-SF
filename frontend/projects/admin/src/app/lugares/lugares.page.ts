import { DecimalPipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { EMPTY, Subject, catchError, debounceTime, finalize, merge, startWith, switchMap } from 'rxjs';

import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { Avisos } from '../core/avisos';
import { Catalogos } from '../core/catalogos';
import { Confirmar } from '../core/confirmar';
import { EstadoChip } from '../core/estado';
import { ESTADOS_GEO, EstadoGeo, Lugar, Pagina } from '../core/modelos';
import { LugarDialog } from './lugar-dialog';

const A_REVISAR: EstadoGeo[] = ['AUTOMATICO', 'APROXIMADO', 'NO_ENCONTRADO', 'PENDIENTE'];

/**
 * Lugares (direcciones y zonas) y la cola de "ubicaciones a revisar": las que vinieron de la
 * geocodificación automática hay que confirmarlas o corregirlas antes de poder publicar.
 */
@Component({
  selector: 'app-lugares-page',
  imports: [
    ReactiveFormsModule, DecimalPipe, EstadoChip, MatTableModule, MatPaginatorModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatButtonToggleModule, MatIconModule, MatProgressBarModule, MatTooltipModule,
  ],
  template: `
    <section class="pagina">
      <div class="encabezado">
        <h1>Lugares y ubicaciones</h1>
        @if (puedeEditar) {
          <button mat-flat-button (click)="editar(null)"><mat-icon>add</mat-icon> Nuevo lugar</button>
        }
      </div>
      <form class="filtros" [formGroup]="filtros">
        <mat-button-toggle-group formControlName="vista" aria-label="Vista">
          <mat-button-toggle value="revisar">A revisar</mat-button-toggle>
          <mat-button-toggle value="todos">Todos</mat-button-toggle>
        </mat-button-toggle-group>
        <mat-form-field class="busqueda">
          <mat-label>Buscar por nombre o calle</mat-label>
          <mat-icon matPrefix>search</mat-icon>
          <input matInput formControlName="q" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Estado de ubicación</mat-label>
          <mat-select formControlName="estadoGeo" multiple>
            @for (e of estadosGeo; track e.valor) { <mat-option [value]="e.valor">{{ e.texto }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Localidad</mat-label>
          <mat-select formControlName="localidadId">
            <mat-option [value]="null">Todas</mat-option>
            @for (l of localidades(); track l.id) { <mat-option [value]="l.id">{{ l.nombre }}</mat-option> }
          </mat-select>
        </mat-form-field>
      </form>
      @if (filtros.controls.vista.value === 'revisar') {
        <p class="suave ayuda">
          Revisá cada dirección: si las coordenadas son correctas tocá <mat-icon inline>check</mat-icon>; si no, editá el lugar y
          cargá la latitud y longitud correctas (podés copiarlas de OpenStreetMap o Google Maps).
        </p>
      }
      <div class="tarjeta">
        @if (cargando()) { <mat-progress-bar mode="indeterminate" /> }
        <table mat-table [dataSource]="pagina()?.contenido ?? []" class="tabla">
          <ng-container matColumnDef="nombre">
            <th mat-header-cell *matHeaderCellDef>Nombre</th>
            <td mat-cell *matCellDef="let l">
              <strong>{{ l.nombre }}</strong>
              @if (l.tipoLugar || l.padre) { <div class="suave">{{ l.tipoLugar }}{{ l.padre ? ' · en ' + l.padre : '' }}</div> }
            </td>
          </ng-container>
          <ng-container matColumnDef="direccion">
            <th mat-header-cell *matHeaderCellDef>Dirección</th>
            <td mat-cell *matCellDef="let l">{{ direccion(l) }}</td>
          </ng-container>
          <ng-container matColumnDef="coordenadas">
            <th mat-header-cell *matHeaderCellDef>Coordenadas</th>
            <td mat-cell *matCellDef="let l">
              @if (l.coordenadas) {
                <a [href]="osm(l)" target="_blank" rel="noopener" matTooltip="Abrir en OpenStreetMap">
                  {{ l.coordenadas.lat | number: '1.5-5' }}, {{ l.coordenadas.lng | number: '1.5-5' }}
                </a>
              } @else { <span class="suave">—</span> }
            </td>
          </ng-container>
          <ng-container matColumnDef="estado">
            <th mat-header-cell *matHeaderCellDef>Estado</th>
            <td mat-cell *matCellDef="let l">@if (l.estadoGeo) { <app-estado [valor]="l.estadoGeo" /> }</td>
          </ng-container>
          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let l" class="acciones-celda">
              @if (puedeEditar) {
                @if (l.coordenadas && l.estadoGeo !== 'VERIFICADO') {
                  <button mat-icon-button matTooltip="Confirmar ubicación" (click)="verificar(l)"><mat-icon>check</mat-icon></button>
                }
                <button mat-icon-button matTooltip="Editar" (click)="editar(l)"><mat-icon>edit</mat-icon></button>
              }
              @if (esAdmin) {
                <button mat-icon-button matTooltip="Eliminar" (click)="eliminar(l)"><mat-icon>delete</mat-icon></button>
              }
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let l; columns: columnas"></tr>
        </table>
        @if (pagina()?.contenido?.length === 0) {
          <p class="vacio">{{ filtros.controls.vista.value === 'revisar' ? 'No quedan ubicaciones por revisar.' : 'No hay lugares con esos filtros.' }}</p>
        }
        <mat-paginator [length]="pagina()?.totalElementos ?? 0" [pageIndex]="pagina()?.pagina ?? 0" [pageSize]="tamanio"
                       [pageSizeOptions]="[20, 50, 100]" (page)="paginar($event)" />
      </div>
    </section>
  `,
  styles: `
    .ayuda { margin: -8px 0 0; }
    .ayuda mat-icon { vertical-align: -3px; }
    .acciones-celda { white-space: nowrap; text-align: right; }
  `,
})
export class LugaresPage {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly confirmar = inject(Confirmar);
  private readonly dialog = inject(MatDialog);
  private readonly auth = inject(AuthService);
  protected readonly puedeEditar = this.auth.tieneRol('ADMIN', 'CARGADOR');
  protected readonly esAdmin = this.auth.tieneRol('ADMIN');

  protected readonly localidades = toSignal(inject(Catalogos).de('localidad'), { initialValue: [] });
  protected readonly estadosGeo = ESTADOS_GEO;
  protected readonly columnas = ['nombre', 'direccion', 'coordenadas', 'estado', 'acciones'];
  protected readonly pagina = signal<Pagina<Lugar> | null>(null);
  protected readonly cargando = signal(false);
  protected tamanio = 20;
  private numero = 0;
  private readonly recargar = new Subject<void>();

  protected readonly filtros = inject(NonNullableFormBuilder).group({
    vista: ['revisar' as 'revisar' | 'todos'],
    q: [''],
    estadoGeo: [A_REVISAR as string[]],
    localidadId: [null as number | null],
  });

  constructor() {
    const destroy = inject(DestroyRef);
    this.filtros.controls.vista.valueChanges.pipe(takeUntilDestroyed(destroy)).subscribe((v) =>
      this.filtros.controls.estadoGeo.setValue(v === 'revisar' ? A_REVISAR : []),
    );
    merge(this.filtros.valueChanges.pipe(debounceTime(300), startWith(null)), this.recargar)
      .pipe(
        switchMap((cambio) => {
          if (cambio !== undefined) {
            this.numero = 0;
          }
          const f = this.filtros.getRawValue();
          this.cargando.set(true);
          return this.api
            .get<Pagina<Lugar>>('/lugares', {
              q: f.q,
              estadoGeo: f.estadoGeo,
              localidadId: f.localidadId,
              page: this.numero,
              size: this.tamanio,
              sort: 'nombre',
            })
            .pipe(
              catchError((e) => {
                this.avisos.error(e);
                return EMPTY;
              }),
              finalize(() => this.cargando.set(false)),
            );
        }),
        takeUntilDestroyed(destroy),
      )
      .subscribe((p) => this.pagina.set(p));
  }

  protected direccion(l: Lugar): string {
    const calle = [l.calle, l.altura].filter((x) => x != null && x !== '').join(' ');
    const km = l.km != null ? `km ${l.km}` : '';
    return [calle, km, l.calleCruce && `esq. ${l.calleCruce}`, l.complemento, l.localidad].filter(Boolean).join(', ');
  }

  protected osm(l: Lugar): string {
    const { lat, lng } = l.coordenadas!;
    return `https://www.openstreetmap.org/?mlat=${lat}&mlon=${lng}#map=18/${lat}/${lng}`;
  }

  protected paginar(e: PageEvent): void {
    this.numero = e.pageIndex;
    this.tamanio = e.pageSize;
    this.recargar.next();
  }

  protected editar(l: Lugar | null): void {
    const abrir = (data: Lugar | null) =>
      this.dialog
        .open(LugarDialog, { data, width: '720px' })
        .afterClosed()
        .subscribe((r?: Lugar) => r && this.recargar.next());
    if (l) {
      // El detalle trae datos que el listado no (hijos, geometría completa).
      this.api.get<Lugar>(`/lugares/${l.id}`).subscribe({ next: abrir, error: (e) => this.avisos.error(e) });
    } else {
      abrir(null);
    }
  }

  protected verificar(l: Lugar): void {
    const cuerpo = {
      nombre: l.nombre,
      padreId: l.padreId ?? null,
      tipoLugarId: l.tipoLugarId ?? null,
      localidadId: l.localidadId ?? null,
      calle: l.calle ?? null,
      altura: l.altura ?? null,
      km: l.km ?? null,
      calleCruce: l.calleCruce ?? null,
      complemento: l.complemento ?? null,
      coordenadas: l.coordenadas,
      geometria: l.geometria ?? null,
      estadoGeo: 'VERIFICADO',
    };
    this.api.put<Lugar>(`/lugares/${l.id}`, cuerpo).subscribe({
      next: () => {
        this.avisos.ok(`Ubicación de "${l.nombre}" confirmada`);
        this.recargar.next();
      },
      error: (e) => this.avisos.error(e),
    });
  }

  protected eliminar(l: Lugar): void {
    this.confirmar.pedir('Eliminar lugar', `¿Eliminar "${l.nombre}"? Solo se puede si ningún establecimiento lo usa.`, 'Eliminar').subscribe((ok) => {
      if (ok) {
        this.api.delete(`/lugares/${l.id}`).subscribe({
          next: () => {
            this.avisos.ok('Lugar eliminado');
            this.recargar.next();
          },
          error: (e) => this.avisos.error(e),
        });
      }
    });
  }
}
