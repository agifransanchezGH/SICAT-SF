import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { normalizar, similitud } from 'shared';

import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { Catalogos } from '../core/catalogos';
import { Confirmar } from '../core/confirmar';
import { HistorialNavegacion } from '../core/navegacion';

interface Uso {
  id: number;
  grupo: string;
  nombre: string;
  admiteCantidad: boolean;
  esPublica: boolean;
  activo: boolean;
  establecimientos: number;
  salones: number;
  categorias: number;
}

interface Resultado {
  resultantes: Uso[];
  establecimientos: number;
  salones: number;
  categorias: number;
  eliminadas: number;
}

interface Item extends Uso {
  clave: string;
  /** Índice del conjunto de posibles duplicados (null si no tiene parecidas). */
  conjunto: number | null;
  direccion: boolean;
}

/** Misma heurística que la importación (Texto.pareceDireccion). */
const DIRECCION = /\b(calle|av|avda|avenida|bv|boulevard|bulevar|ruta|esquina|esq)\b\.?|[a-záéíóúñ]{3,}\.?\s+n?[°º]?\s*\d{3,5}\b/i;

/** Parecidas: mismo texto normalizado con pocos cambios, o una es la otra con algo agregado al final. */
function parecidas(a: string, b: string): boolean {
  return similitud(a, b) >= 0.85 || (a.length >= 6 && b.startsWith(a + ' ')) || (b.length >= 6 && a.startsWith(b + ' '));
}

/**
 * Limpieza del catálogo de características (ADR 0017): fusionar duplicados, separar entradas pegadas y pasar a
 * notas internas lo que no es una característica. Solo ADMIN.
 */
