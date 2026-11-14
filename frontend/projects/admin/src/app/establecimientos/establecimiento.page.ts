import { Component, inject, input, numberAttribute, signal, viewChild, effect } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { CaracteristicasEditor } from '../comun/caracteristicas-editor';
import { ContactosEditor } from '../comun/contactos-editor';
import { Historial } from '../comun/historial';
import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { Avisos } from '../core/avisos';
import { Confirmar } from '../core/confirmar';
import { EstadoChip } from '../core/estado';
import { Establecimiento } from '../core/modelos';
import { DistincionesEditor } from './distinciones-editor';
import { EstablecimientoForm } from './establecimiento-form';
import { OfertasLista } from './ofertas-lista';
import { PublicacionPanel } from './publicacion-panel';
import { ReferentesEditor } from './referentes-editor';

const PESTANIAS = ['datos', 'publicacion', 'ofertas', 'contactos', 'caracteristicas', 'referentes', 'distinciones', 'historial'];

@Component({
  selector: 'app-establecimiento-page',
  imports: [
    RouterLink, MatTabsModule, MatButtonModule, MatIconModule, MatMenuModule, MatProgressBarModule, EstadoChip,
    EstablecimientoForm, PublicacionPanel, OfertasLista, ContactosEditor, CaracteristicasEditor,
    ReferentesEditor, DistincionesEditor, Historial,
  ],
  template: `
    <section class="pagina">
      <div class="encabezado">
        <a mat-icon-button routerLink="/establecimientos" aria-label="Volver"><mat-icon>arrow_back</mat-icon></a>
        <h1>
          {{ e()?.nombre ?? 'Establecimiento' }}
          @if (e(); as est) {
            <app-estado [valor]="est.estadoPublicacion" />
            @if (!est.activo) { <app-estado valor="inactivo" /> }
          }
        </h1>
        @if (e(); as est) {
          @if (esAdmin) {
            <button mat-icon-button [matMenuTriggerFor]="menu" aria-label="Más acciones"><mat-icon>more_vert</mat-icon></button>
            <mat-menu #menu="matMenu">
              @if (est.activo) {
                <button mat-menu-item (click)="darDeBaja()"><mat-icon>delete</mat-icon> Dar de baja</button>
              } @else {
                <button mat-menu-item (click)="reactivar()"><mat-icon>restore</mat-icon> Reactivar</button>
              }
            </mat-menu>
          }
        }
      </div>
      @if (e(); as est) {
        <p class="suave sub">
          Oferente: <a [routerLink]="['/oferentes', est.oferente.id]">{{ est.oferente.nombre }}</a>
          @if (est.lugar) { · {{ est.lugar.direccion || est.lugar.nombre }}{{ est.lugar.localidad ? ', ' + est.lugar.localidad : '' }} }
        </p>

        <div class="tarjeta">
          <mat-tab-group [selectedIndex]="indice()" (selectedIndexChange)="cambiarPestania($event)" mat-stretch-tabs="false" animationDuration="0ms">
            <mat-tab label="Datos">
              <ng-template matTabContent>
                <app-establecimiento-form [establecimiento]="est" [editable]="puedeEditar" (guardado)="actualizar($event)" />
              </ng-template>
            </mat-tab>
            <mat-tab label="Publicación">
              <ng-template matTabContent>
                <app-publicacion-panel #publicacion [establecimientoId]="est.id" [editable]="puedeEditar" (cambio)="actualizar($event)" />
              </ng-template>
            </mat-tab>
            <mat-tab label="Ofertas">
              <ng-template matTabContent>
                <app-ofertas-lista [establecimientoId]="est.id" [editable]="puedeEditar" />
              </ng-template>
            </mat-tab>
            <mat-tab label="Contactos">
              <ng-template matTabContent>
                <app-contactos-editor [ruta]="'/establecimientos/' + est.id + '/contactos'" [editable]="puedeEditar" />
              </ng-template>
            </mat-tab>
            <mat-tab label="Características">
              <ng-template matTabContent>
                <app-caracteristicas-editor [ruta]="'/establecimientos/' + est.id + '/caracteristicas'" [editable]="puedeEditar" />
              </ng-template>
            </mat-tab>
            <mat-tab label="Referentes">
              <ng-template matTabContent>
                <app-referentes-editor [establecimientoId]="est.id" [editable]="puedeEditar" />
              </ng-template>
            </mat-tab>
            <mat-tab label="Distinciones">
              <ng-template matTabContent>
                <app-distinciones-editor [establecimientoId]="est.id" [editable]="puedeEditar" />
              </ng-template>
            </mat-tab>
            @if (puedeEditar) {
              <mat-tab label="Historial">
                <ng-template matTabContent>
                  <app-historial tabla="establecimiento" [clave]="est.id" />
                </ng-template>
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
    h1 { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .sub { margin: -8px 0 0; }
  `,
})
export class EstablecimientoPage {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly confirmar = inject(Confirmar);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);

  readonly id = input.required({ transform: numberAttribute });

  protected readonly puedeEditar = this.auth.tieneRol('ADMIN', 'CARGADOR');
  protected readonly esAdmin = this.auth.tieneRol('ADMIN');
  protected readonly e = signal<Establecimiento | null>(null);
  protected readonly indice = signal(Math.max(0, PESTANIAS.indexOf(this.route.snapshot.queryParamMap.get('pestania') ?? 'datos')));

  private readonly publicacion = viewChild<PublicacionPanel>('publicacion');

  constructor() {
    effect(() => this.cargar(this.id()));
  }

  private cargar(id: number): void {
    this.api.get<Establecimiento>(`/establecimientos/${id}`).subscribe({
      next: (e) => this.e.set(e),
      error: (err) => {
        this.avisos.error(err);
        this.router.navigate(['/establecimientos']);
      },
    });
  }

  protected cambiarPestania(i: number): void {
    this.indice.set(i);
    this.router.navigate([], { queryParams: { pestania: PESTANIAS[i] === 'datos' ? null : PESTANIAS[i] }, replaceUrl: true });
  }

  protected actualizar(e: Establecimiento): void {
    this.e.set(e);
    this.publicacion()?.cargar();
  }

  protected darDeBaja(): void {
    this.confirmar
      .pedir('Dar de baja', 'El establecimiento deja de estar disponible y sale del sitio público. Se puede reactivar.', 'Dar de baja')
      .subscribe((ok) => {
        if (ok) {
          this.api.delete(`/establecimientos/${this.id()}`).subscribe({
            next: () => {
              this.avisos.ok('Establecimiento dado de baja');
              this.recargar();
            },
            error: (err) => this.avisos.error(err),
          });
        }
      });
  }

  protected reactivar(): void {
    const e = this.e()!;
    const cuerpo = {
      oferenteId: e.oferente.id,
      lugarId: e.lugar?.id ?? null,
      nombre: e.nombre,
      descripcion: e.descripcion,
      anioInauguracion: e.anioInauguracion,
      estadoFuncionamiento: e.estadoFuncionamiento,
      motivoEstado: e.motivoEstado,
      fechaReapertura: e.fechaReapertura,
      destacado: e.destacado,
      activo: true,
    };
    this.api.put<Establecimiento>(`/establecimientos/${e.id}`, cuerpo).subscribe({
      next: () => {
        this.avisos.ok('Establecimiento reactivado');
        this.recargar();
      },
      error: (err) => this.avisos.error(err),
    });
  }

  private recargar(): void {
    this.e.set(null);
    this.cargar(this.id());
  }
}
