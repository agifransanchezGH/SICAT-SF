import { DOCUMENT } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, ParamMap, Router, RouterLink, convertToParamMap } from '@angular/router';
import { EMPTY, Subject, catchError, combineLatest, debounceTime, finalize, map, skip, startWith, switchMap } from 'rxjs';

import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { Avisos } from '../core/avisos';
import { Catalogos } from '../core/catalogos';
import { EstadoChip } from '../core/estado';
import { FiltrosRecordados } from '../core/filtros-recordados';
import { ESTADOS_PUBLICACION, Establecimiento, EstablecimientoResumen, Pagina } from '../core/modelos';
import { AccionesEnLote, BarraSeleccion } from '../comun/lote';
import { Seleccion } from '../comun/seleccion';
import { NavegacionCola } from './cola-fichas';
import { accionRapida } from './publicacion-rapida';

@Component({
  selector: 'app-establecimientos-page',
  imports: [
    ReactiveFormsModule, RouterLink, EstadoChip,
    MatTableModule, MatPaginatorModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatCheckboxModule, MatButtonModule, MatIconModule, MatProgressBarModule, MatTooltipModule, BarraSeleccion,
  ],
  template: `
    <section class="pagina">
      <div class="encabezado">
        <h1>Establecimientos</h1>
        @if (puedeEditar) {
          <a mat-flat-button routerLink="nuevo"><mat-icon>add</mat-icon> Nuevo</a>
        }
      </div>

      <div class="accesos" role="group" aria-label="Accesos rápidos">
        <button type="button" class="chip" [attr.aria-pressed]="filtros.controls.listos.value" (click)="alternarListos()">
          <mat-icon aria-hidden="true">{{ filtros.controls.listos.value ? 'check' : 'rocket_launch' }}</mat-icon>
          Listos para publicar
        </button>
      </div>

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
            @for (e of estados; track e) {
              <mat-option [value]="e"><app-estado [valor]="e" /></mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Categoría</mat-label>
          <mat-select formControlName="categoria">
            <mat-option value="">Todas</mat-option>
            @for (c of catalogos.categorias(); track c.id) {
              <mat-option [value]="'' + c.id">{{ c.nombre }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Localidad</mat-label>
          <mat-select formControlName="localidadId">
            <mat-option value="">Todas</mat-option>
            @for (l of localidades(); track l.id) {
              <mat-option [value]="'' + l.id">{{ l.nombre }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Ubicación</mat-label>
          <mat-select formControlName="ubicacion" multiple>
            @for (u of ubicaciones; track u.valor) { <mat-option [value]="u.valor">{{ u.texto }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-checkbox formControlName="contactosARevisar"
                      title="Instagram o Facebook que no es un usuario ni una URL, o web con espacios">Contactos a revisar</mat-checkbox>
        <mat-checkbox formControlName="incluirInactivos">Incluir dados de baja</mat-checkbox>
      </form>

      <div class="tarjeta">
        @if (cargando()) {
          <mat-progress-bar mode="indeterminate" />
        }
        <table mat-table [dataSource]="pagina()?.contenido ?? []" class="tabla">
          <ng-container matColumnDef="seleccion">
            <th mat-header-cell *matHeaderCellDef class="celda-seleccion">
              @let filas = pagina()?.contenido ?? [];
              <mat-checkbox [checked]="seleccion.estadoDe(filas) === 'todos'" [indeterminate]="seleccion.estadoDe(filas) === 'algunos'"
                            [disabled]="!filas.length" (change)="seleccion.alternarPagina(filas)"
                            aria-label="Seleccionar todos los de esta página" />
            </th>
            <td mat-cell *matCellDef="let e" class="celda-seleccion" (click)="$event.stopPropagation()">
              <mat-checkbox [checked]="seleccion.tiene(e.id)" (change)="seleccion.alternar(e)" [aria-label]="'Seleccionar ' + e.nombre" />
            </td>
          </ng-container>
          <ng-container matColumnDef="nombre">
            <th mat-header-cell *matHeaderCellDef>Nombre</th>
            <td mat-cell *matCellDef="let e">
              <strong>{{ e.nombre }}</strong>
              @if (e.destacado) { <mat-icon class="destacado" inline title="Destacado">star</mat-icon> }
              <div class="suave">{{ e.oferente }}</div>
            </td>
          </ng-container>
          <ng-container matColumnDef="categorias">
            <th mat-header-cell *matHeaderCellDef>Categorías</th>
            <td mat-cell *matCellDef="let e">{{ e.categorias.join(', ') }}</td>
          </ng-container>
          <ng-container matColumnDef="localidad">
            <th mat-header-cell *matHeaderCellDef>Localidad</th>
            <td mat-cell *matCellDef="let e">{{ e.localidad }}</td>
          </ng-container>
          <ng-container matColumnDef="ubicacion">
            <th mat-header-cell *matHeaderCellDef>Ubicación</th>
            <td mat-cell *matCellDef="let e">
              @if (e.estadoGeo) { <app-estado [valor]="e.estadoGeo" /> }
              @else if (!e.coordenadas) { <span class="suave">Sin ubicación</span> }
            </td>
          </ng-container>
          <ng-container matColumnDef="estado">
            <th mat-header-cell *matHeaderCellDef>Estado</th>
            <td mat-cell *matCellDef="let e">
              <app-estado [valor]="e.estadoPublicacion" />
              @if (!e.activo) { <app-estado valor="inactivo" /> }
            </td>
          </ng-container>
          <ng-container matColumnDef="publicar">
            <th mat-header-cell *matHeaderCellDef><span class="cdk-visually-hidden">Publicación rápida</span></th>
            <td mat-cell *matCellDef="let e" class="celda-publicar">
              @let a = accion(e);
              @if (a.accion === 'despublicar') {
                <button mat-stroked-button type="button" class="boton-publicar" [disabled]="enviando().has(e.id)"
                        [attr.aria-label]="'Despublicar ' + e.nombre" (click)="alternar($event, e)">
                  <mat-icon>visibility_off</mat-icon> Despublicar
                </button>
              } @else {
                <!-- Deshabilitado pero enfocable: el motivo llega por tooltip y por aria-describedby -->
                <button mat-flat-button type="button" class="boton-publicar" disabledInteractive
                        [disabled]="!a.habilitada || enviando().has(e.id)"
                        [matTooltip]="a.motivo ?? ''" [matTooltipDisabled]="!a.motivo"
                        [attr.aria-label]="'Publicar ' + e.nombre" (click)="alternar($event, e)">
                  <mat-icon>public</mat-icon> Publicar
                </button>
              }
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let e; columns: columnas" class="fila-link" [class.elegida]="seleccion.tiene(e.id)"
              [routerLink]="['/establecimientos', e.id]" (click)="abrirFicha(e)"></tr>
        </table>
        @if (error()) {
          <div class="vacio" role="alert">
            <p>No se pudo cargar el listado.</p>
            <button mat-stroked-button type="button" (click)="reintentar()">Reintentar</button>
          </div>
        } @else if (pagina()?.contenido?.length === 0) {
          <p class="vacio">
            {{ filtros.controls.listos.value ? 'No hay establecimientos listos para publicar con esos filtros.' : 'No hay establecimientos con esos filtros.' }}
          </p>
        }
        @if (seleccion.cantidad()) {
          <app-barra-seleccion [cantidad]="seleccion.cantidad()" [nombres]="['establecimiento seleccionado', 'establecimientos seleccionados']"
                               (limpiar)="seleccion.limpiar()">
            <button mat-flat-button type="button" class="en-barra" (click)="publicarSeleccionados()">
              <mat-icon>public</mat-icon> Publicar seleccionados
            </button>
          </app-barra-seleccion>
        }
        <mat-paginator
          [length]="pagina()?.totalElementos ?? 0"
          [pageIndex]="pagina()?.pagina ?? 0"
          [pageSize]="pagina()?.tamanio ?? 20"
          [pageSizeOptions]="[20, 50, 100]"
          (page)="paginar($event)" />
      </div>
    </section>
  `,
  styles: `
    .destacado { color: #c79100; vertical-align: -2px; margin-left: 4px; }
    td app-estado + app-estado { margin-left: 4px; }
    .celda-publicar { white-space: nowrap; width: 1%; }
    /* Casilla con área de 44 px: se puede tildar sin abrir la ficha */
    .celda-seleccion { width: 1%; padding-right: 0 !important; --mat-checkbox-state-layer-size: 44px; }
    tr.elegida { background: color-mix(in srgb, var(--sicat-rio-claro) 55%, transparent); }
    .en-barra { --mat-button-filled-container-color: #ffffff; --mat-button-filled-label-text-color: var(--sicat-tinta); }
    .accesos { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
    /* Acceso rápido: pastilla con estado (aria-pressed); al activarse cambia ícono y relleno, no solo el color */
    .chip {
      display: inline-flex; align-items: center; gap: 6px; min-height: 36px; padding: 0 14px 0 10px;
      border: 1px solid var(--mat-sys-outline); border-radius: 18px; background: var(--mat-sys-surface);
      color: var(--sicat-tinta); font: inherit; font-size: 14px; font-weight: 600; cursor: pointer;
    }
    .chip mat-icon { font-size: 18px; width: 18px; height: 18px; color: var(--sicat-rio); }
    .chip:hover { border-color: var(--sicat-rio); }
    .chip[aria-pressed='true'] { background: var(--sicat-rio-claro); border-color: var(--sicat-rio); color: var(--sicat-rio); }
    .chip:focus-visible { outline: 2px solid var(--sicat-rio); outline-offset: 2px; }
    /* mismo ancho y alto para los dos estados: la columna queda alineada al alternar */
    .boton-publicar { width: 9.5rem; height: 36px; padding-inline: 12px; font-size: 13px; }
    .boton-publicar mat-icon { font-size: 18px; width: 18px; height: 18px; }
  `,
})
export class EstablecimientosPage {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  protected readonly catalogos = inject(Catalogos);
  protected readonly puedeEditar = inject(AuthService).tieneRol('ADMIN', 'CARGADOR');