@Component({
  selector: 'app-limpiar-caracteristicas-page',
  imports: [MatButtonModule, MatIconModule, MatProgressBarModule, MatSlideToggleModule, MatTooltipModule],
  template: `
    <section class="pagina">
      <div class="encabezado">
        <button mat-icon-button type="button" aria-label="Volver" (click)="irAtras()"><mat-icon>arrow_back</mat-icon></button>
        <h1>Limpiar características</h1>
      </div>
      <p class="suave intro">
        Elegí un grupo, tildá las que son lo mismo y fusionalas: los establecimientos pasan a la que queda, sin duplicarse.
        Las sugerencias son solo eso: decidí vos si "Rampas" y "Rampas con asistencia" son lo mismo.
      </p>

      @if (cargando()) { <mat-progress-bar mode="indeterminate" aria-label="Cargando" /> }
      @if (errorAlCargar()) {
        <div class="tarjeta estado" role="alert">
          <p>No se pudo cargar el catálogo.</p>
          <button mat-stroked-button type="button" (click)="cargar()">Reintentar</button>
        </div>
      }

      @if (items().length) {
        <div class="dos-columnas">
          <nav class="tarjeta grupos" aria-label="Grupos">
            <ul>
              @for (g of grupos(); track g.nombre) {
                <li>
                  <button type="button" [class.activo]="g.nombre === grupo()" [attr.aria-current]="g.nombre === grupo() ? 'true' : null"
                          (click)="elegirGrupo(g.nombre)">
                    <span>{{ g.nombre }}</span>
                    <span class="cuentas">
                      @if (g.sugeridas) { <span class="sugeridas" [matTooltip]="g.sugeridas + ' posibles duplicados'">{{ g.sugeridas }}</span> }
                      <span class="total">{{ g.total }}</span>
                    </span>
                  </button>
                </li>
              }
            </ul>
          </nav>

          <div class="tarjeta contenido">
            <div class="barra">
              <h2>{{ grupo() }}</h2>
              <mat-slide-toggle [checked]="soloSugeridas()" (change)="soloSugeridas.set($event.checked)">Solo posibles duplicados</mat-slide-toggle>
            </div>

            <ul class="lista" [attr.aria-label]="'Características de ' + grupo()">
              @for (c of visibles(); track c.id) {
                <li class="item" [class.elegida]="elegidas().has(c.id)" [class.inicio-conjunto]="c.conjunto !== null && esInicio(c)">
                  <label class="fila">
                    <input type="checkbox" [checked]="elegidas().has(c.id)" (change)="alternar(c.id)" />
                    <span class="nombre">
                      {{ c.nombre }}
                      @if (c.conjunto !== null) { <span class="marca dup">Parecida a otras</span> }
                      @if (c.direccion) { <span class="marca dir">¿Dirección?</span> }
                      @if (!c.activo) { <span class="marca">De baja</span> }
                    </span>
                    <span class="usos" [attr.aria-label]="usosTexto(c)">{{ usosTexto(c) }}</span>
                  </label>
                  <div class="acciones-item">
                    <button mat-icon-button type="button" [attr.aria-label]="'Separar ' + c.nombre" matTooltip="Separar en varias"
                            (click)="abrirSeparar(c)"><mat-icon>call_split</mat-icon></button>
                    <button mat-icon-button type="button" [attr.aria-label]="'Pasar a notas internas: ' + c.nombre"
                            matTooltip="Pasar a notas internas" [disabled]="c.salones > 0 || c.categorias > 0"
                            (click)="aNotas(c)"><mat-icon>sticky_note_2</mat-icon></button>
                  </div>
                  @if (separando()?.id === c.id) {
                    <div class="separar">
                      <label for="partes-{{ c.id }}">Una característica por línea</label>
                      <textarea id="partes-{{ c.id }}" rows="3" [value]="partes()" (input)="partes.set($any($event.target).value)"></textarea>
                      <div class="botones">
                        <button mat-button type="button" (click)="separando.set(null)">Cancelar</button>
                        <button mat-flat-button type="button" [disabled]="lineas().length < 2 || enviando()" (click)="separar(c)">
                          Separar en {{ lineas().length }}
                        </button>
                      </div>
                    </div>
                  }
                </li>
              } @empty {
                <li class="estado">{{ soloSugeridas() ? 'No hay posibles duplicados en este grupo.' : 'Este grupo no tiene características.' }}</li>
              }
            </ul>
          </div>
        </div>
      }

      @if (seleccion().length >= 2) {
        <section class="fusion tarjeta" aria-labelledby="titulo-fusion">
          <h2 id="titulo-fusion">Fusionar {{ seleccion().length }} características</h2>
          <fieldset>
            <legend>¿Cuál queda?</legend>
            @for (c of seleccion(); track c.id) {
              <label class="opcion-queda">
                <input type="radio" name="queda" [checked]="queda() === c.id" (change)="elegirQueda(c)" />
                {{ c.nombre }} <span class="suave">({{ usosTexto(c) }})</span>
              </label>
            }
          </fieldset>
          <label class="campo-nombre">
            <span>Nombre de la que queda</span>
            <input type="text" maxlength="200" [value]="nombreFinal()" (input)="nombreFinal.set($any($event.target).value)" />
          </label>
          <p class="impacto" aria-live="polite">{{ impacto() }}</p>
          <div class="botones">
            <button mat-button type="button" (click)="limpiarSeleccion()">Cancelar</button>
            <button mat-flat-button type="button" [disabled]="!queda() || !nombreFinal().trim() || enviando()" (click)="fusionar()">
              {{ enviando() ? 'Fusionando…' : 'Fusionar' }}
            </button>
          </div>
        </section>
      }
    </section>
  `,
  styles: `
    .encabezado h1 { margin-right: auto; }
    .intro { margin: -8px 0 0; max-width: 80ch; }
    .estado { padding: 24px; text-align: center; color: var(--sicat-texto-suave); list-style: none; }
    .dos-columnas { display: grid; grid-template-columns: 280px minmax(0, 1fr); gap: 16px; align-items: start; }
    .grupos { position: sticky; top: 0; max-height: calc(100vh - 160px); overflow-y: auto; padding: 8px; }
    .grupos ul, .lista { list-style: none; margin: 0; padding: 0; }
    .grupos button { display: flex; width: 100%; justify-content: space-between; align-items: center; gap: 8px; min-height: 44px; padding: 6px 10px;
                     border: 0; border-radius: 6px; background: transparent; font: inherit; font-size: 14px; text-align: left; cursor: pointer; color: var(--sicat-tinta); }
    .grupos button:hover { background: var(--mat-sys-surface-container-low); }
    .grupos button.activo { background: var(--sicat-rio-claro); color: var(--sicat-rio); font-weight: 700; box-shadow: inset 3px 0 var(--sicat-rio); }
    .cuentas { display: inline-flex; gap: 6px; align-items: center; flex: none; }
    .total { font-size: 12px; color: var(--sicat-texto-suave); }
    .sugeridas { min-width: 20px; padding: 0 6px; border-radius: 999px; background: #f6ecd8; color: var(--sicat-alerta); font-size: 12px; font-weight: 700; text-align: center; }
    .contenido { padding: 0 0 8px; }
    .barra { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; padding: 12px 16px; border-bottom: 1px solid var(--sicat-borde); }
    .barra h2 { margin: 0 auto 0 0; font-size: 19px; font-weight: 700; font-stretch: var(--sicat-ancho-nombre); }
    .item { display: flex; flex-wrap: wrap; align-items: center; border-bottom: 1px solid var(--sicat-borde); }
    .item.inicio-conjunto { border-top: 2px solid var(--sicat-limo); }
    .item.elegida { background: var(--sicat-rio-claro); }
    .fila { flex: 1; display: flex; align-items: flex-start; gap: 10px; min-height: 44px; padding: 10px 8px 10px 16px; box-sizing: border-box; cursor: pointer; min-width: 0; }
    .fila input { width: 18px; height: 18px; margin: 1px 0 0; accent-color: var(--sicat-rio); flex: none; }
    .nombre { flex: 1; overflow-wrap: anywhere; line-height: 1.35; }
    .item.elegida .nombre { font-weight: 600; }
    .usos { flex: none; font-size: 13px; color: var(--sicat-texto-suave); font-variant-numeric: tabular-nums; }
    .marca { margin-left: 6px; padding: 0 6px; border-radius: 4px; background: #e8eeed; color: #3a4b50; font-size: 12px; font-weight: 600; white-space: nowrap; }
    .marca.dup { background: #f6ecd8; color: var(--sicat-alerta); }
    .marca.dir { background: #f8e6e4; color: var(--sicat-error); }
    .acciones-item { display: flex; padding-right: 8px; }
    .separar { flex-basis: 100%; padding: 0 16px 12px 44px; display: flex; flex-direction: column; gap: 6px; }
    .separar textarea { font: inherit; padding: 8px; border: 1px solid var(--sicat-borde); border-radius: 6px; resize: vertical; }
    .botones { display: flex; gap: 8px; justify-content: flex-end; }
    .fusion { position: sticky; bottom: 12px; z-index: 3; padding: 16px 20px; box-shadow: 0 8px 28px rgb(22 38 42 / 22%); display: flex; flex-direction: column; gap: 10px; }
    .fusion h2 { margin: 0; font-size: 18px; font-weight: 700; font-stretch: var(--sicat-ancho-nombre); }
    .fusion fieldset { border: 0; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 2px; max-height: 160px; overflow-y: auto; }
    .fusion legend { font-weight: 600; margin-bottom: 4px; padding: 0; }
    .opcion-queda { display: flex; align-items: center; gap: 8px; min-height: 36px; cursor: pointer; }
    .opcion-queda input { accent-color: var(--sicat-rio); width: 18px; height: 18px; }
    .campo-nombre { display: flex; flex-direction: column; gap: 4px; font-weight: 600; }
    .campo-nombre input { font: inherit; font-weight: 400; padding: 10px; border: 1px solid var(--sicat-borde); border-radius: 6px; }
    .impacto { margin: 0; color: var(--sicat-tinta); }
    @media (max-width: 860px) { .dos-columnas { grid-template-columns: 1fr; } .grupos { position: static; max-height: 240px; } }
  `,
})
export class LimpiarCaracteristicasPage {
  private readonly historial = inject(HistorialNavegacion);

