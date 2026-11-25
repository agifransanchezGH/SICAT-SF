import { Component, effect, inject, input, numberAttribute, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { Router, RouterLink } from '@angular/router';

import { ContactosEditor } from '../comun/contactos-editor';
import { Historial } from '../comun/historial';
import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { Avisos } from '../core/avisos';
import { CambiosPendientes } from '../core/cambios-pendientes';
import { Confirmar } from '../core/confirmar';
import { EstadoChip } from '../core/estado';
import { Oferente } from '../core/modelos';
import { OferenteDialog } from './oferente-dialog';
import { HistorialNavegacion } from '../core/navegacion';

@Component({
  selector: 'app-oferente-page',
  imports: [RouterLink, MatTabsModule, MatListModule, MatButtonModule, MatIconModule, MatProgressBarModule, EstadoChip, ContactosEditor, Historial],
  template: `
    <section class="pagina">
      @if (oferente(); as o) {
        <div class="encabezado">
          <button mat-icon-button type="button" aria-label="Volver" (click)="irAtras()"><mat-icon>arrow_back</mat-icon></button>
          <h1>{{ o.nombre }} @if (!o.activo) { <app-estado valor="inactivo" /> }</h1>
          @if (puedeEditar) {
            <button mat-stroked-button (click)="editar()"><mat-icon>edit</mat-icon> Editar</button>
          }
          @if (esAdmin) {
            @if (o.activo) {
              <button mat-button (click)="darDeBaja()">Dar de baja</button>
            } @else {
              <button mat-button (click)="reactivar()">Reactivar</button>
            }
          }
        </div>
        @let cantidad = o.establecimientos?.length ?? 0;
        <p class="sub">
          <span>{{ o.tipoOferente || 'Sin tipo' }}</span>
          <span class="suave">{{ cantidad }} {{ cantidad === 1 ? 'establecimiento' : 'establecimientos' }}</span>
        </p>
        <div class="tarjeta">
          <mat-tab-group #pestanias mat-stretch-tabs="false" animationDuration="0ms" preserveContent>
            <mat-tab [label]="'Establecimientos (' + cantidad + ')'">
              @if (cantidad) {
                <mat-nav-list class="lista" aria-label="Establecimientos del oferente">
                  @for (e of o.establecimientos; track e.id) {
                    <a mat-list-item [routerLink]="['/establecimientos', e.id]" [class.inactivo]="!e.activo">
                      <span matListItemTitle>{{ e.nombre }}</span>
                      <span matListItemLine class="estados"><app-estado [valor]="e.estadoPublicacion" /> @if (!e.activo) { <app-estado valor="inactivo" /> }</span>
                      <mat-icon matListItemMeta aria-hidden="true">chevron_right</mat-icon>
                    </a>
                  }
                </mat-nav-list>
              } @else {
                <p class="vacio">No tiene establecimientos. Se le asignan desde la pestaña Datos de cada establecimiento.</p>
              }
            </mat-tab>
            <mat-tab [label]="marca('Contactos')">
              <ng-template matTabContent>
                <app-contactos-editor [ruta]="'/oferentes/' + o.id + '/contactos'" [editable]="puedeEditar" />
              </ng-template>
            </mat-tab>
            @if (puedeEditar) {
              <mat-tab label="Historial">
                <ng-template matTabContent>@if (pestanias.selectedIndex === 2) { <app-historial tabla="oferente" [clave]="o.id" /> }</ng-template>
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
    h1 { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
    .sub { display: flex; gap: 4px 16px; flex-wrap: wrap; margin: -8px 0 0 52px; font-size: 14px; }
    .lista { padding: 0 8px; }
    .lista a + a { border-top: 1px solid var(--sicat-borde); border-radius: 0; }
    .lista .inactivo [matListItemTitle] { color: var(--sicat-texto-suave); }
    .estados { display: flex; gap: 6px; padding-top: 2px; }
    @media (max-width: 720px) { .sub { margin-left: 0; } }
  `,
})
export class OferentePage {
  private readonly historial = inject(HistorialNavegacion);

  /** A la página de la que se vino; si se entró directo, a /oferentes. */
  protected irAtras(): void {
    this.historial.volver('/oferentes');
  }

  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly confirmar = inject(Confirmar);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly id = input.required({ transform: numberAttribute });

  private readonly cambios = inject(CambiosPendientes);
  /** "Contactos •" si esa pestaña tiene cambios sin guardar. */
  protected marca(texto: string, etiqueta = texto): string {
    return this.cambios.pendientes().includes(etiqueta) ? `${texto} •` : texto;
  }
  protected readonly puedeEditar = this.auth.tieneRol('ADMIN', 'CARGADOR');
  protected readonly esAdmin = this.auth.tieneRol('ADMIN');
  protected readonly oferente = signal<Oferente | null>(null);

  constructor() {
    effect(() => this.cargar(this.id()));
  }

  private cargar(id: number): void {
    this.api.get<Oferente>(`/oferentes/${id}`).subscribe({
      next: (o) => this.oferente.set(o),
      error: (e) => {
        this.avisos.error(e);
        this.router.navigate(['/oferentes']);
      },
    });
  }

  protected editar(): void {
    this.dialog
      .open(OferenteDialog, { data: this.oferente() })
      .afterClosed()
      .subscribe((o?: Oferente) => o && this.cargar(o.id));
  }

  protected darDeBaja(): void {
    this.confirmar
      .pedir('Dar de baja', 'Si tiene establecimientos publicados, primero hay que despublicarlos. Se puede reactivar.', 'Dar de baja')
      .subscribe((ok) => {
        if (ok) {
          this.api.delete(`/oferentes/${this.id()}`).subscribe({
            next: () => {
              this.avisos.ok('Oferente dado de baja');
              this.cargar(this.id());
            },
            error: (e) => this.avisos.error(e),
          });
        }
      });
  }

  protected reactivar(): void {
    const o = this.oferente()!;
    this.api.put<Oferente>(`/oferentes/${o.id}`, { nombre: o.nombre, tipoOferenteId: o.tipoOferenteId, activo: true }).subscribe({
      next: () => {
        this.avisos.ok('Oferente reactivado');
        this.cargar(o.id);
      },
      error: (e) => this.avisos.error(e),
    });
  }
}