  protected readonly estados = ESTADOS_PUBLICACION;
  /** "Sin ubicación": sin lugar o con un lugar sin coordenadas (no encontrado al importar). */
  protected readonly ubicaciones = [
    { valor: 'VERIFICADO', texto: 'Verificada' },
    { valor: 'AUTOMATICO', texto: 'Automática' },
    { valor: 'APROXIMADO', texto: 'Aproximada' },
    { valor: 'SIN_UBICACION', texto: 'Sin ubicación' },
  ];
  protected readonly columnas = [...(this.puedeEditar ? ['seleccion'] : []), 'nombre', 'categorias', 'localidad', 'ubicacion', 'estado',
    ...(this.puedeEditar ? ['publicar'] : [])];
  /** Selección para acciones en lote: se conserva al cambiar de página y se vacía al cambiar los filtros. */
  protected readonly seleccion = new Seleccion<EstablecimientoResumen>();
  private readonly lote = inject(AccionesEnLote);
  private readonly nav = inject(NavegacionCola);
  private ultimosFiltros: string | null = null;
  protected readonly localidades = toSignal(this.catalogos.de('localidad'), { initialValue: [] });
  protected readonly pagina = signal<Pagina<EstablecimientoResumen> | null>(null);
  protected readonly cargando = signal(false);
  /** Ids con un publicar/despublicar en curso (deshabilita su botón). */
  protected readonly enviando = signal<ReadonlySet<number>>(new Set());
  protected readonly error = signal(false);
  protected readonly accion = accionRapida;
  private readonly recargar = new Subject<void>();
  private readonly documento = inject(DOCUMENT);