  /** A la página de la que se vino; si se entró directo, a /catalogos. */
  protected irAtras(): void {
    this.historial.volver('/catalogos');
  }

  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly confirmar = inject(Confirmar);
  private readonly catalogos = inject(Catalogos);

  protected readonly cargando = signal(true);
  protected readonly errorAlCargar = signal(false);
  protected readonly enviando = signal(false);
  private readonly usos = signal<Uso[]>([]);
  protected readonly grupo = signal<string | null>(null);
  protected readonly soloSugeridas = signal(false);
  protected readonly elegidas = signal<Set<number>>(new Set());
  protected readonly queda = signal<number | null>(null);
  protected readonly nombreFinal = signal('');
  protected readonly separando = signal<Item | null>(null);
  protected readonly partes = signal('');

  /** Con los conjuntos de posibles duplicados calculados dentro de cada grupo. */
  protected readonly items = computed<Item[]>(() => {
    const porGrupo = new Map<string, Item[]>();
    for (const u of this.usos()) {
      const it: Item = { ...u, clave: normalizar(u.nombre), conjunto: null, direccion: DIRECCION.test(u.nombre) };
      porGrupo.set(u.grupo, [...(porGrupo.get(u.grupo) ?? []), it]);
    }
    let siguiente = 0;
    for (const lista of porGrupo.values()) {
      const padre = lista.map((_, i) => i);
      const raiz = (i: number): number => (padre[i] === i ? i : (padre[i] = raiz(padre[i])));
      for (let i = 0; i < lista.length; i++) {
        for (let j = i + 1; j < lista.length; j++) {
          if (parecidas(lista[i].clave, lista[j].clave)) {
            padre[raiz(i)] = raiz(j);
          }
        }
      }
      const tamanio = new Map<number, number>();
      lista.forEach((_, i) => tamanio.set(raiz(i), (tamanio.get(raiz(i)) ?? 0) + 1));
      const numero = new Map<number, number>();
      lista.forEach((it, i) => {
        if ((tamanio.get(raiz(i)) ?? 0) > 1) {
          if (!numero.has(raiz(i))) {
            numero.set(raiz(i), siguiente++);
          }
          it.conjunto = numero.get(raiz(i))!;
        }
      });
    }
    return [...porGrupo.values()].flat();
  });

