import { NgTemplateOutlet } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';

import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { Avisos } from '../core/avisos';
import { Catalogos } from '../core/catalogos';
import { Confirmar } from '../core/confirmar';
import { EstadoChip } from '../core/estado';
import { CategoriaNodo } from '../core/modelos';

/** Árbol de categorías (p. ej. Gastronomía › Restaurante). Solo ADMIN modifica. */
@Component({
  selector: 'app-categorias-page',
  imports: [FormsModule, NgTemplateOutlet, EstadoChip, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatCheckboxModule, MatTooltipModule],
  template: `
    <section class="pagina">
      <div class="encabezado">
        <h1>Categorías</h1>
        <mat-checkbox [(ngModel)]="incluirInactivas" (change)="cargar()">Ver dadas de baja</mat-checkbox>
      </div>
      <div class="tarjeta">
        <div class="barra">
          @if (esAdmin) {
            <form class="nueva-raiz" (ngSubmit)="crear(null)">
              <mat-form-field>
                <mat-label>Nueva categoría principal</mat-label>
                <input matInput name="nombreRaiz" [(ngModel)]="nombreRaiz" />
              </mat-form-field>
              <button mat-flat-button type="submit" [disabled]="!nombreRaiz.trim()"><mat-icon>add</mat-icon> Agregar</button>
            </form>
          }
          <div class="vista">
            <button mat-button type="button" (click)="expandirTodo()"><mat-icon>unfold_more</mat-icon> Abrir todo</button>
            <button mat-button type="button" (click)="abiertos.set(vacio())"><mat-icon>unfold_less</mat-icon> Cerrar todo</button>
          </div>
        </div>

        <ng-template #nodoTpl let-nodos let-nivel="nivel">
          <ul [class.raiz]="nivel === 0">
            @for (n of nodos; track n.id) {
              <li>
                <div class="nodo" [class.inactiva]="!n.activo">
                  @if (n.hijos?.length) {
                    <button mat-icon-button class="chico" (click)="alternar(n.id)" [attr.aria-expanded]="abiertos().has(n.id)"
                            [attr.aria-label]="(abiertos().has(n.id) ? 'Cerrar ' : 'Abrir ') + n.nombre">
                      <mat-icon>{{ abiertos().has(n.id) ? 'expand_more' : 'chevron_right' }}</mat-icon>
                    </button>
                  } @else {
                    <span class="hueco"></span>
                  }
                  @if (editandoId() === n.id) {
                    <input class="edicion" [(ngModel)]="nombreEdicion" [attr.aria-label]="'Nuevo nombre de ' + n.nombre"
                           (keydown.enter)="renombrar(n)" (keydown.escape)="editandoId.set(null)" />
                    <button mat-icon-button class="chico" [attr.aria-label]="'Guardar el nombre de ' + n.nombre" matTooltip="Guardar" (click)="renombrar(n)"><mat-icon>check</mat-icon></button>
                    <button mat-icon-button class="chico" aria-label="Cancelar" matTooltip="Cancelar" (click)="editandoId.set(null)"><mat-icon>close</mat-icon></button>
                  } @else {
                    <span class="nombre" [class.principal]="nivel === 0">{{ n.nombre }}</span>
                    @if (n.hijos?.length) { <span class="cantidad">{{ n.hijos.length }} {{ n.hijos.length === 1 ? 'subcategoría' : 'subcategorías' }}</span> }
                    @if (!n.activo) { <app-estado valor="inactivo" /> }
                  }
                  @if (esAdmin && editandoId() !== n.id) {
                    <span class="acciones">
                      <button mat-icon-button class="chico" matTooltip="Agregar subcategoría" [attr.aria-label]="'Agregar subcategoría en ' + n.nombre" (click)="nuevaEn(n)"><mat-icon>add</mat-icon></button>
                      <button mat-icon-button class="chico" matTooltip="Renombrar" [attr.aria-label]="'Renombrar ' + n.nombre" (click)="editar(n)"><mat-icon>edit</mat-icon></button>
                      @if (n.activo) {
                        <button mat-icon-button class="chico" matTooltip="Dar de baja" [attr.aria-label]="'Dar de baja ' + n.nombre" (click)="darDeBaja(n)"><mat-icon>delete</mat-icon></button>
                      } @else {
                        <button mat-icon-button class="chico" matTooltip="Reactivar" [attr.aria-label]="'Reactivar ' + n.nombre" (click)="reactivar(n)"><mat-icon>restore</mat-icon></button>
                      }
                    </span>
                  }
                </div>
                @if (padreNueva() === n.id) {
                  <div class="nueva-hija">
                    <input class="edicion" placeholder="Nombre de la subcategoría" [attr.aria-label]="'Nueva subcategoría en ' + n.nombre"
                           [(ngModel)]="nombreNueva" (keydown.enter)="crear(n.id)" (keydown.escape)="padreNueva.set(undefined)" />
                    <button mat-flat-button (click)="crear(n.id)" [disabled]="!nombreNueva.trim()">Agregar</button>
                    <button mat-button (click)="padreNueva.set(undefined)">Cancelar</button>
                  </div>
                }
                @if (n.hijos?.length && abiertos().has(n.id)) {
                  <ng-container *ngTemplateOutlet="nodoTpl; context: { $implicit: n.hijos, nivel: nivel + 1 }" />
                }
              </li>
            }
          </ul>
        </ng-template>
        @if (arbol().length) {
          <div class="arbol">
            <ng-container *ngTemplateOutlet="nodoTpl; context: { $implicit: arbol(), nivel: 0 }" />
          </div>
        } @else {
          <p class="vacio">Todavía no hay categorías.</p>
        }
      </div>
    </section>
  `,
  styles: `
    .barra {
      display: flex; gap: 8px 16px; align-items: flex-start; flex-wrap: wrap; padding: 16px 20px 4px;
      border-bottom: 1px solid var(--sicat-borde);
    }
    .nueva-raiz { display: flex; gap: 8px 12px; align-items: flex-start; flex-wrap: wrap; flex: 1 1 380px; }
    .nueva-raiz mat-form-field { flex: 1 1 260px; max-width: 420px; }
    .nueva-raiz button { min-height: 52px; }
    .vista { display: flex; gap: 4px; margin-left: auto; min-height: 52px; align-items: center; }
    .arbol { padding: 8px 12px 16px; }
    ul { list-style: none; margin: 0; padding: 0 0 0 20px; }
    /* Guía vertical para ver de qué categoría cuelga cada subcategoría. */
    ul:not(.raiz) { margin-left: 15px; padding-left: 12px; border-left: 1px solid var(--sicat-borde); }
    ul.raiz { padding-left: 0; }
    ul.raiz > li + li { border-top: 1px solid var(--sicat-borde); }
    .nodo { display: flex; align-items: center; flex-wrap: wrap; gap: 4px 8px; min-height: 40px; padding: 2px 6px 2px 0; border-radius: 6px; }
    .nodo:hover, .nodo:focus-within { background: var(--mat-sys-surface-container-low); }
    .nombre.principal { font-weight: 600; font-stretch: var(--sicat-ancho-nombre); }
    .inactiva .nombre { color: var(--sicat-texto-suave); }
    .cantidad { font-size: 12.5px; color: var(--sicat-texto-suave); font-variant-numeric: tabular-nums; }
    /* Las acciones aparecen al pasar el mouse o al llegar con el teclado; en pantallas táctiles, siempre. */
    .acciones { display: inline-flex; margin-left: auto; opacity: 0; transition: opacity 120ms var(--sicat-curva); }
    .nodo:hover .acciones, .nodo:focus-within .acciones { opacity: 1; }
    .hueco { display: inline-block; width: 32px; }
    .chico { --mat-icon-button-state-layer-size: 32px; width: 32px; height: 32px; padding: 4px; }
    .edicion {
      height: 36px; padding: 0 10px; min-width: min(280px, 100%); border: 1px solid var(--mat-sys-outline); border-radius: 4px;
      background: var(--mat-sys-surface); color: var(--sicat-tinta); font: inherit;
    }
    .edicion:focus { outline: 2px solid var(--sicat-rio); outline-offset: 0; border-color: var(--sicat-rio); }
    .nueva-hija { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; padding: 4px 0 10px 40px; }
    @media (hover: none) { .acciones { opacity: 1; } }
    @media (prefers-reduced-motion: reduce) { .acciones { transition: none; } }
  `,
})
export class CategoriasPage {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly confirmar = inject(Confirmar);
  private readonly cache = inject(Catalogos);
  protected readonly esAdmin = inject(AuthService).tieneRol('ADMIN');