  private readonly recordados = inject(FiltrosRecordados);
  /**
   * Si se llega sin filtros (flecha "Volver" del detalle, menú lateral, enlaces) se restauran los últimos
   * que se usaron, incluida la página: moverse entre establecimientos no pierde la búsqueda.
   */
  private readonly restaurados = Object.keys(this.route.snapshot.queryParams).length
    ? null
    : this.recordados.leer('establecimientos');
  private readonly inicial = this.restaurados ? convertToParamMap(this.restaurados) : this.route.snapshot.queryParamMap;
  protected readonly filtros = inject(NonNullableFormBuilder).group({
    q: this.inicial.get('q') ?? '',
    estado: this.inicial.get('estado') ?? '',
    categoria: this.inicial.get('categoria') ?? '',
    localidadId: this.inicial.get('localidadId') ?? '',
    ubicacion: [this.inicial.getAll('ubicacion')],
    contactosARevisar: this.inicial.get('contactosARevisar') === 'true',
    listos: this.inicial.get('listos') === 'true',
    incluirInactivos: this.inicial.get('incluirInactivos') === 'true',
  });

  constructor() {
    const destroy = inject(DestroyRef);
    this.catalogos.arbolCategorias().pipe(takeUntilDestroyed(destroy)).subscribe();

    // Los filtros viven en la URL: al volver del detalle se conserva la búsqueda.
    this.filtros.valueChanges.pipe(debounceTime(300), takeUntilDestroyed(destroy)).subscribe((f) =>
      this.router.navigate([], {
        queryParams: {
          ...f,
          contactosARevisar: f.contactosARevisar || null,
          listos: f.listos || null,
          incluirInactivos: f.incluirInactivos || null,
          pagina: null,
        },
        queryParamsHandling: 'merge',
        replaceUrl: true,
      }),
    );

    if (this.restaurados) {
      this.router.navigate([], { queryParams: this.restaurados, replaceUrl: true });
    }

    // al restaurar, la primera emisión es la URL vacía: se espera la restaurada
    const urlActual = this.route.queryParamMap.pipe(skip(this.restaurados ? 1 : 0));
    combineLatest([urlActual, this.recargar.pipe(startWith(undefined))])
      .pipe(
        map(([p]) => p),
        switchMap((p) => {
          this.recordados.guardar('establecimientos', Object.fromEntries(p.keys.map((k) => [k, p.getAll(k)])));
          this.sincronizarFiltros(p);
          const filtrosActuales = JSON.stringify({ ...consulta(p), page: null, size: null });
          if (this.ultimosFiltros !== null && filtrosActuales !== this.ultimosFiltros) {
            this.seleccion.limpiar(); // lo elegido con otros filtros ya no está a la vista
          }
          this.ultimosFiltros = filtrosActuales;
          this.cargando.set(true);
          this.error.set(false);
          return this.api
            .get<Pagina<EstablecimientoResumen>>('/establecimientos', consulta(p))
            .pipe(
              catchError((e) => {
                this.avisos.error(e);
                this.error.set(true);
                return EMPTY;
              }),
              finalize(() => this.cargando.set(false)),
            );
        }),
        takeUntilDestroyed(destroy),
      )
      .subscribe((r) => this.pagina.set(r));
  }

