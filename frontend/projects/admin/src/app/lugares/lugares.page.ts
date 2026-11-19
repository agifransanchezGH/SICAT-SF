import { DecimalPipe } from '@angular/common';
import { LiveAnnouncer } from '@angular/cdk/a11y';
import { Component, DestroyRef, Injector, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { FiltrosRecordados } from '../core/filtros-recordados';
import { EMPTY, Subject, catchError, debounceTime, finalize, map, merge, startWith, switchMap } from 'rxjs';

import { AccionesEnLote, BarraSeleccion } from '../comun/lote';
import { Seleccion } from '../comun/seleccion';
import { NavegacionCola } from '../establecimientos/cola-fichas';

import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { Avisos } from '../core/avisos';
import { Catalogos } from '../core/catalogos';
import { Confirmar } from '../core/confirmar';
import { EstadoChip } from '../core/estado';
import { ESTADOS_GEO, EstadoGeo, Lugar, Pagina } from '../core/modelos';
import { CalidadChip, cuerpoLugar } from './calidad';
import { POSICION_EN_COLA, elegirSiguiente } from './cola';
import { PanelUbicacion } from './panel-ubicacion';
import { LugarDialog, LugarGuardado } from './lugar-dialog';

const A_REVISAR: EstadoGeo[] = ['AUTOMATICO', 'APROXIMADO', 'NO_ENCONTRADO', 'PENDIENTE'];

/**
 * Lugares (direcciones y zonas) y la cola de "ubicaciones a revisar": las que vinieron de la
 * geocodificación automática hay que confirmarlas o corregirlas antes de poder publicar.
 */
@Component({
  selector: 'app-lugares-page',
  imports: [
    ReactiveFormsModule, DecimalPipe, RouterLink, EstadoChip, MatTableModule, MatPaginatorModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatButtonToggleModule, MatIconModule, MatProgressBarModule, MatTooltipModule,
    MatCheckboxModule, BarraSeleccion, PanelUbicacion, CalidadChip,
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
          <mat-label>Buscar por nombre, calle o establecimiento</mat-label>
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
      @if (enRevision()) {
        <p class="suave ayuda">
          Primero van las más dudosas. Elegí una de la lista: si el pin está bien, confirmala; si no, arrastralo o tocá el punto
          correcto y confirmá (<kbd>Ctrl+Enter</kbd>). Después pasa sola a la siguiente. Con las flechas ↑ ↓ te movés por la lista.
        </p>
      }
      <div [class.revision]="enRevision()">
      <div class="tarjeta">
        @if (cargando()) { <mat-progress-bar mode="indeterminate" /> }
        @if (enRevision()) {
          <ul class="cola-lista" aria-label="Ubicaciones a revisar" (keydown)="moverConFlechas($event)">
            @for (l of pagina()?.contenido ?? []; track l.id) {
              <li [class.actual]="seleccionado()?.id === l.id">
                @if (puedeEditar) {
                  @if (elegible(l)) {
                    <mat-checkbox class="casilla" [checked]="seleccion.tiene(l.id)" (change)="seleccion.alternar(l)" [aria-label]="'Seleccionar ' + l.nombre" />
                  } @else {
                    <span class="casilla" aria-hidden="true"></span>
                  }
                }
                <button type="button" class="item" [attr.data-id]="l.id" [attr.aria-current]="seleccionado()?.id === l.id ? 'true' : null"
                        (click)="seleccionar(l)">
                  <span class="item-nombre">{{ l.nombre }}</span>
                  <span class="item-dir">{{ direccion(l) || 'Sin dirección' }}</span>
                  <span class="item-senales"><app-calidad [calidad]="l.calidad" />@if (l.estadoGeo) { <app-estado [valor]="l.estadoGeo" /> }</span>
                </button>
              </li>
            }
          </ul>
        } @else {
        <table mat-table [dataSource]="pagina()?.contenido ?? []" class="tabla">
          <ng-container matColumnDef="seleccion">
            <th mat-header-cell *matHeaderCellDef class="celda-seleccion">
              @let elegibles = elegiblesDePagina();
              <mat-checkbox [checked]="seleccion.estadoDe(elegibles) === 'todos'" [indeterminate]="seleccion.estadoDe(elegibles) === 'algunos'"
                            [disabled]="!elegibles.length" (change)="seleccion.alternarPagina(elegibles)"
                            aria-label="Seleccionar las ubicaciones automáticas de esta página" />
            </th>
            <td mat-cell *matCellDef="let l" class="celda-seleccion">
              @if (elegible(l)) {
                <mat-checkbox [checked]="seleccion.tiene(l.id)" (change)="seleccion.alternar(l)" [aria-label]="'Seleccionar ' + l.nombre" />
              } @else {
                <!-- Solo las automáticas con coordenadas se confirman en lote: las demás se miran en el mapa -->
                <mat-checkbox disabled disabledInteractive [aria-label]="'Seleccionar ' + l.nombre"
                              matTooltip="En lote solo se confirman ubicaciones automáticas con coordenadas" />
              }
            </td>
          </ng-container>
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
          <ng-container matColumnDef="establecimientos">
            <th mat-header-cell *matHeaderCellDef>Establecimientos</th>
            <td mat-cell *matCellDef="let l" class="establecimientos">
              @for (e of l.establecimientos ?? []; track e.id) {
                <div>
                  <a [routerLink]="['/establecimientos', e.id]" [class.baja]="!e.activo" (click)="abrirFicha(e.id)">{{ e.nombre }}</a>
                  @if (e.estadoPublicacion === 'PUBLICADO') { <mat-icon inline class="publicado" matTooltip="Publicado">public</mat-icon> }
                  @if (!e.activo) { <span class="suave"> (de baja)</span> }
                </div>
              } @empty {
                <span class="suave">Ninguno</span>
              }
            </td>
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
                  <button mat-icon-button matTooltip="Confirmar ubicación" [attr.aria-label]="'Confirmar ubicación de ' + l.nombre"
                          (click)="verificar(l)"><mat-icon>check</mat-icon></button>
                }
                <button mat-icon-button matTooltip="Editar" [attr.aria-label]="'Editar ' + l.nombre" (click)="editar(l)"><mat-icon>edit</mat-icon></button>
              }
              @if (esAdmin) {
                <button mat-icon-button matTooltip="Eliminar" [attr.aria-label]="'Eliminar ' + l.nombre" (click)="eliminar(l)"><mat-icon>delete</mat-icon></button>
              }
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let l; columns: columnas"></tr>
        </table>
        }
        @if (error()) {
          <div class="vacio" role="alert">
            <p>No se pudo cargar la lista de lugares.</p>
            <button mat-stroked-button type="button" (click)="reintentar()">Reintentar</button>
          </div>
        } @else if (pagina()?.contenido?.length === 0) {
          <p class="vacio">{{ filtros.controls.vista.value === 'revisar' ? 'No quedan ubicaciones por revisar.' : 'No hay lugares con esos filtros.' }}</p>
        }
        @if (seleccion.cantidad()) {
          <app-barra-seleccion [cantidad]="seleccion.cantidad()" [nombres]="['ubicación seleccionada', 'ubicaciones seleccionadas']" (limpiar)="seleccion.limpiar()">
            <button mat-flat-button type="button" class="en-barra" (click)="confirmarSeleccionadas()">
              <mat-icon>check</mat-icon> Confirmar ubicaciones
            </button>
          </app-barra-seleccion>
        }
        <mat-paginator [length]="pagina()?.totalElementos ?? 0" [pageIndex]="pagina()?.pagina ?? 0" [pageSize]="tamanio"
                       [pageSizeOptions]="[20, 50, 100]" (page)="paginar($event)" />
      </div>
      @if (enRevision()) {
        <div class="tarjeta panel-mapa">
          <app-panel-ubicacion [lugar]="seleccionado()" [editable]="puedeEditar" (confirmado)="alConfirmar($event)"
                               (saltar)="saltarActual()" (editar)="editar($event)" />
        </div>
      }
      </div>
    </section>
  `,
  styles: `
    .ayuda { margin: -8px 0 0; }
    .ayuda mat-icon { vertical-align: -3px; }
    .ayuda kbd { padding: 0 4px; border: 1px solid var(--mat-sys-outline); border-radius: 4px; font-family: inherit; font-size: 12px; font-weight: 600; line-height: 1.4; }
    .acciones-celda { white-space: nowrap; text-align: right; }
    .celda-seleccion { width: 1%; padding-right: 0 !important; --mat-checkbox-state-layer-size: 44px; }
    .en-barra { --mat-button-filled-container-color: #ffffff; --mat-button-filled-label-text-color: var(--sicat-tinta); }
    .establecimientos { max-width: 280px; }
    .establecimientos a.baja { text-decoration: line-through; }
    .establecimientos .publicado { color: var(--sicat-ok); vertical-align: -2px; margin-left: 2px; }
    /* Cola en dos paneles: lista a la izquierda, mapa a la derecha (fijo mientras se recorre la lista) */
    .revision { display: grid; grid-template-columns: minmax(300px, 2fr) minmax(0, 3fr); gap: 16px; align-items: start; }
    .panel-mapa { position: sticky; top: 8px; padding: 16px; }
    @media (max-width: 1100px) {
      .revision { grid-template-columns: 1fr; }
      .panel-mapa { position: static; order: -1; }
    }
    .cola-lista { list-style: none; margin: 0; padding: 0; }
    .cola-lista li { display: flex; align-items: stretch; border-bottom: 1px solid var(--sicat-borde); }
    .cola-lista li.actual { background: var(--sicat-rio-claro); }
    .casilla { flex: none; width: 48px; display: flex; align-items: center; justify-content: center; --mat-checkbox-state-layer-size: 44px; }
    .item {
      flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; padding: 10px 16px 10px 4px; min-height: 56px;
      border: 0; background: none; text-align: left; font: inherit; color: var(--sicat-tinta); cursor: pointer;
    }
    .cola-lista li:not(:has(.casilla)) .item { padding-left: 16px; }
    .item:hover { background: color-mix(in srgb, var(--sicat-rio-claro) 50%, transparent); }
    .item:focus-visible { outline: 2px solid var(--sicat-rio); outline-offset: -2px; }
    .item[aria-current='true'] { box-shadow: inset 0 0 0 2px var(--sicat-rio); }
    .item-nombre { font-weight: 700; }
    .item-dir { font-size: 13.5px; color: var(--sicat-texto-suave); }
    .item-senales { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 2px; }
  `,
})
export class LugaresPage {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly confirmar = inject(Confirmar);
  private readonly dialog = inject(MatDialog);
  private readonly auth = inject(AuthService);
  private readonly anunciar = inject(LiveAnnouncer);
  private readonly injector = inject(Injector);
  /** Se llama una vez con la próxima página que llegue (para "Confirmar y siguiente"). */
  private alCargar?: (p: Pagina<Lugar>) => void;
  protected readonly puedeEditar = this.auth.tieneRol('ADMIN', 'CARGADOR');
  protected readonly esAdmin = this.auth.tieneRol('ADMIN');

  protected readonly localidades = toSignal(inject(Catalogos).de('localidad'), { initialValue: [] });
  protected readonly estadosGeo = ESTADOS_GEO;
  protected readonly columnas = [...(this.puedeEditar ? ['seleccion'] : []), 'nombre', 'direccion', 'establecimientos', 'coordenadas', 'estado', 'acciones'];
  /** Selección para confirmar en lote: se conserva al cambiar de página y se vacía al cambiar los filtros. */
  protected readonly seleccion = new Seleccion<Lugar>();
  private readonly lote = inject(AccionesEnLote);
  private readonly nav = inject(NavegacionCola);
  /** En lote solo se confirman las que OpenStreetMap encontró con su número (ADR 0010). */
  protected readonly elegible = (l: Lugar) => l.estadoGeo === 'AUTOMATICO' && !!l.coordenadas;
  protected readonly elegiblesDePagina = () => (this.pagina()?.contenido ?? []).filter(this.elegible);
  /** Vista "A revisar": lista y mapa en dos paneles, ordenada por duda. */
  protected readonly enRevision = () => this.vista() === 'revisar';
  private readonly vista = signal<'revisar' | 'todos'>('revisar');
  /** Lugar abierto en el panel del mapa. */
  protected readonly seleccionado = signal<Lugar | null>(null);
  protected readonly pagina = signal<Pagina<Lugar> | null>(null);
  protected readonly cargando = signal(false);
  protected readonly error = signal(false);
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
    // Al volver de un establecimiento (o desde el menú) se restauran los últimos filtros y la página.
    const recordados = inject(FiltrosRecordados);
    const g = recordados.leer('lugares');
    if (g) {
      this.filtros.setValue({
        vista: g['vista'] === 'todos' ? 'todos' : 'revisar',
        q: g['q'] ?? '',
        estadoGeo: g['estadoGeo'] ?? [], // una lista vacía no se guarda
        localidadId: g['localidadId'] ?? null,
      });
      this.numero = g['pagina'] ?? 0;
      this.tamanio = g['tamanio'] ?? 20;
    }
    this.filtros.valueChanges.pipe(takeUntilDestroyed(destroy)).subscribe(() => this.seleccion.limpiar());
    this.filtros.controls.vista.valueChanges.pipe(takeUntilDestroyed(destroy)).subscribe((v) =>
      this.filtros.controls.estadoGeo.setValue(v === 'revisar' ? A_REVISAR : []),
    );
    // startWith(undefined): la primera carga no vuelve a la página 0 (sí los cambios de filtro).
    merge(this.filtros.valueChanges.pipe(debounceTime(300), startWith(undefined)), this.recargar)
      .pipe(
        switchMap((cambio) => {
          if (cambio !== undefined) {
            this.numero = 0;
          }
          const f = this.filtros.getRawValue();
          recordados.guardar('lugares', { ...f, pagina: this.numero || null, tamanio: this.tamanio === 20 ? null : this.tamanio });
          this.cargando.set(true);
          this.error.set(false);
          return this.api
            .get<Pagina<Lugar>>('/lugares', this.consulta(this.numero))
            .pipe(
              catchError((e) => {
                this.avisos.error(e);
                this.error.set(true);
                this.alCargar = undefined; // un "siguiente" pendiente no se abre sobre datos viejos
                return EMPTY;
              }),
              finalize(() => this.cargando.set(false)),
            );
        }),
        takeUntilDestroyed(destroy),
      )
      .subscribe((p) => {
        this.pagina.set(p);
        this.vista.set(this.filtros.controls.vista.value);
        const actual = this.seleccionado();
        // Se conserva el elegido (con sus datos nuevos); si ya no está en la página, se elige el primero.
        this.seleccionado.set(p.contenido.find((x) => x.id === actual?.id) ?? p.contenido[0] ?? null);
        const despues = this.alCargar;
        this.alCargar = undefined;
        despues?.(p);
      });
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

  private consulta(pagina: number) {
    const f = this.filtros.getRawValue();
    // En "A revisar", primero lo más dudoso (NO_ENCONTRADO, APROXIMADO…); en "Todos", por nombre.
    const orden = f.vista === 'revisar' ? { orden: 'duda' } : { sort: 'nombre' };
    return { q: f.q, estadoGeo: f.estadoGeo, localidadId: f.localidadId, page: pagina, size: this.tamanio, ...orden };
  }

  /**
   * Al abrir un establecimiento desde la lista, sus vecinos son los de los lugares siguientes (mismos filtros y
   * orden). Se pagina por lugares, así que la ficha navega sin "5 de N".
   */
  protected abrirFicha(id: number): void {
    const p = this.pagina();
    if (!p) {
      return;
    }
    const idsDe = (lugares: Lugar[]) => lugares.flatMap((l) => (l.establecimientos ?? []).map((e) => e.id));
    const filtros = this.filtros.getRawValue();
    this.nav.fijar(
      {
        etiqueta: filtros.vista === 'revisar' ? 'Ubicaciones a revisar' : 'Lugares',
        volver: { ruta: '/lugares', queryParams: {} }, // la pantalla recuerda sus filtros y su página
        pagina: this.numero,
        tamanio: this.tamanio,
        numerada: false,
        cargar: (n) => this.api.get<Pagina<Lugar>>('/lugares', this.consulta(n))
          .pipe(map((r) => ({ ids: idsDe(r.contenido), total: r.totalElementos }))),
      },
      { ids: idsDe(p.contenido), total: p.totalElementos },
      id,
    );
  }

  /** Confirma la selección; cada una en su transacción y el resultado dice cuáles no se pudieron y por qué. */
  protected confirmarSeleccionadas(): void {
    const n = this.seleccion.cantidad();
    this.lote
      .ejecutar({
        titulo: 'Confirmar ubicaciones',
        mensaje: `Se ${n === 1 ? 'marca' : 'marcan'} como verificada${n === 1 ? '' : 's'} ${n} ${n === 1 ? 'ubicación automática' : 'ubicaciones automáticas'}, con el punto que tienen. Confirmalas solo si ya las revisaste.`,
        boton: `Confirmar ${n}`,
        ruta: '/lugares/lote/verificar',
        ids: this.seleccion.ids(),
        participio: 'confirmada',
        infinitivo: 'confirmar',
      })
      .subscribe((r) => {
        this.seleccion.quitar(r.resultados.filter((x) => x.ok).map((x) => x.id));
        this.recargar.next();
      });
  }

  protected reintentar(): void {
    this.recargar.next();
  }

  protected paginar(e: PageEvent): void {
    this.numero = e.pageIndex;
    this.tamanio = e.pageSize;
    this.recargar.next();
  }

  protected editar(l: Lugar | null): void {
    if (!l) {
      this.dialog.open(LugarDialog, { data: null, width: '720px' }).afterClosed()
        .subscribe((r?: LugarGuardado) => r && this.recargar.next());
      return;
    }
    // El detalle trae datos que el listado no (hijos, geometría completa).
    this.api.get<Lugar>(`/lugares/${l.id}`).subscribe({ next: (d) => this.abrirEnCola(d), error: (e) => this.avisos.error(e) });
  }

  /** Abre un lugar de la lista con su posición; "Confirmar y siguiente" sigue con el próximo de la misma lista. */
  private abrirEnCola(l: Lugar): void {
    const p = this.pagina();
    const indice = p?.contenido.findIndex((x) => x.id === l.id) ?? -1;
    const cola = { posicion: this.numero * this.tamanio + indice + 1, total: p?.totalElementos ?? 0 };
    const injector = Injector.create({ providers: [{ provide: POSICION_EN_COLA, useValue: cola }], parent: this.injector });
    this.dialog
      .open(LugarDialog, { data: l, width: '720px', injector })
      .afterClosed()
      .subscribe((r?: LugarGuardado) => {
        if (r?.pasarAlSiguiente) {
          this.pasarAlSiguiente(l, !!r.saltado);
        } else if (r) {
          this.recargar.next();
        }
      });
  }

  /** Recarga la cola (el confirmado puede salir de ella) y abre el que seguía; si era el último de la página, pasa a la próxima. */
  private pasarAlSiguiente(actual: Lugar, saltado = false, abrir: (l: Lugar) => void = (l) => this.editar(l)): void {
    const anterior = this.pagina()?.contenido ?? [];
    const hecho = saltado ? `${actual.nombre} sin cambios` : `Ubicación de ${actual.nombre} confirmada`;
    this.alCargar = (nueva) => {
      const siguiente = elegirSiguiente(anterior, nueva.contenido, actual.id);
      if (siguiente) {
        this.anunciar.announce(`${hecho}. Ahora: ${siguiente.nombre}.`, 'polite');
        abrir(siguiente);
      } else if ((this.numero + 1) * this.tamanio < nueva.totalElementos) {
        this.alCargar = (otra) => {
          const primero = otra.contenido[0];
          if (primero) {
            this.anunciar.announce(`${hecho}. Página siguiente: ${primero.nombre}.`, 'polite');
            abrir(primero);
          }
        };
        this.numero++;
        this.recargar.next();
      } else {
        this.avisos.ok(`${hecho}. No quedan más en esta lista.`);
      }
    };
    this.recargar.next();
  }

  /** Elige el lugar del panel del mapa y lleva el foco a su fila (para seguir con las flechas). */
  protected seleccionar(l: Lugar, enfocar = false): void {
    this.seleccionado.set(l);
    // En pantallas angostas el mapa está arriba de la lista: al tocar un lugar se lo trae a la vista.
    if (!enfocar && matchMedia('(max-width: 1100px)').matches) {
      const reducido = matchMedia('(prefers-reduced-motion: reduce)').matches;
      setTimeout(() => document.querySelector('.panel-mapa')?.scrollIntoView({ behavior: reducido ? 'auto' : 'smooth', block: 'start' }));
    }
    if (enfocar) {
      setTimeout(() => document.querySelector<HTMLElement>(`.cola-lista .item[data-id="${l.id}"]`)?.focus());
    }
  }

  /** ↑ ↓ en la lista: pasa al anterior o al siguiente y lo muestra en el mapa. */
  protected moverConFlechas(e: KeyboardEvent): void {
    if (e.key !== 'ArrowDown' && e.key !== 'ArrowUp') {
      return;
    }
    const lista = this.pagina()?.contenido ?? [];
    const i = lista.findIndex((x) => x.id === this.seleccionado()?.id);
    const destino = lista[e.key === 'ArrowDown' ? i + 1 : i - 1];
    if (destino) {
      e.preventDefault();
      this.seleccionar(destino, true);
    }
  }

  /** Confirmada desde el panel: sale de la cola y se elige la siguiente (o la primera de la página que sigue). */
  protected alConfirmar(l: Lugar): void {
    this.pasarAlSiguiente(l, false, (s) => this.seleccionar(s, true));
  }

  protected saltarActual(): void {
    const l = this.seleccionado();
    if (l) {
      this.pasarAlSiguiente(l, true, (s) => this.seleccionar(s, true));
    }
  }

  protected verificar(l: Lugar): void {
    this.api.put<Lugar>(`/lugares/${l.id}`, cuerpoLugar(l, l.coordenadas ?? null, 'VERIFICADO')).subscribe({
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
