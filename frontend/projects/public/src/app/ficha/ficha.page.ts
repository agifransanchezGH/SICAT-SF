import { DatePipe, Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, input, numberAttribute, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Title } from '@angular/platform-browser';
import { Router, RouterLink } from '@angular/router';
import * as L from 'leaflet';
import { Caracteristica, DIAS_SEMANA, EstablecimientoFicha, Horario, SicatMapa, iconoPin } from 'shared';

import { ApiPublicaService } from '../core/api-publica.service';
import { enlaceDe } from './contacto-link';

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
      <button mat-button class="volver" (click)="volver()"><mat-icon>arrow_back</mat-icon> Volver</button>

      @if (ficha(); as f) {
        <header class="cabecera">
          <h1>{{ f.nombre }} @if (f.destacado) { <mat-icon class="destacado" title="Destacado">star</mat-icon> }</h1>
          <div class="chips">
            @for (c of categorias(); track c) { <span class="chip">{{ c }}</span> }
          </div>
        </header>

        @if (cerrado()) {
          <div class="aviso" role="status">
            <mat-icon>info</mat-icon>
            <div>
              <strong>{{ f.estadoFuncionamiento }}</strong>
              @if (f.motivoEstado) { · {{ f.motivoEstado }} }
              @if (f.fechaReapertura) { <div>Reabre el {{ f.fechaReapertura | date: 'd/M/yyyy' }}</div> }
            </div>
          </div>
        }

        <div class="columnas">
          <div class="principal">
            @if (f.descripcion) {
              <section class="tarjeta bloque"><p class="descripcion">{{ f.descripcion }}</p></section>
            }

            @for (o of f.ofertas ?? []; track o.id) {
              <section class="tarjeta bloque">
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
                  <h3><mat-icon inline>schedule</mat-icon> Horarios</h3>
                  <table class="horarios">
                    @for (h of horarios(o.horarios); track h.dia) { <tr><th>{{ h.dia }}</th><td>{{ h.franjas }}</td></tr> }
                  </table>
                }

                @if (o.detalle?.alojamiento; as a) {
                  <h3><mat-icon inline>hotel</mat-icon> Alojamiento</h3>
                  <ul class="lista">
                    @if (a.estrellas) { <li>{{ '★'.repeat(a.estrellas) }} ({{ a.estrellas }} estrellas)</li> }
                    @if (a.cantHabitaciones) { <li>{{ a.cantHabitaciones }} habitaciones</li> }
                    @if (a.totalPlazas) { <li>{{ a.totalPlazas }} plazas</li> }
                    @if (a.habAccesibles) { <li>{{ a.habAccesibles }} habitaciones accesibles</li> }
                    @if (a.plazasAccesibles) { <li>{{ a.plazasAccesibles }} plazas accesibles</li> }
                  </ul>
                }

                @if (o.detalle?.salones?.length) {
                  <h3><mat-icon inline>meeting_room</mat-icon> Salones</h3>
                  <table class="salones">
                    <tr><th>Salón</th><th>Auditorio</th><th>Banquete</th><th>Cóctel</th><th>m²</th></tr>
                    @for (s of o.detalle!.salones!; track s.nombre) {
                      <tr><td>{{ s.nombre }}</td><td>{{ s.capAuditorio ?? '—' }}</td><td>{{ s.capBanquete ?? '—' }}</td><td>{{ s.capCoctel ?? '—' }}</td><td>{{ s.m2 ?? '—' }}</td></tr>
                    }
                  </table>
                }

                @if (o.detalle?.visitasGuiadas?.length) {
                  <h3><mat-icon inline>tour</mat-icon> Visitas guiadas</h3>
                  @for (v of o.detalle!.visitasGuiadas!; track $index) {
                    <div class="visita">
                      <strong>{{ v.modalidad || 'Visita guiada' }}</strong>
                      @if (v.requiereReserva) { <span class="chip">Con reserva</span> }
                      @for (h of horarios(v.horarios); track h.dia) { <div class="suave">{{ h.dia }}: {{ h.franjas }}</div> }
                    </div>
                  }
                }

                @if (o.detalle?.circuito; as c) {
                  <h3><mat-icon inline>route</mat-icon> Circuito</h3>
                  <p>
                    @if (c.lugarPartida) { Parte desde {{ c.lugarPartida }}. }
                    @if (c.duracionMin) { Duración aproximada: {{ duracion(c.duracionMin) }}. }
                  </p>
                  @if (c.paradas?.length) {
                    <ol class="paradas">
                      @for (p of c.paradas!; track p.orden) {
                        <li><a [routerLink]="['/establecimientos', p.establecimientoId]">{{ p.nombre }}</a></li>
                      }
                    </ol>
                  }
                }
              </section>
            }

            @if (gruposCaracteristicas().length) {
              <section class="tarjeta bloque">
                <h2>Servicios y características</h2>
                @for (g of gruposCaracteristicas(); track g.grupo) {
                  <h3>{{ g.grupo }}</h3>
                  <ul class="lista columnas-lista">
                    @for (c of g.items; track c.nombre) {
                      <li><mat-icon inline>check</mat-icon> {{ c.nombre }}{{ c.cantidad ? ': ' + c.cantidad : '' }}</li>
                    }
                  </ul>
                }
              </section>
            }
          </div>

          <aside class="lateral">
            <section class="tarjeta bloque">
              <h2>Dónde queda</h2>
              @if (f.lugar && f.lugar !== f.nombre && f.lugar !== f.direccion) { <p>{{ f.lugar }}</p> }
              <p>{{ unir(f.direccion, f.localidad) || 'Sin dirección cargada' }}</p>
              @if (f.coordenadas; as c) {
                <sicat-mapa class="mini-mapa" [centro]="[c.lat, c.lng]" [zoom]="16" (listo)="ubicar($event)" />
                <a mat-stroked-button target="_blank" rel="noopener"
                   [href]="'https://www.openstreetmap.org/directions?to=' + c.lat + '%2C' + c.lng + '#map=17/' + c.lat + '/' + c.lng">
                  <mat-icon>directions</mat-icon> Cómo llegar
                </a>
              }
            </section>

            @if (contactos().length) {
              <section class="tarjeta bloque">
                <h2>Contacto</h2>
                <ul class="contactos">
                  @for (c of contactos(); track $index) {
                    <li>
                      <mat-icon>{{ c.enlace.icono }}</mat-icon>
                      <span>
                        @if (c.enlace.href) { <a [href]="c.enlace.href" target="_blank" rel="noopener">{{ c.enlace.texto }}</a> }
                        @else { {{ c.enlace.texto }} }
                        <small class="suave">{{ c.tipo }}{{ c.uso ? ' · ' + c.uso : '' }}</small>
                      </span>
                    </li>
                  }
                </ul>
              </section>
            }

            @if (f.distinciones?.length) {
              <section class="tarjeta bloque">
                <h2>Distinciones</h2>
                <ul class="lista">
                  @for (d of f.distinciones!; track d.nombre) {
                    <li>
                      <mat-icon inline>workspace_premium</mat-icon>
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
        <div class="tarjeta vacio">
          <p>Este lugar no está disponible.</p>
          <a mat-flat-button routerLink="/">Ver toda la oferta</a>
        </div>
      } @else if (error()) {
        <p class="tarjeta vacio">No pudimos cargar la información. Intentá de nuevo en unos minutos.</p>
      } @else {
        <mat-progress-bar mode="indeterminate" />
      }
    </div>
  `,
  styles: `
    .volver { margin: -8px 0 8px -8px; }
    .cabecera { margin-bottom: 16px; }
    h1 { margin: 0 0 8px; font: var(--mat-sys-headline-medium); display: flex; align-items: center; gap: 8px; }
    .destacado { color: var(--sicat-acento); }
    .chips { display: flex; gap: 6px; flex-wrap: wrap; }
    .aviso { display: flex; gap: 10px; padding: 12px 16px; margin-bottom: 16px; border-radius: 12px; background: #fff4e0; color: var(--sicat-alerta); }
    .columnas { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 16px; align-items: start; }
    .principal, .lateral { display: flex; flex-direction: column; gap: 16px; }
    .bloque { padding: 16px 20px; }
    .bloque > :first-child { margin-top: 0; }
    .bloque > :last-child { margin-bottom: 0; }
    h2 { font: var(--mat-sys-title-large); margin: 0 0 8px; }
    h3 { font: var(--mat-sys-title-small); margin: 16px 0 6px; color: var(--sicat-texto-suave); }
    h3 mat-icon { vertical-align: -3px; }
    .descripcion { white-space: pre-line; line-height: 1.55; }
    .datos { display: grid; grid-template-columns: max-content 1fr; gap: 4px 16px; margin: 8px 0; }
    dt { color: var(--sicat-texto-suave); }
    dd { margin: 0; }
    .horarios th { text-align: left; font-weight: 500; padding: 2px 16px 2px 0; }
    .salones { border-collapse: collapse; width: 100%; font-size: 14px; }
    .salones th, .salones td { text-align: left; padding: 6px 8px; border-bottom: 1px solid var(--sicat-borde); }
    .lista { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 4px; }
    .columnas-lista { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); }
    .lista mat-icon { color: var(--mat-sys-primary); vertical-align: -3px; }
    .visita { padding: 4px 0; }
    .visita .chip { margin-left: 8px; }
    .paradas { margin: 0; padding-left: 20px; }
    .contactos { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 10px; }
    .contactos li { display: flex; gap: 10px; align-items: flex-start; word-break: break-word; }
    .contactos mat-icon { color: var(--mat-sys-primary); flex: none; }
    .contactos small { display: block; }
    .nota { margin: 0 4px; font-size: 14px; }
    .mini-mapa { height: 200px; border-radius: 8px; overflow: hidden; margin: 0 0 12px; }
    @media (max-width: 860px) { .columnas { grid-template-columns: 1fr; } }
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

  protected readonly categorias = computed(() => {
    const f = this.ficha();
    return [...new Set((f?.ofertas ?? []).flatMap((o) => [o.categoria, ...(o.categoriasAdicionales ?? [])]))];
  });

  protected readonly cerrado = computed(() => {
    const e = this.ficha()?.estadoFuncionamiento?.toLowerCase();
    return !!e && !e.startsWith('abierto') && !e.startsWith('en funcionamiento') && e !== 'activo';
  });

  protected readonly contactos = computed(() => (this.ficha()?.contactos ?? []).map((c) => ({ ...c, enlace: enlaceDe(c) })));

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
        this.titulo.setTitle(`${f.nombre} · Santa Fe`);
      },
      error: (e: unknown) => (e instanceof HttpErrorResponse && e.status === 404 ? this.noEncontrado : this.error).set(true),
    });
  }

  /** Marca el establecimiento y, si alguna oferta es un circuito con trazado, lo dibuja. */
  protected ubicar(m: L.Map): void {
    const f = this.ficha();
    if (!f?.coordenadas) {
      return;
    }
    L.marker([f.coordenadas.lat, f.coordenadas.lng], { icon: iconoPin(), title: f.nombre }).addTo(m);
    const trazados = (f.ofertas ?? [])
      .map((o) => o.detalle?.circuito?.trazado)
      .filter((t): t is GeoJSON.LineString => !!t);
    if (trazados.length) {
      const capa = L.geoJSON(trazados, { style: { color: '#7b1fa2', weight: 4 } }).addTo(m);
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
