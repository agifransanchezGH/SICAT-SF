import { DatePipe, Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, input, numberAttribute, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Title } from '@angular/platform-browser';
import { Router, RouterLink } from '@angular/router';
import * as L from 'leaflet';
import { Caracteristica, DIAS_SEMANA, EstablecimientoFicha, Horario, SicatMapa, enlaceDe, iconoPin, nombreLegible } from 'shared';

import { ApiPublicaService } from '../core/api-publica.service';


/** Agrupa franjas por día: "Lunes: 09:00–13:00, 17:00–21:00". */
export function horariosPorDia(horarios: Horario[] | undefined): { dia: string; franjas: string }[] {
  const porDia = new Map<number, string[]>();
  [...(horarios ?? [])]
    .sort((a, b) => a.diaSemana - b.diaSemana || a.desde.localeCompare(b.desde))
    .forEach((h) => porDia.set(h.diaSemana, [...(porDia.get(h.diaSemana) ?? []), `${h.desde}–${h.hasta}`]));
  return [...porDia.entries()].map(([d, f]) => ({ dia: DIAS_SEMANA[d], franjas: f.join(', ') }));
}

/** Ficha pública de un establecimiento: bloque común y sus ofertas con el detalle de cada una. */
@Component({
  selector: 'app-ficha-page',
  imports: [RouterLink, DatePipe, SicatMapa, MatIconModule, MatButtonModule, MatProgressBarModule],
  template: `
    <div class="contenido">
      <a class="volver" href="/" (click)="$event.preventDefault(); volver()"><mat-icon aria-hidden="true">arrow_back</mat-icon> Volver</a>

      @if (nombreMostrado(); as nombre) {
        <header class="cabecera">
          @if (categorias().length) { <p class="categorias">{{ categorias().join(', ') }}</p> }
          <h1 class="nombre-lugar" [style.view-transition-name]="'nombre-' + id()">{{ legible(nombre) }}</h1>
          @if (ficha()?.destacado) { <p class="destacado"><mat-icon inline aria-hidden="true">star</mat-icon> Destacado</p> }
        </header>
      }

      @if (ficha(); as f) {

        @if (cerrado()) {
          <div class="aviso" role="status">
            <mat-icon aria-hidden="true">info</mat-icon>
            <div>
              <strong>{{ f.estadoFuncionamiento }}</strong>
              @if (f.motivoEstado) { <span>: {{ f.motivoEstado }}</span> }
              @if (f.fechaReapertura) { <div>Reabre el {{ f.fechaReapertura | date: 'd/M/yyyy' }}</div> }
            </div>
          </div>
        }

        <div class="columnas">
          <div class="principal">
            @if (f.descripcion) {
              <section class="bloque"><p class="descripcion">{{ f.descripcion }}</p></section>
            }

            @for (o of f.ofertas ?? []; track o.id) {
              <section class="bloque">
                <h2>{{ o.categoria }}</h2>
                @if (o.categoriasAdicionales?.length) { <p class="suave">También: {{ o.categoriasAdicionales!.join(', ') }}</p> }
                @if (o.acceso || o.publicoAdmitido || o.frecuencia) {
                  <dl class="datos">
                    @if (o.acceso) { <dt>Acceso</dt><dd>{{ o.acceso }}</dd> }
                    @if (o.publicoAdmitido) { <dt>Público</dt><dd>{{ o.publicoAdmitido }}</dd> }
                    @if (o.frecuencia) { <dt>Cuándo</dt><dd>{{ o.frecuencia }}</dd> }
                  </dl>
                }
                @if (o.horarios?.length) {
                  <h3>Horarios</h3>
                  <table class="horarios">
                    @for (h of horarios(o.horarios); track h.dia) { <tr><th>{{ h.dia }}</th><td>{{ h.franjas }}</td></tr> }
                  </table>
                }

                @if (o.detalle?.alojamiento; as a) {
                  <h3>Alojamiento</h3>
                  <ul class="lista">
                    @if (a.estrellas) { <li>{{ '★'.repeat(a.estrellas) }} ({{ a.estrellas }} estrellas)</li> }
                    @if (a.cantHabitaciones) { <li>{{ a.cantHabitaciones }} habitaciones</li> }
                    @if (a.totalPlazas) { <li>{{ a.totalPlazas }} plazas</li> }
                    @if (a.habAccesibles) { <li>{{ a.habAccesibles }} habitaciones accesibles</li> }
                    @if (a.plazasAccesibles) { <li>{{ a.plazasAccesibles }} plazas accesibles</li> }
                  </ul>
                }

                @if (o.detalle?.salones?.length) {
                  <h3>Salones</h3>
                  <table class="salones">
                    <tr><th>Salón</th><th>Auditorio</th><th>Banquete</th><th>Cóctel</th><th>m²</th></tr>
                    @for (s of o.detalle!.salones!; track s.nombre) {
                      <tr><td>{{ s.nombre }}</td><td>{{ s.capAuditorio ?? '—' }}</td><td>{{ s.capBanquete ?? '—' }}</td><td>{{ s.capCoctel ?? '—' }}</td><td>{{ s.m2 ?? '—' }}</td></tr>
                    }
                  </table>
                }

                @if (o.detalle?.visitasGuiadas?.length) {
                  <h3>Visitas guiadas</h3>
                  @for (v of o.detalle!.visitasGuiadas!; track $index) {
                    <div class="visita">
                      <strong>{{ v.modalidad || 'Visita guiada' }}</strong>
                      @if (v.requiereReserva) { <span class="chip">Con reserva</span> }
                      @for (h of horarios(v.horarios); track h.dia) { <div class="suave">{{ h.dia }}: {{ h.franjas }}</div> }
                    </div>
                  }
                }

                @if (o.detalle?.circuito; as c) {
                  <h3>Circuito</h3>
                  <p>
                    @if (c.lugarPartida) { Parte desde {{ c.lugarPartida }}. }
                    @if (c.duracionMin) { Duración aproximada: {{ duracion(c.duracionMin) }}. }
                  </p>
                  @if (c.paradas?.length) {
                    <ol class="paradas">
                      @for (p of c.paradas!; track p.orden) {
                        <li><a [routerLink]="['/establecimientos', p.establecimientoId]" [state]="{ nombre: p.nombre }">{{ legible(p.nombre) }}</a></li>
                      }
                    </ol>
                  }
                }
              </section>
            }

            @if (gruposCaracteristicas().length) {
              <section class="bloque">
                <h2>Servicios y características</h2>
                @for (g of gruposCaracteristicas(); track g.grupo) {
                  <h3>{{ g.grupo }}</h3>
                  <ul class="lista columnas-lista">
                    @for (c of g.items; track c.nombre) {
                      <li><mat-icon inline aria-hidden="true">check</mat-icon> {{ c.nombre }}{{ c.cantidad ? ': ' + c.cantidad : '' }}</li>
                    }
                  </ul>
                }
              </section>
            }
          </div>

          <aside class="lateral">
            <div class="panel">
            <section class="bloque-lateral">
              <h2>Dónde queda</h2>
              @if (f.lugar && f.lugar !== f.nombre && f.lugar !== f.direccion) { <p>{{ f.lugar }}</p> }
              <p>{{ unir(f.direccion, f.localidad) || 'Sin dirección cargada' }}</p>
              @if (f.coordenadas; as c) {
                <sicat-mapa class="mini-mapa" [centro]="[c.lat, c.lng]" [zoom]="16" (listo)="ubicar($event)" />
                <a mat-flat-button target="_blank" rel="noopener"
                   [href]="'https://www.openstreetmap.org/directions?to=' + c.lat + '%2C' + c.lng + '#map=17/' + c.lat + '/' + c.lng">
                  <mat-icon>directions</mat-icon> Cómo llegar
                </a>
              }
            </section>

            @if (contactos().length) {
              <section class="bloque-lateral">
                <h2>Contacto</h2>
                <ul class="contactos">
                  @for (c of contactos(); track $index) {
                    <li>
                      <mat-icon aria-hidden="true">{{ c.enlace.icono }}</mat-icon>
                      <span>
                        @if (c.enlace.href) { <a [href]="c.enlace.href" target="_blank" rel="noopener">{{ c.enlace.texto }}</a> }
                        @else { {{ c.enlace.texto }} }
                        <small class="suave">{{ c.tipo }}{{ c.uso ? ', ' + c.uso : '' }}</small>
                      </span>
                    </li>
                  }
                </ul>
              </section>
            }

            </div>
            @if (f.distinciones?.length) {
              <section class="bloque-lateral">
                <h2>Distinciones</h2>
                <ul class="lista">
                  @for (d of f.distinciones!; track d.nombre) {
                    <li>
                      <mat-icon inline aria-hidden="true">workspace_premium</mat-icon>
                      @if (d.urlVerificacion) { <a [href]="d.urlVerificacion" target="_blank" rel="noopener">{{ d.nombre }}</a> } @else { {{ d.nombre }} }
                      {{ d.anio ? '(' + d.anio + ')' : '' }}
                      @if (d.organismoOtorgante) { <div class="suave">{{ d.organismoOtorgante }}</div> }
                    </li>
                  }
                </ul>
              </section>
            }

            @if (f.anioInauguracion) { <p class="suave nota">Desde {{ f.anioInauguracion }}</p> }
          </aside>
        </div>
      } @else if (noEncontrado()) {
        <div class="vacio">
          <p>Este lugar no está publicado o ya no existe.</p>
          <a mat-flat-button routerLink="/">Ver todos los lugares</a>
        </div>
      } @else if (error()) {
        <p class="vacio">No se pudo cargar la información. Volvé a intentar en unos minutos.</p>
      } @else {
        <mat-progress-bar mode="indeterminate" aria-label="Cargando" />
      }
    </div>
  `,
  styles: `
    .volver { display: inline-flex; align-items: center; gap: 6px; margin: 0 0 20px; font-weight: 500; text-decoration: none; }
    .volver:hover { text-decoration: underline; }
    .volver mat-icon { font-size: 20px; width: 20px; height: 20px; }
    .cabecera { margin-bottom: 28px; max-width: 900px; }
    .categorias { margin: 0 0 6px; font-weight: 600; color: var(--sicat-rio); }
    h1 { margin: 0; font-size: clamp(30px, 4.6vw, 52px); line-height: 1.04; }
    .destacado { margin: 10px 0 0; font-weight: 600; color: var(--sicat-limo-texto); }
    .destacado mat-icon { vertical-align: -2px; }
    .aviso { display: flex; gap: 10px; padding: 12px 16px; margin-bottom: 24px; border-radius: 8px; background: #f6ecd8; color: var(--sicat-alerta); max-width: 760px; }
    .columnas { display: grid; grid-template-columns: minmax(0, 1fr) 340px; gap: 48px; align-items: start; }
    .principal { display: flex; flex-direction: column; min-width: 0; }
    .bloque { padding: 20px 0 24px; border-top: 1px solid var(--sicat-borde); max-width: 72ch; }
    .bloque:first-child { border-top: 2px solid var(--sicat-tinta); }
    .bloque > :first-child { margin-top: 0; }
    .bloque > :last-child { margin-bottom: 0; }
    h2 { font-size: 22px; font-weight: 700; font-stretch: var(--sicat-ancho-nombre); margin: 0 0 10px; line-height: 1.2; }
    h3 { font-size: 15px; font-weight: 700; margin: 20px 0 6px; color: var(--sicat-tinta); }
    .descripcion { white-space: pre-line; font-size: 17px; line-height: 1.6; }
    .datos { display: grid; grid-template-columns: max-content 1fr; gap: 4px 20px; margin: 8px 0; }
    dt { color: var(--sicat-texto-suave); }
    dd { margin: 0; }
    .horarios { border-collapse: collapse; }
    .horarios th { text-align: left; font-weight: 600; padding: 3px 20px 3px 0; }
    .horarios td { font-variant-numeric: tabular-nums; }
    .salones { border-collapse: collapse; width: 100%; font-size: 15px; font-variant-numeric: tabular-nums; }
    .salones th, .salones td { text-align: left; padding: 6px 8px 6px 0; border-bottom: 1px solid var(--sicat-borde); }
    .salones th { font-weight: 600; }
    .lista { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 4px; }
    .columnas-lista { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 4px 16px; }
    .lista mat-icon { color: var(--sicat-camalote); vertical-align: -3px; }
    .visita { padding: 4px 0; }
    .visita strong { font-weight: 600; }
    .visita .chip { margin-left: 8px; }
    .paradas { margin: 0; padding-left: 22px; }
    .paradas li { padding: 2px 0; }
    .lateral { display: flex; flex-direction: column; gap: 16px; position: sticky; top: 16px; }
    .panel { background: var(--mat-sys-surface); border: 1px solid var(--sicat-borde); border-radius: 8px; overflow: hidden; }
    .bloque-lateral { padding: 18px 20px 20px; }
    .panel .bloque-lateral + .bloque-lateral { border-top: 1px solid var(--sicat-borde); }
    .bloque-lateral h2 { font-size: 18px; margin-bottom: 8px; }
    .bloque-lateral p { margin: 0 0 4px; }
    .contactos { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 12px; }
    .contactos li { display: flex; gap: 10px; align-items: flex-start; overflow-wrap: anywhere; }
    .contactos mat-icon { color: var(--sicat-rio); flex: none; }
    .contactos small { display: block; }
    .nota { margin: 0 4px; font-size: 14px; }
    .mini-mapa { height: 200px; border-radius: 6px; overflow: hidden; margin: 12px 0; }
    .vacio p { color: var(--sicat-tinta); font-size: 17px; }
    @media (max-width: 900px) {
      .columnas { grid-template-columns: 1fr; gap: 24px; }
      .lateral { position: static; order: -1; }
    }
  `,
})
export class FichaPage {
  private readonly api = inject(ApiPublicaService);
  private readonly location = inject(Location);
  private readonly router = inject(Router);
  private readonly titulo = inject(Title);

