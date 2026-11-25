import { Component, DestroyRef, ElementRef, OnInit, computed, effect, inject, input, signal, viewChild } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { forkJoin } from 'rxjs';
import { normalizar } from 'shared';

import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { avisarSiHayCambios } from '../core/cambios-pendientes';
import { Catalogos } from '../core/catalogos';
import { CaracteristicaAsignada, CatalogoItem } from '../core/modelos';

interface Seleccion {
  marcada: boolean;
  cantidad: number | null;
}

interface Opcion extends CatalogoItem {
  clave: string;
}

interface Grupo {
  nombre: string;
  /** Opciones del grupo que pasan el filtro (orden alfabético). */
  visibles: Opcion[];
  total: number;
  marcadas: number;
}

/** Desde este ancho del componente (no de la ventana) se usa el panel de grupos a la izquierda. */
const ANCHO_PANEL = 900;

/**
 * Características (servicios, accesibilidad, equipamiento…) de un establecimiento o un salón.
 * Buscador sin acentos ni mayúsculas, resumen fijo con lo elegido y, según el espacio disponible, un panel con
 * la lista de grupos (ancho) o grupos plegables (angosto, p. ej. el celular o dentro de un salón).
 */
@Component({
  selector: 'app-caracteristicas-editor',
  // La altura de la cabecera fija se usa para ubicar el panel de grupos y para que el foco no quede tapado.
  host: { '[style.--alto-cabecera.px]': 'altoCabecera()', '(focusin)': 'noTaparFoco($event)' },
  imports: [MatButtonModule, MatIconModule, MatProgressBarModule, MatSlideToggleModule],
  template: `
    @if (cargando()) {
      <mat-progress-bar mode="indeterminate" aria-label="Cargando características" />
    } @else if (errorAlCargar()) {
      <div class="estado" role="alert">
        <p>No se pudieron cargar las características.</p>
        <button mat-stroked-button type="button" (click)="cargar()">Reintentar</button>
      </div>
    } @else if (!opciones().length) {
      <p class="estado">El catálogo no tiene características. Se cargan en Catálogos › Características.</p>
    } @else {
      <div class="cabecera" #cabecera>
        <div class="fila-cabecera">
          <label class="buscador">
            <mat-icon aria-hidden="true">search</mat-icon>
            <span class="oculto">Buscar características</span>
            <input type="search" placeholder="Buscar: rampa, wifi, braille…" [value]="busqueda()"
                   (input)="busqueda.set($any($event.target).value)" (keydown.escape)="busqueda.set('')" />
            @if (busqueda()) {
              <button mat-icon-button type="button" class="limpiar" aria-label="Borrar búsqueda" (click)="busqueda.set('')">
                <mat-icon>close</mat-icon>
              </button>
            }
          </label>
          <mat-slide-toggle [checked]="soloSeleccionadas()" (change)="soloSeleccionadas.set($event.checked)">
            Ver solo seleccionadas
          </mat-slide-toggle>
          <span class="espacio"></span>
          <span class="contador" aria-live="polite">{{ resumenEstado() }}</span>
          @if (editable()) {
            <button mat-flat-button type="button" (click)="guardar()" [disabled]="!cambios() || guardando()">
              {{ guardando() ? 'Guardando…' : 'Guardar características' }}
            </button>
          }
        </div>
      </div>

      <div class="cuerpo" [class.con-panel]="modoPanel() && !filtrando()">
        @if (modoPanel() && !filtrando()) {
          <nav class="grupos" aria-label="Grupos de características">
            <ul>
              @for (g of grupos(); track g.nombre) {
                <li>
                  <button type="button" [class.activo]="g.nombre === grupoActivo()" [attr.aria-current]="g.nombre === grupoActivo() ? 'true' : null"
                          (click)="grupoActivo.set(g.nombre)">
                    <span class="nombre-grupo">{{ g.nombre }}</span>
                    <span class="cuenta" [class.con-marcadas]="g.marcadas">{{ g.marcadas }} de {{ g.total }}</span>
                  </button>
                </li>
              }
            </ul>
          </nav>
        }

        <div class="opciones">
          @for (g of gruposMostrados(); track g.nombre) {
            <fieldset class="grupo" [disabled]="!editable()">
              <legend>
                @if (modoPanel()) {
                  <span class="titulo-grupo">{{ g.nombre }} <span class="cuenta-legend">({{ g.marcadas }} de {{ g.total }})</span></span>
                } @else {
                  <button type="button" class="titulo-grupo plegable" [attr.aria-expanded]="abierto(g.nombre)" (click)="alternar(g.nombre)">
                    <mat-icon aria-hidden="true">{{ abierto(g.nombre) ? 'expand_less' : 'expand_more' }}</mat-icon>
                    {{ g.nombre }} <span class="cuenta-legend">({{ g.marcadas }} de {{ g.total }})</span>
                  </button>
                }
              </legend>
              @if (modoPanel() || abierto(g.nombre)) {
                <div class="lista">
                  @for (c of g.visibles; track c.id) {
                    <div class="opcion" [class.marcada]="marcada(c.id)">
                      <label class="fila">
                        <input type="checkbox" [checked]="marcada(c.id)" (change)="marcar(c, $any($event.target).checked)" />
                        <span class="texto">
                          {{ c.nombre }}
                          @if (c.esPublica === false) { <span class="interna" title="No se muestra en el sitio público">Interna</span> }
                        </span>
                      </label>
                      @if (c.admiteCantidad && marcada(c.id)) {
                        <input class="cantidad" type="number" min="0" inputmode="numeric" [attr.aria-label]="'Cantidad de ' + c.nombre"
                               [value]="sel()[c.id].cantidad ?? ''" (input)="cantidad(c, $any($event.target).value)" />
                      }
                    </div>
                  }
                </div>
              }
            </fieldset>
          } @empty {
            <div class="estado">
              @if (busqueda()) {
                <p>Ninguna característica coincide con "{{ busqueda() }}".</p>
                <button mat-stroked-button type="button" (click)="busqueda.set('')">Borrar búsqueda</button>
              } @else {
                <p>Todavía no hay características seleccionadas.</p>
                <button mat-stroked-button type="button" (click)="soloSeleccionadas.set(false)">Ver todas</button>
              }
            </div>
          }
        </div>
      </div>
    }
  `,
  styles: `
    :host { display: block; container-type: inline-size; }
    .oculto { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; }
    .estado { padding: 32px 16px; text-align: center; color: var(--sicat-texto-suave); }
    .estado p { margin: 0 0 12px; }

    /* Cabecera fija: búsqueda, filtro, contador, guardar y lo elegido */
    .cabecera { position: sticky; top: 0; z-index: 2; background: var(--mat-sys-surface); border-bottom: 1px solid var(--sicat-borde); padding: 12px 16px; }
    .fila-cabecera { display: flex; align-items: center; gap: 12px 16px; flex-wrap: wrap; }
    .espacio { flex: 1; }
    .buscador { display: flex; align-items: center; gap: 8px; flex: 1 1 280px; max-width: 420px; min-height: 44px; padding: 0 4px 0 12px;
                border: 1px solid var(--sicat-borde); border-radius: 8px; background: var(--mat-sys-surface); }
    .buscador:focus-within { outline: 2px solid var(--sicat-rio); outline-offset: 1px; }
    .buscador mat-icon { color: var(--sicat-texto-suave); }
    .buscador input { flex: 1; min-width: 0; border: 0; outline: 0; font: inherit; font-size: 16px; background: transparent; color: var(--sicat-tinta); }
    .buscador input::-webkit-search-cancel-button { display: none; }
    .contador { font-size: 14px; color: var(--sicat-texto-suave); font-variant-numeric: tabular-nums; }

    /* Panel de grupos (ancho) */
    .cuerpo.con-panel { display: grid; grid-template-columns: 260px minmax(0, 1fr); align-items: start; }
    .grupos { position: sticky; top: var(--alto-cabecera, 0px); max-height: calc(100vh - 140px - var(--alto-cabecera, 0px)); overflow-y: auto; border-right: 1px solid var(--sicat-borde); padding: 8px; }
    .grupos ul { list-style: none; margin: 0; padding: 0; }
    .grupos button { display: flex; width: 100%; align-items: center; justify-content: space-between; gap: 8px; min-height: 44px; padding: 6px 10px;
                     border: 0; border-radius: 6px; background: transparent; font: inherit; font-size: 14px; color: var(--sicat-tinta); text-align: left; cursor: pointer; }
    .grupos button:hover { background: var(--mat-sys-surface-container-low); }
    .grupos button.activo { background: var(--sicat-rio-claro); color: var(--sicat-rio); font-weight: 700; box-shadow: inset 3px 0 var(--sicat-rio); }
    .cuenta { flex: none; font-size: 12px; color: var(--sicat-texto-suave); font-variant-numeric: tabular-nums; }
    .cuenta.con-marcadas { color: var(--sicat-rio); font-weight: 700; }

    /* Grupos y opciones */
    .opciones { padding: 8px 16px 16px; min-width: 0; }
    .grupo { border: 0; margin: 0; padding: 12px 0 8px; min-width: 0; }
    .grupo + .grupo { border-top: 1px solid var(--sicat-borde); }
    legend { padding: 0; width: 100%; }
    .titulo-grupo { display: block; font-size: 17px; font-weight: 700; font-stretch: var(--sicat-ancho-nombre); color: var(--sicat-tinta); margin-bottom: 8px; }
    .cuenta-legend { font-weight: 400; font-stretch: 100%; font-size: 14px; color: var(--sicat-texto-suave); }
    .plegable { display: flex; align-items: center; gap: 6px; width: 100%; min-height: 44px; margin: 0; padding: 0; border: 0; background: transparent; text-align: left; cursor: pointer; }
    .lista { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 4px 12px; }
    .opcion { display: flex; align-items: flex-start; gap: 8px; border-radius: 6px; }
    .opcion.marcada { background: var(--sicat-rio-claro); }
    .fila { display: flex; align-items: flex-start; gap: 10px; flex: 1; min-height: 44px; padding: 10px 10px; box-sizing: border-box; cursor: pointer; border-radius: 6px; }
    .fila:hover { background: var(--mat-sys-surface-container-low); }
    .opcion.marcada .fila:hover { background: transparent; }
    .fila input { flex: none; width: 18px; height: 18px; margin: 1px 0 0; accent-color: var(--sicat-rio); cursor: pointer; }
    .texto { line-height: 1.35; overflow-wrap: anywhere; }
    .opcion.marcada .texto { font-weight: 600; color: var(--sicat-tinta); }
    .interna { margin-left: 6px; padding: 0 6px; border-radius: 4px; background: #f6ecd8; color: var(--sicat-alerta); font-size: 12px; font-weight: 600; white-space: nowrap; }
    .cantidad { width: 68px; margin: 6px 8px 6px 0; padding: 6px 8px; border: 1px solid var(--sicat-borde); border-radius: 6px; font: inherit; }
    fieldset:disabled .fila { cursor: default; }
    .fila, .plegable, .grupos button { scroll-margin-top: calc(var(--alto-cabecera, 0px) + 12px); }

    @container (max-width: 520px) {
      .lista { grid-template-columns: 1fr; }
      /* Cabecera compacta: que lo fijo no tape media pantalla del celular */
      .cabecera { padding: 8px 12px; }
      .fila-cabecera { gap: 6px 12px; }
      .buscador { flex-basis: 100%; max-width: none; }
      .espacio { display: none; }
      .fila-cabecera button[mat-flat-button] { margin-left: auto; }
      .opciones { padding: 4px 12px 12px; }
    }
  `,
})
export class CaracteristicasEditor implements OnInit {
  private readonly avisoDeCambios = avisarSiHayCambios('Características', () => this.cambios());
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly catalogos = inject(Catalogos);

