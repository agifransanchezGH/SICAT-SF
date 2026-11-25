import { LiveAnnouncer } from '@angular/cdk/a11y';
import { Component, ElementRef, inject, input, numberAttribute, signal, viewChild, effect, untracked } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';

import { CaracteristicasEditor } from '../comun/caracteristicas-editor';
import { ContactosEditor } from '../comun/contactos-editor';
import { Historial } from '../comun/historial';
import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { Avisos } from '../core/avisos';
import { Confirmar } from '../core/confirmar';
import { EstadoChip } from '../core/estado';
import { Establecimiento, Lugar, Requisito } from '../core/modelos';
import { LugarDialog, OPCIONES_DIALOGO_LUGAR } from '../lugares/lugar-dialog';
import { ChecklistPublicacion } from './checklist-publicacion';
import { NavegacionCola } from './cola-fichas';
import { DistincionesEditor } from './distinciones-editor';
import { EstablecimientoForm } from './establecimiento-form';
import { OfertasLista } from './ofertas-lista';
import { PublicacionPanel } from './publicacion-panel';
import { CambiosPendientes } from '../core/cambios-pendientes';
import { ReferentesEditor } from './referentes-editor';
import { HistorialNavegacion } from '../core/navegacion';

const PESTANIAS = ['datos', 'publicacion', 'ofertas', 'contactos', 'caracteristicas', 'referentes', 'distinciones', 'historial'];

