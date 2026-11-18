import { Component, effect, inject, input, numberAttribute, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { Router, RouterLink } from '@angular/router';

import { HorariosEditor } from '../comun/horarios-editor';
import { Historial } from '../comun/historial';
import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { Avisos } from '../core/avisos';
import { CambiosPendientes } from '../core/cambios-pendientes';
import { Confirmar } from '../core/confirmar';
import { EstadoChip } from '../core/estado';
import { Oferta } from '../core/modelos';
import { AlojamientoForm } from './alojamiento-form';
import { CircuitoForm } from './circuito-form';
import { OfertaDatos } from './oferta-datos';
import { SalonesEditor } from './salones-editor';
import { VisitasEditor } from './visitas-editor';

type Especializacion = 'alojamiento' | 'circuito' | 'salones' | 'visitas';

const NOMBRES: Record<Especializacion, string> = {
  alojamiento: 'Alojamiento',
  circuito: 'Circuito',
  salones: 'Salones',
  visitas: 'Visitas guiadas',
};

/** Una oferta del establecimiento: datos, horarios y especializaciones (alojamiento, circuito, salones, visitas). */
@Component({
  selector: 'app-oferta-page',
  imports: [
    RouterLink, MatTabsModule, MatButtonModule, MatIconModule, MatMenuModule, MatProgressBarModule, EstadoChip,
    OfertaDatos, HorariosEditor, AlojamientoForm, CircuitoForm, SalonesEditor, VisitasEditor, Historial,
  ],
  template: `
    <section class="pagina">
      @if (oferta(); as o) {
        <div class="encabezado">
          <a mat-icon-button [routerLink]="['/establecimientos', o.establecimientoId]" [queryParams]="{ pestania: 'ofertas' }" aria-label="Volver">
            <mat-icon>arrow_back</mat-icon>
          </a>
          <h1>
            {{ o.categoria }}
            @if (!o.activo) { <app-estado valor="inactivo" /> }
          </h1>
          @if (puedeEditar) {
            <button mat-stroked-button [matMenuTriggerFor]="agregar"><mat-icon>add</mat-icon> Especialización</button>
            <mat-menu #agregar="matMenu">
              @for (e of disponibles(); track e) {
                <button mat-menu-item (click)="activar(e)">{{ nombres[e] }}</button>
              } @empty {
                <span class="suave menu-vacio">Ya tiene todas</span>
              }
            </mat-menu>
            <button mat-icon-button [matMenuTriggerFor]="mas" aria-label="Más acciones"><mat-icon>more_vert</mat-icon></button>
            <mat-menu #mas="matMenu">
              @if (o.activo) {
                <button mat-menu-item (click)="darDeBaja()"><mat-icon>delete</mat-icon> Dar de baja la oferta</button>
              } @else {
                <button mat-menu-item (click)="reactivar()"><mat-icon>restore</mat-icon> Reactivar</button>
              }
            </mat-menu>
          }
        </div>
        <p class="suave sub">
          Oferta de <a [routerLink]="['/establecimientos', o.establecimientoId]">{{ o.establecimiento }}</a>
        </p>

        <div class="tarjeta">
          <mat-tab-group mat-stretch-tabs="false" animationDuration="0ms" [(selectedIndex)]="indice" preserveContent>
            <mat-tab [label]="marca('Datos', 'Datos de la oferta')">
              <ng-template matTabContent>
                <app-oferta-datos [oferta]="o" [editable]="puedeEditar" (guardado)="oferta.set($event)" />
              </ng-template>
            </mat-tab>
            <mat-tab [label]="marca('Horarios')">
              <ng-template matTabContent>
                <app-horarios-editor [ruta]="'/ofertas/' + o.id + '/horarios'" [editable]="puedeEditar" />
              </ng-template>
            </mat-tab>
            @for (e of activas(); track e) {
              <mat-tab [label]="marca(nombres[e])">
                <ng-template matTabContent>
                  @switch (e) {
                    @case ('alojamiento') { <app-alojamiento-form [ofertaId]="o.id" [alojamiento]="o.alojamiento" [editable]="puedeEditar" /> }
                    @case ('circuito') { <app-circuito-form [ofertaId]="o.id" [circuito]="o.circuito" [editable]="puedeEditar" /> }
                    @case ('salones') { <app-salones-editor [ofertaId]="o.id" [editable]="puedeEditar" /> }
                    @case ('visitas') { <app-visitas-editor [ofertaId]="o.id" [editable]="puedeEditar" /> }
                  }
                </ng-template>
              </mat-tab>
            }
            @if (puedeEditar) {
              <mat-tab label="Historial">
                <ng-template matTabContent>@if (indice === 2 + activas().length) { <app-historial tabla="oferta" [clave]="o.id" /> }</ng-template>
              </mat-tab>
            }
          </mat-tab-group>
        </div>
      } @else {
        <mat-progress-bar mode="indeterminate" />
      }
    </section>
  `,
  styles: `
    h1 { display: flex; align-items: center; gap: 8px; }
    .sub { margin: -8px 0 0; }
    .menu-vacio { display: block; padding: 12px 16px; }
  `,
})
export class OfertaPage {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly confirmar = inject(Confirmar);
  private readonly router = inject(Router);

  readonly id = input.required({ transform: numberAttribute });

  protected readonly puedeEditar = inject(AuthService).tieneRol('ADMIN', 'CARGADOR');
  protected readonly nombres = NOMBRES;
  protected readonly oferta = signal<Oferta | null>(null);
  protected readonly activas = signal<Especializacion[]>([]);
  protected readonly disponibles = signal<Especializacion[]>([]);
  protected indice = 0;

  private readonly cambios = inject(CambiosPendientes);
  /** "Contactos •" si esa pestaña tiene cambios sin guardar. */
  protected marca(texto: string, etiqueta = texto): string {
    return this.cambios.pendientes().includes(etiqueta) ? `${texto} •` : texto;
  }

  constructor() {
    effect(() => this.cargar(this.id()));
  }

  private cargar(id: number): void {
    this.api.get<Oferta>(`/ofertas/${id}`).subscribe({
      next: (o) => {
        this.oferta.set(o);
        const tiene: Especializacion[] = [];
        if (o.alojamiento) tiene.push('alojamiento');
        if (o.circuito) tiene.push('circuito');
        if (o.salones?.length) tiene.push('salones');
        if (o.visitasGuiadas?.length) tiene.push('visitas');
        this.activas.set(tiene);
        this.actualizarDisponibles();
      },
      error: (e) => {
        this.avisos.error(e);
        this.router.navigate(['/establecimientos']);
      },
    });
  }

  private actualizarDisponibles(): void {
    const activas = this.activas();
    this.disponibles.set((Object.keys(NOMBRES) as Especializacion[]).filter((e) => !activas.includes(e)));
  }

  /** Agrega la pestaña; los datos se guardan recién cuando se completa el formulario. */
  protected activar(e: Especializacion): void {
    this.activas.update((l) => [...l, e]);
    this.actualizarDisponibles();
    this.indice = 2 + this.activas().length - 1;
  }

  protected darDeBaja(): void {
    this.confirmar.pedir('Dar de baja la oferta', 'Si es la única oferta válida de un establecimiento publicado, primero hay que despublicarlo. Se puede reactivar.', 'Dar de baja').subscribe((ok) => {
      if (ok) {
        this.api.delete(`/ofertas/${this.id()}`).subscribe({
          next: () => {
            this.avisos.ok('Oferta dada de baja');
            this.cargar(this.id());
          },
          error: (e) => this.avisos.error(e),
        });
      }
    });
  }

  protected reactivar(): void {
    const o = this.oferta()!;
    const cuerpo = {
      categoriaId: o.categoriaId,
      acceso: o.acceso,
      publicoAdmitido: o.publicoAdmitido,
      frecuencia: o.frecuencia,
      fechaRelevamiento: o.fechaRelevamiento,
      activo: true,
    };
    this.api.put<Oferta>(`/ofertas/${o.id}`, cuerpo).subscribe({
      next: () => {
        this.avisos.ok('Oferta reactivada');
        this.cargar(o.id);
      },
      error: (e) => this.avisos.error(e),
    });
  }
}