  protected readonly grupos = computed(() => {
    const m = new Map<string, { nombre: string; total: number; conjuntos: Set<number> }>();
    for (const it of this.items()) {
      const g = m.get(it.grupo) ?? { nombre: it.grupo, total: 0, conjuntos: new Set<number>() };
      g.total++;
      if (it.conjunto !== null) {
        g.conjuntos.add(it.conjunto);
      }
      m.set(it.grupo, g);
    }
    return [...m.values()]
      .map((g) => ({ nombre: g.nombre, total: g.total, sugeridas: g.conjuntos.size }))
      .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es'));
  });

  /** Del grupo elegido: primero los conjuntos de parecidas (juntas), después el resto, todo alfabético. */
  protected readonly visibles = computed(() => {
    const delGrupo = this.items().filter((i) => i.grupo === this.grupo() && (!this.soloSugeridas() || i.conjunto !== null));
    const orden = (a: Item, b: Item) => a.nombre.localeCompare(b.nombre, 'es');
    const primero = new Map<number, string>();
    for (const i of [...delGrupo].sort(orden)) {
      if (i.conjunto !== null && !primero.has(i.conjunto)) {
        primero.set(i.conjunto, i.nombre);
      }
    }
    return delGrupo.sort((a, b) => {
      const ka = a.conjunto !== null ? '0' + primero.get(a.conjunto) : '1' + a.nombre;
      const kb = b.conjunto !== null ? '0' + primero.get(b.conjunto) : '1' + b.nombre;
      return ka.localeCompare(kb, 'es') || orden(a, b);
    });
  });

  protected readonly seleccion = computed(() => this.items().filter((i) => this.elegidas().has(i.id)));
  protected readonly lineas = computed(() => this.partes().split('\n').map((l) => l.trim()).filter(Boolean));