@Component({
  selector: 'app-establecimiento-page',
  imports: [
    RouterLink, MatTabsModule, MatButtonModule, MatIconModule, MatMenuModule, MatProgressBarModule, EstadoChip,
    EstablecimientoForm, PublicacionPanel, OfertasLista, ContactosEditor, CaracteristicasEditor,
    ReferentesEditor, DistincionesEditor, Historial, ChecklistPublicacion,
  ],
  template: `
    <section class="pagina">
      <div class="encabezado">
        <button mat-icon-button type="button" aria-label="Volver" (click)="irAtras()"><mat-icon>arrow_back</mat-icon></button>
        <h1>
          {{ e()?.nombre ?? 'Establecimiento' }}
          @if (e(); as est) {
            <app-estado [valor]="est.estadoPublicacion" />
            @if (!est.activo) { <app-estado valor="inactivo" /> }
          }
        </h1>
        @if (e(); as est) {
          @if (esAdmin) {
            <button #masAcciones mat-icon-button [matMenuTriggerFor]="menu" aria-label="Más acciones"><mat-icon>more_vert</mat-icon></button>
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
      @if (nav.posicion(id()); as pos) {
        <nav class="cola" aria-label="Recorrido de la cola">
          <button mat-stroked-button type="button" [disabled]="!nav.hayAnterior(id()) || moviendo()" (click)="anterior()">
            <mat-icon>chevron_left</mat-icon> Anterior
          </button>
          <span class="donde">
            <span class="etiqueta">{{ nav.cola()?.etiqueta }}</span>
            @if (pos.numero) { <span class="numero">{{ pos.numero }} de {{ pos.total }}</span> }
          </span>
          <button mat-stroked-button type="button" class="siguiente" [disabled]="moviendo()" (click)="siguiente()">
            Siguiente <mat-icon iconPositionEnd>chevron_right</mat-icon>
          </button>
        </nav>
      }
      @if (e(); as est) {
        <app-checklist-publicacion [establecimiento]="est" [editable]="puedeEditar" (irA)="irA($event)" (cambio)="actualizar($event)" />
        <p class="suave sub">
          Oferente: <a [routerLink]="['/oferentes', est.oferente.id]">{{ est.oferente.nombre }}</a>
          @if (est.lugar) { · {{ est.lugar.direccion || est.lugar.nombre }}{{ est.lugar.localidad ? ', ' + est.lugar.localidad : '' }} }
        </p>

        <div class="tarjeta">
          <!-- preserveContent: al cambiar de pestaña no se pierde lo que se estaba escribiendo -->
          <mat-tab-group [selectedIndex]="indice()" (selectedIndexChange)="cambiarPestania($event)" mat-stretch-tabs="false"
                         animationDuration="0ms" preserveContent>
            <mat-tab [label]="marca('Datos')">
              <ng-template matTabContent>
                <app-establecimiento-form #formulario [establecimiento]="est" [editable]="puedeEditar" [enCola]="!!nav.posicion(id())"
                                          (guardado)="actualizar($event)" (pasarAlSiguiente)="siguiente()" />
              </ng-template>
            </mat-tab>
            <mat-tab label="Publicación">
              <ng-template matTabContent>
                <!-- Se vuelve a crear cada vez que se abre, para mostrar lo que falta con los datos recién guardados -->
                @if (indice() === 1) {
                  <app-publicacion-panel #publicacion [establecimientoId]="est.id" [editable]="puedeEditar" (cambio)="actualizar($event)" />
                }
              </ng-template>
            </mat-tab>
            <mat-tab label="Ofertas">
              <ng-template matTabContent>
                @if (indice() === 2) { <app-ofertas-lista [establecimientoId]="est.id" [editable]="puedeEditar" /> }
              </ng-template>
            </mat-tab>
            <mat-tab [label]="marca('Contactos')">
              <ng-template matTabContent>
                <app-contactos-editor [ruta]="'/establecimientos/' + est.id + '/contactos'" [editable]="puedeEditar" />
              </ng-template>
            </mat-tab>
            <mat-tab [label]="marca('Características')">
              <ng-template matTabContent>
                <app-caracteristicas-editor [ruta]="'/establecimientos/' + est.id + '/caracteristicas'" [editable]="puedeEditar" />
              </ng-template>
            </mat-tab>
            <mat-tab [label]="marca('Referentes')">
              <ng-template matTabContent>
                <app-referentes-editor [establecimientoId]="est.id" [editable]="puedeEditar" />
              </ng-template>
            </mat-tab>
            <mat-tab [label]="marca('Distinciones')">
              <ng-template matTabContent>
                <app-distinciones-editor [establecimientoId]="est.id" [editable]="puedeEditar" />
              </ng-template>
            </mat-tab>
            @if (puedeEditar) {
              <mat-tab label="Historial">
                <ng-template matTabContent>
                  @if (indice() === 7) { <app-historial tabla="establecimiento" [clave]="est.id" /> }
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
    .sub { margin: 0 0 12px; }
    .cola { display: flex; align-items: center; gap: 8px 12px; flex-wrap: wrap; margin: -4px 0 12px; }
    .cola button { min-height: 40px; }
    .donde { display: flex; align-items: baseline; gap: 8px; font-size: 14px; }
    .etiqueta { font-weight: 600; color: var(--sicat-tinta); }
    .numero { color: var(--sicat-texto-suave); font-variant-numeric: tabular-nums; }
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
  private readonly formulario = viewChild<EstablecimientoForm>('formulario');
  private readonly masAcciones = viewChild('masAcciones', { read: ElementRef });
  private readonly dialog = inject(MatDialog);
  private readonly anunciar = inject(LiveAnnouncer);
  protected readonly nav = inject(NavegacionCola);
  protected readonly moviendo = signal(false);
  /** Vuelve al listado de la cola, con sus filtros; si no hay cola, al listado (que recuerda los últimos filtros). */
  private readonly historial = inject(HistorialNavegacion);

  /**
   * Flecha de volver: a la página de la que se vino (mapa, oferente, lugares…). Si se viene recorriendo una cola
   * con Anterior/Siguiente (la página anterior es otra ficha de la cola), al listado de esa cola con sus filtros.
   */
  protected irAtras(): void {
    const cola = this.nav.cola()?.volver;
    const anterior = this.historial.anterior();
    if (cola && this.nav.posicion(this.id()) && (!anterior || /^\/establecimientos\/\d+/.test(anterior))) {
      this.router.navigate([cola.ruta], { queryParams: cola.queryParams });
    } else {
      this.historial.volver('/establecimientos');
    }
  }

  private readonly cambios = inject(CambiosPendientes);
  /** "Contactos •" si esa pestaña tiene cambios sin guardar. */
  protected marca(texto: string, etiqueta = texto): string {
    return this.cambios.pendientes().includes(etiqueta) ? `${texto} •` : texto;
  }

  constructor() {
    // Al pasar a otra ficha de la cola el componente se reutiliza: se vacía para que las pestañas se vuelvan a crear.
    effect(() => {
      const id = this.id();
      untracked(() => {
        this.e.set(null);
        this.cargar(id);
      });
    });
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

  protected siguiente(): void {
    this.mover(this.nav.siguiente(this.id()), 'No quedan más en esta cola.');
  }

  protected anterior(): void {
    this.mover(this.nav.anterior(this.id()), 'Es el primero de la cola.');
  }

  /** La navegación pasa por el guard de cambios sin guardar, igual que salir de la ficha. */
  private mover(destino: Observable<number | null>, sinMas: string): void {
    this.moviendo.set(true);
    destino.subscribe({
      next: (id) => {
        this.moviendo.set(false);
        if (id == null) {
          this.avisos.ok(sinMas);
          return;
        }
        this.router.navigate(['/establecimientos', id], { queryParamsHandling: 'preserve' }).then((ok) => {
          if (ok) {
            const p = this.nav.posicion(id);
            this.anunciar.announce(p?.numero ? `Ficha ${p.numero} de ${p.total}` : 'Siguiente ficha de la cola', 'polite');
          }
        });
      },
      error: (err) => {
        this.moviendo.set(false);
        this.avisos.error(err);
      },
    });
  }

  /** Cada requisito faltante del checklist lleva a donde se arregla. */
  protected irA(r: Requisito): void {
    const e = this.e();
    if (!e) {
      return;
    }
    switch (r) {
      case 'OFERENTE_ACTIVO':
        this.router.navigate(['/oferentes', e.oferente.id]);
        break;
      case 'OFERTA_ACTIVA':
        this.cambiarPestania(PESTANIAS.indexOf('ofertas'));
        break;
      case 'COORDENADAS':
      case 'UBICACION_VERIFICADA':
        if (e.lugar) {
          this.abrirLugar(e.lugar.id);
          break;
        }
        this.irAlCampoLugar();
        break;
      case 'LUGAR':
        this.irAlCampoLugar();
        break;
      case 'ESTABLECIMIENTO_ACTIVO':
        // Reactivar es una decisión: se lleva el foco al menú que la tiene, no se reactiva solo.
        (this.masAcciones()?.nativeElement as HTMLElement | undefined)?.focus();
        if (!this.esAdmin) {
          this.avisos.ok('Solo un administrador puede reactivarlo');
        }
        break;
    }
  }

  private irAlCampoLugar(): void {
    this.cambiarPestania(PESTANIAS.indexOf('datos'));
    setTimeout(() => this.formulario()?.enfocarLugar()); // después de que la pestaña se dibuje
  }

  private abrirLugar(id: number): void {
    this.api.get<Lugar>(`/lugares/${id}`).subscribe({
      next: (l) =>
        this.dialog.open(LugarDialog, { data: l, ...OPCIONES_DIALOGO_LUGAR }).afterClosed().subscribe((r?: Lugar) => {
          if (r) {
            this.recargar();
          }
        }),
      error: (err) => this.avisos.error(err),
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
      notasInternas: e.notasInternas,
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