  /** Guarda la página visible como cola: la ficha ofrece Anterior/Siguiente con los mismos filtros y orden. */
  protected abrirFicha(e: EstablecimientoResumen): void {
    const p = this.route.snapshot.queryParamMap;
    const pagina = this.pagina();
    if (!pagina) {
      return;
    }
    const etiqueta = p.get('listos') === 'true' ? 'Listos para publicar'
      : p.get('contactosARevisar') === 'true' ? 'Contactos a revisar'
      : p.keys.some((k) => !['pagina', 'tamanio'].includes(k) && p.get(k)) ? 'Establecimientos filtrados' : 'Establecimientos';
    this.nav.fijar(
      {
        etiqueta,
        volver: { ruta: '/establecimientos', queryParams: { ...this.route.snapshot.queryParams } },
        pagina: pagina.pagina,
        tamanio: pagina.tamanio,
        numerada: true,
        cargar: (n) => this.api
          .get<Pagina<EstablecimientoResumen>>('/establecimientos', { ...consulta(p), page: n })
          .pipe(map((r) => ({ ids: r.contenido.map((x) => x.id), total: r.totalElementos }))),
      },
      { ids: pagina.contenido.map((x) => x.id), total: pagina.totalElementos },
      e.id,
    );
  }

  /**
   * Publica la selección. La API valida cada uno en su propia transacción: los que no cumplen quedan como estaban
   * y el resultado dice qué le falta a cada uno. Los ya publicados no se envían.
   */
  protected publicarSeleccionados(): void {
    const elegidas = this.seleccion.elegidas();
    const aPublicar = elegidas.filter((e) => e.estadoPublicacion !== 'PUBLICADO');
    const cumplen = aPublicar.filter((e) => accionRapida(e).habilitada).length;
    const noCumplen = aPublicar.length - cumplen;
    const yaPublicados = elegidas.length - aPublicar.length;
    if (!aPublicar.length) {
      this.avisos.ok('Los seleccionados ya están publicados');
      return;
    }
    const partes = [
      `Se ${cumplen === 1 ? 'va' : 'van'} a publicar ${cumplen} de ${elegidas.length} ${elegidas.length === 1 ? 'seleccionado' : 'seleccionados'}.`,
      noCumplen ? `${noCumplen} no ${noCumplen === 1 ? 'cumple' : 'cumplen'} los requisitos: quedan como están y se informa qué le falta a cada uno.` : '',
      yaPublicados ? `${yaPublicados} ya ${yaPublicados === 1 ? 'estaba publicado' : 'estaban publicados'}.` : '',
    ];
    this.lote
      .ejecutar({
        titulo: 'Publicar seleccionados',
        mensaje: partes.filter(Boolean).join(' '),
        boton: cumplen ? `Publicar ${cumplen}` : 'Intentar igual',
        ruta: '/establecimientos/lote/publicar',
        ids: aPublicar.map((e) => e.id),
        participio: 'publicado',
        infinitivo: 'publicar',
      })
      .subscribe((r) => {
        this.seleccion.quitar(r.resultados.filter((x) => x.ok).map((x) => x.id));
        this.recargar.next();
      });
  }