  /** p. ej. /establecimientos/5/caracteristicas o /salones/2/caracteristicas */
  readonly ruta = input.required<string>();
  readonly editable = input(true);

  protected readonly cargando = signal(true);
  protected readonly errorAlCargar = signal(false);
  protected readonly opciones = signal<Opcion[]>([]);
  protected readonly sel = signal<Record<number, Seleccion>>({});
  /** Lo guardado, para saber si hay cambios de verdad (tildar y destildar no cuenta). */
  private readonly guardado = signal('');
  protected readonly cambios = computed(() => this.firma(this.sel()) !== this.guardado());
  protected readonly guardando = signal(false);
  protected readonly busqueda = signal('');
  protected readonly soloSeleccionadas = signal(false);
  protected readonly grupoActivo = signal<string | null>(null);
  private readonly abiertos = signal<Set<string>>(new Set());
  private readonly ancho = signal(0);
  private readonly cabecera = viewChild<ElementRef<HTMLElement>>('cabecera');
  protected readonly altoCabecera = signal(0);

  protected readonly modoPanel = computed(() => this.ancho() >= ANCHO_PANEL);
  protected readonly filtrando = computed(() => !!normalizar(this.busqueda()) || this.soloSeleccionadas());

  protected readonly grupos = computed<Grupo[]>(() => {
    const q = normalizar(this.busqueda());
    const solo = this.soloSeleccionadas();
    const sel = this.sel();
    const mapa = new Map<string, Opcion[]>();
    for (const c of this.opciones()) {
      const g = c.grupo ?? 'Otras';
      mapa.set(g, [...(mapa.get(g) ?? []), c]);
    }
    return [...mapa.entries()]
      .sort(([a], [b]) => a.localeCompare(b, 'es'))
      .map(([nombre, items]) => {
        const qGrupo = normalizar(nombre);
        return {
          nombre,
          total: items.length,
          marcadas: items.filter((c) => sel[c.id]?.marcada).length,
          visibles: items.filter(
            (c) => (!q || c.clave.includes(q) || qGrupo.includes(q)) && (!solo || sel[c.id]?.marcada),
          ),
        };
      });
  });