  protected readonly impacto = computed(() => {
    const queda = this.queda();
    const otras = this.seleccion().filter((c) => c.id !== queda);
    const est = otras.reduce((t, c) => t + c.establecimientos, 0);
    const extra = otras.reduce((t, c) => t + c.salones + c.categorias, 0);
    return `Se pasan a "${this.nombreFinal().trim() || '…'}" los usos de ${otras.length === 1 ? '1 característica' : otras.length + ' características'}`
      + ` (${est === 1 ? '1 establecimiento' : est + ' establecimientos'}${extra ? `, ${extra} salones o categorías` : ''})`
      + ` y ${otras.length === 1 ? 'se borra' : 'se borran'}. Los que ya tenían la que queda no se duplican.`;
  });

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.errorAlCargar.set(false);
    this.api.get<Uso[]>('/caracteristicas/uso').subscribe({
      next: (u) => {
        this.usos.set(u);
        this.cargando.set(false);
        if (!this.grupo() || !u.some((x) => x.grupo === this.grupo())) {
          this.grupo.set(this.grupos().find((g) => g.sugeridas)?.nombre ?? this.grupos()[0]?.nombre ?? null);
        }
      },
      error: () => {
        this.cargando.set(false);
        this.errorAlCargar.set(true);
      },
    });
  }

  protected elegirGrupo(g: string): void {
    this.grupo.set(g);
    this.limpiarSeleccion();
    this.separando.set(null);
  }

  protected esInicio(c: Item): boolean {
    const v = this.visibles();
    const i = v.indexOf(c);
    return i > 0 && v[i - 1].conjunto !== c.conjunto;
  }

  protected usosTexto(c: Uso): string {
    const partes = [`${c.establecimientos} ${c.establecimientos === 1 ? 'establecimiento' : 'establecimientos'}`];
    if (c.salones) partes.push(`${c.salones} ${c.salones === 1 ? 'salón' : 'salones'}`);
    if (c.categorias) partes.push(`${c.categorias} ${c.categorias === 1 ? 'categoría' : 'categorías'}`);
    return partes.join(', ');
  }

  protected alternar(id: number): void {
    this.elegidas.update((s) => {
      const n = new Set(s);
      n.has(id) ? n.delete(id) : n.add(id);
      return n;
    });
    // Por defecto queda la más usada; su nombre se puede editar.
    const sel = this.seleccion();
    if (!sel.some((c) => c.id === this.queda())) {
      const masUsada = [...sel].sort((a, b) => b.establecimientos + b.salones - (a.establecimientos + a.salones))[0];
      if (masUsada) {
        this.elegirQueda(masUsada);
      } else {
        this.queda.set(null);
      }
    }
  }

  protected elegirQueda(c: Item): void {
    this.queda.set(c.id);
    this.nombreFinal.set(c.nombre);
  }

  protected limpiarSeleccion(): void {
    this.elegidas.set(new Set());
    this.queda.set(null);
    this.nombreFinal.set('');
  }

  protected fusionar(): void {
    const conservarId = this.queda()!;
    const absorberIds = this.seleccion().map((c) => c.id).filter((id) => id !== conservarId);
    const nombre = this.nombreFinal().trim();
    this.confirmar.pedir('Fusionar características', this.impacto(), 'Fusionar').subscribe((ok) => {
      if (!ok) {
        return;
      }
      this.enviando.set(true);
      this.api.post<Resultado>('/caracteristicas/fusiones', { conservarId, absorberIds, nombre }).subscribe({
        next: (r) => {
          this.avisos.ok(`Fusionadas: queda "${r.resultantes[0]?.nombre}" (${r.eliminadas} borradas)`);
          this.terminar();
        },
        error: (e) => this.fallo(e),
      });
    });
  }

  protected abrirSeparar(c: Item): void {
    this.separando.set(c);
    // Propuesta: cortar donde la importación pegó dos valores (".", ":", "/" o ";").
    this.partes.set(c.nombre.split(/\s*[.:;/]\s*(?=\p{Lu}|\p{Ll})/u).map((p) => p.trim()).filter(Boolean).join('\n'));
  }

  protected separar(c: Item): void {
    this.enviando.set(true);
    this.api.post<Resultado>(`/caracteristicas/${c.id}/separacion`, { nombres: this.lineas() }).subscribe({
      next: (r) => {
        this.avisos.ok(`Separada en ${r.resultantes.length}: ${r.resultantes.map((x) => x.nombre).join(', ')}`);
        this.separando.set(null);
        this.terminar();
      },
      error: (e) => this.fallo(e),
    });
  }

  protected aNotas(c: Item): void {
    const mensaje = `"${c.nombre}" se agrega a las notas internas de ${c.establecimientos === 1 ? '1 establecimiento' : c.establecimientos + ' establecimientos'}`
      + ' (no se publican) y se borra del catálogo.';
    this.confirmar.pedir('Pasar a notas internas', mensaje, 'Pasar a notas').subscribe((ok) => {
      if (!ok) {
        return;
      }
      this.enviando.set(true);
      this.api.post<Resultado>(`/caracteristicas/${c.id}/a-notas-internas`, {}).subscribe({
        next: (r) => {
          this.avisos.ok(`Pasada a notas internas de ${r.establecimientos} establecimientos`);
          this.terminar();
        },
        error: (e) => this.fallo(e),
      });
    });
  }

  private terminar(): void {
    this.enviando.set(false);
    this.limpiarSeleccion();
    this.catalogos.invalidar('caracteristica');
    this.cargar();
  }

  private fallo(e: unknown): void {
    this.enviando.set(false);
    this.avisos.error(e);
  }
}