  /** "Listos para publicar" muestra borradores y ocultos: un estado elegido a mano lo contradiría. */
  protected alternarListos(): void {
    const activar = !this.filtros.controls.listos.value;
    this.filtros.patchValue(activar ? { listos: true, estado: '' } : { listos: false });
  }

  protected reintentar(): void {
    this.recargar.next();
  }

  /**
   * Si la URL cambia desde afuera (p. ej. el acceso "Listos para publicar" del menú con la página ya abierta),
   * el formulario la refleja sin volver a navegar.
   */
  private sincronizarFiltros(p: ParamMap): void {
    // Mientras se escribe en la búsqueda no se pisa lo tipeado (la URL va 300 ms atrás).
    const escribiendo = this.documento.activeElement?.getAttribute('formControlName') === 'q';
    const desdeUrl = {
      q: escribiendo ? this.filtros.controls.q.value : (p.get('q') ?? ''),
      estado: p.get('estado') ?? '',
      categoria: p.get('categoria') ?? '',
      localidadId: p.get('localidadId') ?? '',
      ubicacion: p.getAll('ubicacion'),
      contactosARevisar: p.get('contactosARevisar') === 'true',
      listos: p.get('listos') === 'true',
      incluirInactivos: p.get('incluirInactivos') === 'true',
    };
    if (JSON.stringify(desdeUrl) !== JSON.stringify(this.filtros.getRawValue())) {
      this.filtros.setValue(desdeUrl, { emitEvent: false });
    }
  }

  /**
   * Publicar / despublicar desde el listado. El botón Publicar se deshabilita si la fila trae requisitos
   * faltantes; si igual falta algo (cambió entre medio), la API responde 422 y el aviso dice qué.
   */
  protected alternar(evento: Event, e: EstablecimientoResumen): void {
    evento.stopPropagation(); // la fila entera es un enlace al detalle
    if (!accionRapida(e).habilitada) {
      return; // disabledInteractive deja el botón enfocable y clicable: el clic no hace nada
    }
    const publicar = e.estadoPublicacion !== 'PUBLICADO';
    this.enviando.update((s) => new Set(s).add(e.id));
    this.api
      .post<Establecimiento>(`/establecimientos/${e.id}/${publicar ? 'publicar' : 'despublicar'}`)
      .pipe(finalize(() => this.enviando.update((s) => { const n = new Set(s); n.delete(e.id); return n; })))
      .subscribe({
        next: (r) => {
          this.pagina.update((p) => p && {
            ...p,
            contenido: p.contenido.map((x) => (x.id === e.id ? { ...x, estadoPublicacion: r.estadoPublicacion } : x)),
          });
          this.avisos.ok(`${e.nombre}: ${publicar ? 'publicado' : 'despublicado'}`);
        },
        error: (err) => this.avisos.error(err, `No se pudo ${publicar ? 'publicar' : 'despublicar'} ${e.nombre}`),
      });
  }

  protected paginar(e: PageEvent): void {
    this.router.navigate([], {
      queryParams: { pagina: e.pageIndex || null, tamanio: e.pageSize === 20 ? null : e.pageSize },
      queryParamsHandling: 'merge',
    });
  }
}

/** Parámetros de GET /establecimientos para los filtros de la URL (mismo orden en el listado y en la cola). */
function consulta(p: ParamMap) {
  return {
    q: p.get('q'),
    estado: p.get('estado'),
    categoria: p.get('categoria'),
    localidadId: p.get('localidadId'),
    ubicacion: p.getAll('ubicacion'),
    contactosARevisar: p.get('contactosARevisar'),
    listos: p.get('listos'),
    incluirInactivos: p.get('incluirInactivos'),
    page: p.get('pagina') ?? 0,
    size: p.get('tamanio') ?? 20,
    sort: 'nombre',
  };
}