  /** Lo que se dibuja: el grupo elegido en el panel, o todos los que tienen algo para mostrar. */
  protected readonly gruposMostrados = computed(() => {
    const visibles = this.grupos().filter((g) => g.visibles.length);
    if (this.modoPanel() && !this.filtrando()) {
      const activo = this.grupoActivo() ?? visibles[0]?.nombre;
      return visibles.filter((g) => g.nombre === activo);
    }
    return visibles;
  });

  protected readonly elegidas = computed(() => {
    const sel = this.sel();
    return this.opciones()
      .filter((c) => sel[c.id]?.marcada)
      .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
  });

  protected readonly resumenEstado = computed(() => {
    const n = this.elegidas().length;
    const base = n === 1 ? '1 seleccionada' : `${n} seleccionadas`;
    if (!normalizar(this.busqueda())) {
      return base;
    }
    const r = this.grupos().reduce((t, g) => t + g.visibles.length, 0);
    return `${base} · ${r === 1 ? '1 resultado' : r + ' resultados'}`;
  });

  constructor() {
    // El modo (panel o plegables) depende del espacio del componente, no de la ventana: dentro de un salón es angosto.
    const host = inject(ElementRef<HTMLElement>).nativeElement;
    const observador = new ResizeObserver(([e]) => this.ancho.set(e.contentRect.width));
    observador.observe(host);
    inject(DestroyRef).onDestroy(() => observador.disconnect());

    effect((alLimpiar) => {
      const el = this.cabecera()?.nativeElement;
      if (!el) {
        return;
      }
      const alto = new ResizeObserver(() => this.altoCabecera.set(el.offsetHeight));
      alto.observe(el);
      alLimpiar(() => alto.disconnect());
    });
  }

