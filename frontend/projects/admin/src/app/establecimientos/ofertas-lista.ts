import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';

import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { Catalogos } from '../core/catalogos';
import { EstadoChip } from '../core/estado';
import { Oferta } from '../core/modelos';

/** Ofertas del establecimiento (lo que ofrece, cada una con su categoría) y alta de una nueva. */
@Component({
  selector: 'app-ofertas-lista',
  imports: [FormsModule, RouterLink, MatListModule, MatIconModule, MatButtonModule, MatSelectModule, MatFormFieldModule, MatCheckboxModule, EstadoChip],
  template: `
    <div class="panel">
      <mat-checkbox [(ngModel)]="incluirInactivas" (change)="cargar()">Mostrar dadas de baja</mat-checkbox>
      <mat-nav-list>
        @for (o of ofertas(); track o.id) {
          <a mat-list-item [routerLink]="['/ofertas', o.id]">
            <mat-icon matListItemIcon>category</mat-icon>
            <span matListItemTitle>{{ o.categoria }} @if (!o.activo) { <app-estado valor="inactivo" /> }</span>
            <span matListItemLine class="suave">
              {{ resumen(o) }}
            </span>
          </a>
        } @empty {
          <p class="vacio">Todavía no tiene ofertas. Para publicarlo necesita al menos una activa.</p>
        }
      </mat-nav-list>

      @if (editable()) {
        <div class="nueva">
          <mat-form-field class="categoria">
            <mat-label>Categoría de la nueva oferta</mat-label>
            <mat-select [(ngModel)]="categoriaNueva">
              @for (c of catalogos.categorias(); track c.id) { <mat-option [value]="c.id">{{ c.nombre }}</mat-option> }
            </mat-select>
          </mat-form-field>
          <button mat-flat-button [disabled]="!categoriaNueva || creando()" (click)="crear()"><mat-icon>add</mat-icon> Agregar oferta</button>
        </div>
      }
    </div>
  `,
  styles: `
    .panel { padding: 16px; }
    .nueva { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; margin-top: 8px; }
    .categoria { flex: 1 1 320px; max-width: 520px; }
  `,
})
export class OfertasLista implements OnInit {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly router = inject(Router);
  protected readonly catalogos = inject(Catalogos);

  readonly establecimientoId = input.required<number>();
  readonly editable = input(true);
  readonly cambio = output<void>();

  protected readonly ofertas = signal<Oferta[]>([]);
  protected readonly creando = signal(false);
  protected incluirInactivas = false;
  protected categoriaNueva: number | null = null;

  ngOnInit(): void {
    this.catalogos.arbolCategorias().subscribe();
    this.cargar();
  }

  protected cargar(): void {
    this.api
      .get<Oferta[]>(`/establecimientos/${this.establecimientoId()}/ofertas`, { incluirInactivas: this.incluirInactivas })
      .subscribe({ next: (l) => this.ofertas.set(l), error: (e) => this.avisos.error(e) });
  }

  protected resumen(o: Oferta): string {
    const partes = [o.acceso, o.publicoAdmitido, o.frecuencia].filter(Boolean);
    if (o.categoriasAdicionales?.length) {
      partes.push('también: ' + o.categoriasAdicionales.map((c) => c.nombre).join(', '));
    }
    return partes.join(' · ') || 'Sin detalles cargados';
  }

  protected crear(): void {
    this.creando.set(true);
    this.api.post<Oferta>(`/establecimientos/${this.establecimientoId()}/ofertas`, { categoriaId: this.categoriaNueva }).subscribe({
      next: (o) => {
        this.avisos.ok('Oferta creada');
        this.cambio.emit();
        this.router.navigate(['/ofertas', o.id]);
      },
      error: (e) => {
        this.creando.set(false);
        this.avisos.error(e);
      },
    });
  }
}