  readonly id = input.required({ transform: numberAttribute });

  protected readonly unir = (...partes: (string | undefined)[]) => partes.filter(Boolean).join(", ");
  protected readonly ficha = signal<EstablecimientoFicha | null>(null);
  protected readonly noEncontrado = signal(false);
  protected readonly error = signal(false);
  protected readonly horarios = horariosPorDia;
  protected readonly legible = nombreLegible;
  /** El nombre que trae el listado (estado de la navegación): se muestra antes de que llegue la ficha. */
  private readonly nombrePrevio = signal<string | null>(history.state?.nombre ?? null);
  protected readonly nombreMostrado = computed(() => this.ficha()?.nombre ?? this.nombrePrevio());

  protected readonly categorias = computed(() => {
    const f = this.ficha();
    return [...new Set((f?.ofertas ?? []).flatMap((o) => [o.categoria, ...(o.categoriasAdicionales ?? [])]))];
  });

  protected readonly cerrado = computed(() => {
    const e = this.ficha()?.estadoFuncionamiento?.toLowerCase();
    return !!e && !e.startsWith('abierto') && !e.startsWith('en funcionamiento') && e !== 'activo';
  });

  protected readonly contactos = computed(() => (this.ficha()?.contactos ?? []).map((c) => ({ ...c, enlace: enlaceDe(c.tipo, c.valor) })));