  ngOnInit(): void {
    this.cargar();
  }

  /**
   * WCAG 2.4.11: si el foco cae en algo que queda debajo de la cabecera fija, se lo trae a la vista. El navegador
   * no lo hace solo porque considera visible lo que está tapado por un elemento sticky.
   */
  protected noTaparFoco(e: FocusEvent): void {
    const objetivo = e.target as HTMLElement | null;
    const cabecera = this.cabecera()?.nativeElement;
    if (!objetivo || !cabecera || cabecera.contains(objetivo)) {
      return;
    }
    if (objetivo.getBoundingClientRect().top < cabecera.getBoundingClientRect().bottom + 4) {
      objetivo.scrollIntoView({ block: 'center' });
    }
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.errorAlCargar.set(false);
    forkJoin([this.catalogos.de('caracteristica'), this.api.get<CaracteristicaAsignada[]>(this.ruta())]).subscribe({
      next: ([catalogo, asignadas]) => {
        this.opciones.set(
          catalogo
            .map((c) => ({ ...c, clave: normalizar(c.nombre) }))
            .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es')),
        );
        this.aplicar(asignadas);
        // Arrancan abiertos los grupos con algo elegido; en el panel, se muestra el primero de ellos.
        const conSeleccion = this.grupos().filter((g) => g.marcadas).map((g) => g.nombre);
        this.abiertos.set(new Set(conSeleccion));
        this.grupoActivo.set(conSeleccion[0] ?? this.grupos()[0]?.nombre ?? null);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.errorAlCargar.set(true);
      },
    });
  }