  protected readonly arbol = signal<CategoriaNodo[]>([]);
  protected readonly abiertos = signal(new Set<number>());
  protected readonly editandoId = signal<number | null>(null);
  protected readonly padreNueva = signal<number | undefined>(undefined);
  protected incluirInactivas = false;
  protected nombreEdicion = '';
  protected nombreNueva = '';
  protected nombreRaiz = '';

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.api.get<CategoriaNodo[]>('/categorias', { incluirInactivas: this.incluirInactivas }).subscribe({
      next: (a) => this.arbol.set(a),
      error: (e) => this.avisos.error(e),
    });
  }

  protected alternar(id: number): void {
    this.abiertos.update((s) => {
      const r = new Set(s);
      if (r.has(id)) {
        r.delete(id);
      } else {
        r.add(id);
      }
      return r;
    });
  }

  protected vacio(): Set<number> {
    return new Set();
  }

  /** Abre todas las categorías que tienen subcategorías. */
  protected expandirTodo(): void {
    const ids = new Set<number>();
    const recorrer = (nodos: CategoriaNodo[]) =>
      nodos.forEach((n) => {
        if (n.hijos?.length) {
          ids.add(n.id);
          recorrer(n.hijos);
        }
      });
    recorrer(this.arbol());
    this.abiertos.set(ids);
  }

  protected editar(n: CategoriaNodo): void {
    this.nombreEdicion = n.nombre;
    this.editandoId.set(n.id);
  }

  protected nuevaEn(n: CategoriaNodo): void {
    this.nombreNueva = '';
    this.padreNueva.set(n.id);
    this.abiertos.update((s) => new Set(s).add(n.id));
  }

  protected renombrar(n: CategoriaNodo): void {
    this.guardar(n, { nombre: this.nombreEdicion.trim(), padreId: n.padreId }, 'Categoría renombrada');
  }

  protected reactivar(n: CategoriaNodo): void {
    this.guardar(n, { nombre: n.nombre, padreId: n.padreId, activo: true }, 'Categoría reactivada');
  }

  private guardar(n: CategoriaNodo, cuerpo: object, mensaje: string): void {
    this.api.put(`/categorias/${n.id}`, cuerpo).subscribe({
      next: () => {
        this.editandoId.set(null);
        this.avisos.ok(mensaje);
        this.listo();
      },
      error: (e) => this.avisos.error(e),
    });
  }

  protected crear(padreId: number | null): void {
    const nombre = (padreId === null ? this.nombreRaiz : this.nombreNueva).trim();
    if (!nombre) {
      return;
    }
    this.api.post('/categorias', { nombre, padreId }).subscribe({
      next: () => {
        this.nombreRaiz = '';
        this.nombreNueva = '';
        this.padreNueva.set(undefined);
        this.avisos.ok('Categoría creada');
        this.listo();
      },
      error: (e) => this.avisos.error(e),
    });
  }

  protected darDeBaja(n: CategoriaNodo): void {
    this.confirmar
      .pedir('Dar de baja la categoría', `"${n.nombre}" deja de ofrecerse al cargar ofertas. Si deja incompleto un establecimiento publicado, no se permite.`, 'Dar de baja')
      .subscribe((ok) => {
        if (ok) {
          this.api.delete(`/categorias/${n.id}`).subscribe({
            next: () => {
              this.avisos.ok('Categoría dada de baja');
              this.listo();
            },
            error: (e) => this.avisos.error(e),
          });
        }
      });
  }

  private listo(): void {
    this.cache.invalidar('categorias');
    this.cargar();
  }
}
