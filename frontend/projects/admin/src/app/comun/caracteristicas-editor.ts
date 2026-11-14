import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { Catalogos } from '../core/catalogos';
import { CaracteristicaAsignada, CatalogoItem } from '../core/modelos';

interface Seleccion {
  marcada: boolean;
  cantidad: number | null;
}

/**
 * Características (servicios, accesibilidad, equipamiento…) agrupadas como en el catálogo.
 * Se tildan las que tiene; las que admiten cantidad piden el número.
 */
@Component({
  selector: 'app-caracteristicas-editor',
  imports: [FormsModule, MatCheckboxModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <div class="editor">
      <mat-form-field class="filtro">
        <mat-label>Filtrar</mat-label>
        <input matInput [(ngModel)]="filtro" />
      </mat-form-field>
      @for (g of grupos(); track g.grupo) {
        <h3 class="grupo">{{ g.grupo }}</h3>
        <div class="items">
          @for (c of g.items; track c.id) {
            <div class="item">
              <mat-checkbox [disabled]="!editable()" [checked]="sel()[c.id]?.marcada ?? false" (change)="marcar(c, $event.checked)">
                {{ c.nombre }} @if (c.esPublica === false) { <small class="interna" title="No se muestra en el sitio público">interna</small> }
              </mat-checkbox>
              @if (c.admiteCantidad && sel()[c.id]?.marcada) {
                <input class="cantidad" type="number" min="0" [disabled]="!editable()" aria-label="Cantidad"
                       [value]="sel()[c.id].cantidad ?? ''" (input)="cantidad(c, $any($event.target).value)" />
              }
            </div>
          }
        </div>
      } @empty {
        <p class="vacio">No hay características en el catálogo.</p>
      }
      @if (editable()) {
        <div class="acciones-editor">
          <span class="suave">{{ marcadas() }} seleccionadas</span>
          <span class="espacio"></span>
          <button mat-flat-button type="button" (click)="guardar()" [disabled]="!cambios() || guardando()">Guardar características</button>
        </div>
      }
    </div>
  `,
  styleUrl: './editores.scss',
  styles: `
    .filtro { max-width: 320px; }
    .items { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 0 12px; }
    .item { display: flex; align-items: center; gap: 4px; }
    .interna { color: var(--sicat-alerta); margin-left: 4px; }
    .cantidad { width: 64px; padding: 4px 6px; border: 1px solid var(--sicat-borde); border-radius: 6px; font: inherit; }
  `,
})
export class CaracteristicasEditor implements OnInit {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);

  /** p. ej. /establecimientos/5/caracteristicas o /salones/2/caracteristicas */
  readonly ruta = input.required<string>();
  readonly editable = input(true);

  private readonly catalogo = toSignal(inject(Catalogos).de('caracteristica'), { initialValue: [] as CatalogoItem[] });
  protected readonly sel = signal<Record<number, Seleccion>>({});
  protected readonly cambios = signal(false);
  protected readonly guardando = signal(false);
  protected filtroValor = signal('');

  get filtro(): string {
    return this.filtroValor();
  }
  set filtro(v: string) {
    this.filtroValor.set(v);
  }

  protected readonly grupos = computed(() => {
    const f = this.filtroValor().trim().toLowerCase();
    const mapa = new Map<string, CatalogoItem[]>();
    for (const c of this.catalogo()) {
      if (f && !`${c.grupo} ${c.nombre}`.toLowerCase().includes(f)) {
        continue;
      }
      const g = c.grupo ?? 'Otras';
      mapa.set(g, [...(mapa.get(g) ?? []), c]);
    }
    return [...mapa.entries()].sort(([a], [b]) => a.localeCompare(b)).map(([grupo, items]) => ({ grupo, items }));
  });

  protected readonly marcadas = computed(() => Object.values(this.sel()).filter((s) => s.marcada).length);

  ngOnInit(): void {
    this.api.get<CaracteristicaAsignada[]>(this.ruta()).subscribe({ next: (l) => this.cargar(l), error: (e) => this.avisos.error(e) });
  }

  private cargar(l: CaracteristicaAsignada[]): void {
    this.sel.set(Object.fromEntries(l.map((c) => [c.caracteristicaId, { marcada: true, cantidad: c.cantidad }])));
    this.cambios.set(false);
  }

  protected marcar(c: CatalogoItem, marcada: boolean): void {
    this.sel.update((s) => ({ ...s, [c.id]: { marcada, cantidad: s[c.id]?.cantidad ?? null } }));
    this.cambios.set(true);
  }

  protected cantidad(c: CatalogoItem, v: string): void {
    this.sel.update((s) => ({ ...s, [c.id]: { marcada: true, cantidad: v === '' ? null : Number(v) } }));
    this.cambios.set(true);
  }

  protected guardar(): void {
    const admite = new Set(this.catalogo().filter((c) => c.admiteCantidad).map((c) => c.id));
    const cuerpo = Object.entries(this.sel())
      .filter(([, s]) => s.marcada)
      .map(([id, s]) => ({ caracteristicaId: Number(id), cantidad: admite.has(Number(id)) ? s.cantidad : null }));
    this.guardando.set(true);
    this.api.put<CaracteristicaAsignada[]>(this.ruta(), cuerpo).subscribe({
      next: (l) => {
        this.guardando.set(false);
        this.cargar(l);
        this.avisos.ok('Características guardadas');
      },
      error: (e) => {
        this.guardando.set(false);
        this.avisos.error(e);
      },
    });
  }
}