  private aplicar(l: CaracteristicaAsignada[]): void {
    const sel = Object.fromEntries(l.map((c) => [c.caracteristicaId, { marcada: true, cantidad: c.cantidad }]));
    this.sel.set(sel);
    this.guardado.set(this.firma(sel));
  }

  /** Ids marcados con su cantidad, en orden: dos selecciones iguales dan la misma firma. */
  private firma(sel: Record<number, Seleccion>): string {
    return Object.entries(sel)
      .filter(([, s]) => s.marcada)
      .map(([id, s]) => `${id}:${s.cantidad ?? ''}`)
      .sort()
      .join(',');
  }

  protected marcada(id: number): boolean {
    return this.sel()[id]?.marcada ?? false;
  }

  protected abierto(grupo: string): boolean {
    return this.filtrando() || this.abiertos().has(grupo);
  }

  protected alternar(grupo: string): void {
    this.abiertos.update((s) => {
      const n = new Set(s);
      if (n.has(grupo)) {
        n.delete(grupo);
      } else {
        n.add(grupo);
      }
      return n;
    });
  }

  protected marcar(c: CatalogoItem, marcada: boolean): void {
    if (!this.editable()) {
      return;
    }
    this.sel.update((s) => ({ ...s, [c.id]: { marcada, cantidad: s[c.id]?.cantidad ?? null } }));
  }

  protected cantidad(c: CatalogoItem, v: string): void {
    this.sel.update((s) => ({ ...s, [c.id]: { marcada: true, cantidad: v === '' ? null : Number(v) } }));
  }

  protected guardar(): void {
    const admite = new Set(this.opciones().filter((c) => c.admiteCantidad).map((c) => c.id));
    const cuerpo = Object.entries(this.sel())
      .filter(([, s]) => s.marcada)
      .map(([id, s]) => ({ caracteristicaId: Number(id), cantidad: admite.has(Number(id)) ? s.cantidad : null }));
    this.guardando.set(true);
    this.api.put<CaracteristicaAsignada[]>(this.ruta(), cuerpo).subscribe({
      next: (l) => {
        this.guardando.set(false);
        this.aplicar(l);
        this.avisos.ok('Características guardadas');
      },
      error: (e) => {
        this.guardando.set(false);
        this.avisos.error(e);
      },
    });
  }
}
