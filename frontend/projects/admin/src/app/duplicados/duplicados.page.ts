import { LiveAnnouncer } from '@angular/cdk/a11y';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';

import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { Avisos } from '../core/avisos';
import { Confirmar } from '../core/confirmar';
import { EstadoChip } from '../core/estado';
import { DuplicadoItem, GrupoDuplicados } from '../core/modelos';
import { filasComparacion } from './comparacion';
import { DatosResolver, Resolucion, ResolverDialog } from './resolver-dialog';

/**
 * Posibles duplicados: cada grupo se compara en columnas (un establecimiento por columna). Se marcan los que son
 * realmente el mismo y "Quedarme con este" da de baja a los otros marcados (los no marcados no se tocan); o "No son
 * duplicados" para todo el grupo (no vuelve a aparecer mientras tenga los mismos integrantes).
 */
@Component({
  selector: 'app-duplicados-page',
  imports: [RouterLink, MatButtonModule, MatCheckboxModule, MatIconModule, MatProgressBarModule, EstadoChip],
  template: `
    <section class="pagina">
      <div class="encabezado">
        <h1>Posibles duplicados</h1>
        <button mat-stroked-button (click)="cargar()" [disabled]="cargando()"><mat-icon>refresh</mat-icon> Actualizar</button>
      </div>
      <p class="suave intro">
        Establecimientos activos con el mismo nombre. Los de "misma dirección" son casi seguro el mismo; los demás pueden ser
        sucursales. Las filas marcadas "Difiere" son las que no coinciden. Si solo algunos son el mismo, desmarcá los demás:
        al quedarte con uno se dan de baja solo los marcados.
      </p>
      @if (cargando()) { <mat-progress-bar mode="indeterminate" aria-label="Cargando posibles duplicados" /> }

      @if (error()) {
        <div class="tarjeta vacio" role="alert">
          <p>No se pudo cargar la lista.</p>
          <button mat-stroked-button type="button" (click)="cargar()">Reintentar</button>
        </div>
      }

      @for (g of grupos(); track g.clave) {
        <section class="tarjeta grupo" [attr.aria-labelledby]="'grupo-' + $index">
          <header class="titulo">
            <h2 [id]="'grupo-' + $index">{{ g.establecimientos[0].nombre }}</h2>
            @if (g.mismaDireccion) {
              <span class="marca"><mat-icon aria-hidden="true">warning_amber</mat-icon> Misma dirección</span>
            }
            <span class="suave">{{ g.establecimientos.length }} registros</span>
            <span class="espacio"></span>
            <button mat-stroked-button type="button" [disabled]="ocupado() === g.clave" (click)="noSonDuplicados(g)">
              <mat-icon>call_split</mat-icon> No son duplicados
            </button>
          </header>

          <div class="desplazable" tabindex="0" [attr.aria-label]="'Comparación de ' + g.establecimientos[0].nombre">
            <table class="comparacion">
              <caption class="cdk-visually-hidden">Comparación de los {{ g.establecimientos.length }} registros</caption>
              <thead>
                <tr>
                  <td></td>
                  @for (e of g.establecimientos; track e.id) {
                    <th scope="col">
                      <a [routerLink]="['/establecimientos', e.id]">{{ e.nombre }}</a>
                      <span class="id">#{{ e.id }}</span>
                    </th>
                  }
                </tr>
              </thead>
              <tbody>
                @if (esAdmin) {
                  <tr class="fila-marcar">
                    <th scope="row">Es duplicado</th>
                    @for (e of g.establecimientos; track e.id) {
                      <td>
                        <mat-checkbox [checked]="marcado(g, e)" (change)="alternar(g, e)" [disabled]="ocupado() === g.clave"
                                      [aria-label]="'Marcar ' + e.nombre + ' (#' + e.id + ') como duplicado'">
                          {{ marcado(g, e) ? 'Sí' : 'No, es otro' }}
                        </mat-checkbox>
                      </td>
                    }
                  </tr>
                }
                @for (f of filas(g); track f.etiqueta) {
                  <tr [class.difiere]="f.difiere">
                    <th scope="row">
                      {{ f.etiqueta }}
                      @if (f.difiere) { <span class="difiere-marca">Difiere</span> }
                    </th>
                    @for (v of f.valores; track $index) {
                      <td [class.fuera]="!marcado(g, g.establecimientos[$index])">
                        @if (f.etiqueta === 'Estado') {
                          <app-estado [valor]="v[0]" />
                        } @else if (v.length) {
                          @for (linea of v; track $index) { <div>{{ linea }}</div> }
                        } @else {
                          <span class="suave">—</span>
                        }
                      </td>
                    }
                  </tr>
                }
              </tbody>
              @if (esAdmin) {
                <tfoot>
                  <tr>
                    <td>
                      @if (cantidadMarcados(g) < 2) { <span class="aviso-marcados">Marcá al menos dos para resolver</span> }
                    </td>
                    @for (e of g.establecimientos; track e.id) {
                      <td [class.fuera]="!marcado(g, e)">
                        <button mat-flat-button type="button" class="quedarme"
                                [disabled]="ocupado() === g.clave || !marcado(g, e) || cantidadMarcados(g) < 2"
                                [attr.aria-label]="'Quedarme con ' + e.nombre + ' (#' + e.id + ') y dar de baja los otros marcados'"
                                (click)="quedarmeCon(g, e)">
                          Quedarme con este
                        </button>
                      </td>
                    }
                  </tr>
                </tfoot>
              }
            </table>
          </div>
        </section>
      } @empty {
        @if (!cargando() && !error()) {
          <p class="vacio tarjeta">No quedan posibles duplicados por revisar.</p>
        }
      }
    </section>
  `,
  styles: `
    .intro { margin-top: -8px; }
    .grupo { padding: 16px; margin-bottom: 16px; }
    .titulo { display: flex; flex-wrap: wrap; gap: 8px 12px; align-items: center; margin-bottom: 12px; }
    h2 { margin: 0; font-size: 17px; font-stretch: var(--sicat-ancho-nombre); }
    .marca {
      display: inline-flex; align-items: center; gap: 4px; padding: 2px 10px 2px 6px; border-radius: 999px;
      background: #f6ecd8; color: var(--sicat-alerta); font-size: 13px; font-weight: 600;
    }
    .marca mat-icon { font-size: 16px; width: 16px; height: 16px; }
    .espacio { flex: 1; }
    .titulo button { min-height: 44px; }
    /* En pantallas angostas la tabla se desplaza sola; la columna de etiquetas queda fija */
    .desplazable { overflow-x: auto; }
    .desplazable:focus-visible { outline: 2px solid var(--sicat-rio); outline-offset: 2px; }
    .comparacion { border-collapse: separate; border-spacing: 0; width: 100%; font-size: 14px; }
    .comparacion th, .comparacion td { padding: 8px 12px; text-align: left; vertical-align: top; border-bottom: 1px solid var(--sicat-borde); }
    /* Cada registro es una columna con ancho mínimo: sus celdas (y su botón) no se angostan por debajo de eso. */
    .comparacion thead th, .comparacion tbody td, .comparacion tfoot td { min-width: 200px; }
    .comparacion thead th { font-weight: 700; }
    /*
     * Columna de etiquetas fija a la izquierda: fondo opaco y por encima de las celdas que pasan por debajo al
     * desplazar (los botones de Material crean su propia capa y, sin z-index, se dibujaban encima).
     */
    .comparacion tbody th, .comparacion thead td, .comparacion tfoot td:first-child {
      position: sticky; left: 0; z-index: 2; background: var(--mat-sys-surface); width: 150px; min-width: 130px;
      font-weight: 600; color: var(--sicat-texto-suave); box-shadow: 1px 0 0 var(--sicat-borde);
    }
    .comparacion tbody td, .comparacion tfoot td:not(:first-child) { position: relative; z-index: 0; }
    /* No marcado ("es otro"): texto atenuado sobre fondo gris, sin bajar de contraste AA (no se usa opacity). */
    .comparacion td.fuera { background: var(--mat-sys-surface-container); color: var(--sicat-texto-suave); }
    .fila-marcar td, .comparacion .fila-marcar th { background: var(--mat-sys-surface-container-low); }
    .aviso-marcados { display: block; font-size: 12.5px; font-weight: 600; color: var(--sicat-alerta); }
    .id { margin-left: 6px; color: var(--sicat-texto-suave); font-weight: 400; font-variant-numeric: tabular-nums; }
    tr.difiere td { background: color-mix(in srgb, #f6ecd8 45%, transparent); }
    .difiere-marca {
      display: block; width: fit-content; margin-top: 2px; padding: 0 6px; border-radius: 4px;
      background: #f6ecd8; color: var(--sicat-alerta); font-size: 12px; font-weight: 700;
    }
    tfoot td { border-bottom: 0; padding-top: 12px; }
    /* El botón ocupa el ancho de su columna y, si no entra, el texto baja de línea: no se sale del recuadro. */
    .quedarme { min-height: 44px; width: 100%; max-width: 240px; white-space: normal; line-height: 1.2; }
  `,
})
export class DuplicadosPage {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly confirmar = inject(Confirmar);
  private readonly dialog = inject(MatDialog);
  private readonly anunciar = inject(LiveAnnouncer);
  protected readonly esAdmin = inject(AuthService).tieneRol('ADMIN');

