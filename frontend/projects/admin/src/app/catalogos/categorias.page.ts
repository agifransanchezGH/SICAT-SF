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
      <div class="tarjeta arbol">
        <ng-template #nodoTpl let-nodos let-nivel="nivel">
          <ul [class.raiz]="nivel === 0">
            @for (n of nodos; track n.id) {
              <li>
                <div class="nodo">
                  @if (n.hijos?.length) {
                    <button mat-icon-button class="chico" (click)="alternar(n.id)" [attr.aria-label]="abiertos().has(n.id) ? 'Cerrar' : 'Abrir'">
                      <mat-icon>{{ abiertos().has(n.id) ? 'expand_more' : 'chevron_right' }}</mat-icon>
                    </button>
                  } @else {
                    <span class="hueco"></span>
                  }
                  @if (editandoId() === n.id) {
                    <input class="edicion" [(ngModel)]="nombreEdicion" (keydown.enter)="renombrar(n)" (keydown.escape)="editandoId.set(null)" />
                    <button mat-icon-button class="chico" matTooltip="Guardar" (click)="renombrar(n)"><mat-icon>check</mat-icon></button>
                  } @else {
                    <span class="nombre">{{ n.nombre }}</span>
                    @if (!n.activo) { <app-estado valor="inactivo" /> }
                  }
                  @if (esAdmin && editandoId() !== n.id) {
                    <span class="acciones">
                      <button mat-icon-button class="chico" matTooltip="Agregar subcategoría" (click)="nuevaEn(n)"><mat-icon>add</mat-icon></button>
                      <button mat-icon-button class="chico" matTooltip="Renombrar" (click)="editar(n)"><mat-icon>edit</mat-icon></button>
                      @if (n.activo) {
                        <button mat-icon-button class="chico" matTooltip="Dar de baja" (click)="darDeBaja(n)"><mat-icon>delete</mat-icon></button>
                      } @else {
                        <button mat-icon-button class="chico" matTooltip="Reactivar" (click)="reactivar(n)"><mat-icon>restore</mat-icon></button>
                      }
                    </span>
                  }
                </div>
                @if (padreNueva() === n.id) {
                  <div class="nueva-hija">
                    <input class="edicion" placeholder="Nombre de la subcategoría" [(ngModel)]="nombreNueva" (keydown.enter)="crear(n.id)" (keydown.escape)="padreNueva.set(undefined)" />
                    <button mat-flat-button (click)="crear(n.id)" [disabled]="!nombreNueva.trim()">Agregar</button>
                  </div>
                }
                @if (n.hijos?.length && abiertos().has(n.id)) {
                  <ng-container *ngTemplateOutlet="nodoTpl; context: { $implicit: n.hijos, nivel: nivel + 1 }" />
                }
              </li>
            }
          </ul>
        </ng-template>
        <ng-container *ngTemplateOutlet="nodoTpl; context: { $implicit: arbol(), nivel: 0 }" />

        @if (esAdmin) {
          <div class="nueva-raiz">
            <mat-form-field>
              <mat-label>Nueva categoría principal</mat-label>
              <input matInput [(ngModel)]="nombreRaiz" (keydown.enter)="crear(null)" />
            </mat-form-field>
            <button mat-flat-button (click)="crear(null)" [disabled]="!nombreRaiz.trim()"><mat-icon>add</mat-icon> Agregar</button>
          </div>
        }
      </div>
    </section>
  `,
  styles: `
    .arbol { padding: 12px 16px; }
    ul { list-style: none; margin: 0; padding-left: 24px; }
    ul.raiz { padding-left: 0; }
    .nodo { display: flex; align-items: center; gap: 4px; min-height: 36px; }
    .nodo:hover { background: var(--mat-sys-surface-container-low); border-radius: 6px; }
    .nodo .acciones { visibility: hidden; margin-left: 8px; }
    .nodo:hover .acciones { visibility: visible; }
    .nombre { margin-right: 6px; }
    .hueco { display: inline-block; width: 32px; }
    .chico { --mdc-icon-button-state-layer-size: 32px; width: 32px; height: 32px; padding: 4px; }
    .edicion { padding: 4px 8px; border: 1px solid var(--sicat-borde); border-radius: 6px; font: inherit; min-width: 240px; }
    .nueva-hija { display: flex; gap: 8px; align-items: center; padding: 4px 0 8px 36px; }
    .nueva-raiz { display: flex; gap: 12px; align-items: center; margin-top: 16px; flex-wrap: wrap; }
    @media (hover: none) { .nodo .acciones { visibility: visible; } }
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