  protected readonly gruposCaracteristicas = computed(() => {
    const mapa = new Map<string, Caracteristica[]>();
    for (const c of this.ficha()?.caracteristicas ?? []) {
      mapa.set(c.grupo, [...(mapa.get(c.grupo) ?? []), c]);
    }
    return [...mapa.entries()].map(([grupo, items]) => ({ grupo, items }));
  });

  constructor() {
    effect(() => this.cargar(this.id()));
  }

  private cargar(id: number): void {
    this.ficha.set(null);
    this.noEncontrado.set(false);
    this.error.set(false);
    this.api.ficha(id).subscribe({
      next: (f) => {
        this.ficha.set(f);
        this.titulo.setTitle(`${nombreLegible(f.nombre)}, Santa Fe`);
      },
      error: (e: unknown) => {
        this.nombrePrevio.set(null);
        (e instanceof HttpErrorResponse && e.status === 404 ? this.noEncontrado : this.error).set(true);
      },
    });
  }

  /** Marca el establecimiento y, si alguna oferta es un circuito con trazado, lo dibuja. */
  protected ubicar(m: L.Map): void {
    const f = this.ficha();
    if (!f?.coordenadas) {
      return;
    }
    L.marker([f.coordenadas.lat, f.coordenadas.lng], { icon: iconoPin(), title: nombreLegible(f.nombre), alt: nombreLegible(f.nombre) }).addTo(m);
    const trazados = (f.ofertas ?? [])
      .map((o) => o.detalle?.circuito?.trazado)
      .filter((t): t is GeoJSON.LineString => !!t);
    if (trazados.length) {
      const capa = L.geoJSON(trazados, { style: { color: '#3f6a2e', weight: 4 } }).addTo(m);
      m.fitBounds(capa.getBounds().extend([f.coordenadas.lat, f.coordenadas.lng]), { padding: [20, 20] });
    }
  }

  protected duracion(min: number): string {
    const h = Math.floor(min / 60);
    const m = min % 60;
    return h ? `${h} h${m ? ` ${m} min` : ''}` : `${m} min`;
  }

  /** Vuelve al listado con los filtros que tenía; si se entró directo a la ficha, va al inicio. */
  protected volver(): void {
    if ((history.state?.navigationId ?? 1) > 1) {
      this.location.back();
    } else {
      this.router.navigate(['/']);
    }
  }
}