  protected readonly grupos = signal<GrupoDuplicados[]>([]);
  protected readonly cargando = signal(false);
  protected readonly error = signal(false);
  /** Clave del grupo con una acción en curso. */
  protected readonly ocupado = signal<string | null>(null);
  protected readonly filas = filasComparacion;
  /** Por grupo, los ids desmarcados ("no es duplicado"); todos arrancan marcados. */
  private readonly desmarcados = signal<Record<string, number[]>>({});

  constructor() {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.api.get<GrupoDuplicados[]>('/establecimientos/duplicados').subscribe({
      next: (g) => {
        this.grupos.set(g);
        this.desmarcados.set({});
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        this.error.set(true);
        this.avisos.error(e);
      },
    });
  }

  private cuerpo(g: GrupoDuplicados) {
    return { clave: g.clave, integrantes: g.establecimientos.map((e) => e.id) };
  }

  protected marcado(g: GrupoDuplicados, e: DuplicadoItem): boolean {
    return !(this.desmarcados()[g.clave] ?? []).includes(e.id);
  }

  protected alternar(g: GrupoDuplicados, e: DuplicadoItem): void {
    this.desmarcados.update((d) => {
      const actuales = d[g.clave] ?? [];
      return { ...d, [g.clave]: actuales.includes(e.id) ? actuales.filter((x) => x !== e.id) : [...actuales, e.id] };
    });
  }

  protected marcadosDe(g: GrupoDuplicados): DuplicadoItem[] {
    return g.establecimientos.filter((e) => this.marcado(g, e));
  }

  protected cantidadMarcados(g: GrupoDuplicados): number {
    return this.marcadosDe(g).length;
  }

  protected noSonDuplicados(g: GrupoDuplicados): void {
    const nombre = g.establecimientos[0].nombre;
    this.confirmar
      .pedir('No son duplicados',
        `Los ${g.establecimientos.length} registros de "${nombre}" se marcan como distintos (p. ej. sucursales) y el grupo deja de `
        + 'aparecer. Si más adelante se suma o se da de baja uno con ese nombre, vuelve a aparecer para revisarlo.',
        'No son duplicados')
      .subscribe((ok) => ok && this.enviar(g, '/establecimientos/duplicados/descartes', this.cuerpo(g),
        `"${nombre}" marcado como no duplicado`));
  }

  /** Muestra quiénes se dan de baja y qué le falta al que queda; la copia es opcional (ver ResolverDialog). */
  protected quedarmeCon(g: GrupoDuplicados, queda: DuplicadoItem): void {
    const marcados = this.marcadosDe(g);
    if (marcados.length < 2 || !marcados.some((e) => e.id === queda.id)) {
      this.avisos.ok('Marcá al menos dos registros, incluido el que queda');
      return;
    }
    const otros = marcados.length - 1;
    const distintos = g.establecimientos.length - marcados.length;
    this.dialog
      .open<ResolverDialog, DatosResolver, Resolucion>(ResolverDialog, { data: { grupo: g, queda, marcados }, width: '560px' })
      .afterClosed()
      .subscribe((r) => {
        if (!r) {
          return;
        }
        this.enviar(g, '/establecimientos/duplicados/resolucion',
          { conservarId: queda.id, grupo: this.cuerpo(g), marcados: marcados.map((e) => e.id), ...r },
          (res) => {
            const copia = res as { contactosCopiados: number; caracteristicasCopiadas: number };
            const extra = [
              copia.contactosCopiados ? `${copia.contactosCopiados} ${copia.contactosCopiados === 1 ? 'contacto copiado' : 'contactos copiados'}` : '',
              copia.caracteristicasCopiadas ? `${copia.caracteristicasCopiadas} ${copia.caracteristicasCopiadas === 1 ? 'característica agregada' : 'características agregadas'}` : '',
            ].filter(Boolean).join(' y ');
            const sinTocar = !distintos ? '' : distintos === 1 ? '. El no marcado sigue como distinto' : `. Los ${distintos} no marcados siguen como distintos`;
            return `Quedó "${queda.nombre}"; ${otros === 1 ? 'el otro marcado quedó' : `${otros} marcados quedaron`} de baja${extra ? `. ${extra}` : ''}${sinTocar}`;
          });
      });
  }

  private enviar(g: GrupoDuplicados, ruta: string, cuerpo: unknown, mensaje: string | ((respuesta: unknown) => string)): void {
    this.ocupado.set(g.clave);
    this.api.post(ruta, cuerpo).subscribe({
      next: (respuesta) => {
        this.ocupado.set(null);
        mensaje = typeof mensaje === 'string' ? mensaje : mensaje(respuesta);
        this.avisos.ok(mensaje);
        this.anunciar.announce(`${mensaje}. Quedan ${this.grupos().length - 1} grupos por revisar.`, 'polite');
        this.grupos.update((gs) => gs.filter((x) => x.clave !== g.clave));
      },
      error: (e) => {
        this.ocupado.set(null);
        this.avisos.error(e);
        if (e instanceof HttpErrorResponse && e.status === 409) {
          this.cargar(); // el grupo cambió: se muestra como está ahora
        }
      },
    });
  }
}
