import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, debounceTime, map, of } from 'rxjs';
import { CategoriaPublica } from 'shared';

import { ApiPublicaService } from './api-publica.service';

interface OpcionCategoria {
  id: number;
  nombre: string;
  nivel: number;
}

function aplanar(nodos: CategoriaPublica[], nivel = 0): OpcionCategoria[] {
  return nodos.flatMap((n) => [{ id: n.id, nombre: n.nombre, nivel }, ...aplanar(n.hijos ?? [], nivel + 1)]);
}

/** Buscador y filtros (texto, categoría, localidad). Escribe en la URL; las páginas leen de ahí. */
@Component({
  selector: 'app-filtros-busqueda',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatIconModule, MatButtonModule],
  template: `
    <form class="filtros" [formGroup]="filtros" (ngSubmit)="$event.preventDefault()">
      <mat-form-field class="busqueda">
        <mat-label>Buscar</mat-label>
        <mat-icon matPrefix>search</mat-icon>
        <input matInput formControlName="q" placeholder="Ej.: parrilla, museo, hotel" />
        @if (filtros.controls.q.value) {
          <button matSuffix mat-icon-button type="button" aria-label="Borrar búsqueda" (click)="filtros.controls.q.setValue('')"><mat-icon>close</mat-icon></button>
        }
      </mat-form-field>
      <mat-form-field>
        <mat-label>Categoría</mat-label>
        <mat-select formControlName="categoria">
          <mat-option value="">Todas</mat-option>
          @for (c of categorias(); track c.id) {
            <mat-option [value]="'' + c.id" [class.subcategoria]="c.nivel > 0">{{ c.nombre }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
      @if (localidades().length > 1) {
        <mat-form-field>
          <mat-label>Localidad</mat-label>
          <mat-select formControlName="localidad">
            <mat-option value="">Todas</mat-option>
            @for (l of localidades(); track l.id) { <mat-option [value]="'' + l.id">{{ l.nombre }}</mat-option> }
          </mat-select>
        </mat-form-field>
      }
    </form>
  `,
  styles: `
    .filtros { display: flex; gap: 12px; flex-wrap: wrap; }
    .filtros mat-form-field { flex: 1 1 200px; }
    .filtros .busqueda { flex: 3 1 300px; }
    ::ng-deep .subcategoria { padding-left: 32px !important; }
  `,
})
export class FiltrosBusqueda {
  private readonly api = inject(ApiPublicaService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly categorias = toSignal(this.api.categorias().pipe(map((a) => aplanar(a)), catchError(() => of([]))), {
    initialValue: [] as OpcionCategoria[],
  });
  protected readonly localidades = toSignal(this.api.localidades().pipe(catchError(() => of([]))), { initialValue: [] });

  private readonly inicial = this.route.snapshot.queryParamMap;
  protected readonly filtros = inject(NonNullableFormBuilder).group({
    q: this.inicial.get('q') ?? '',
    categoria: this.inicial.get('categoria') ?? '',
    localidad: this.inicial.get('localidad') ?? '',
  });

  constructor() {
    this.filtros.valueChanges.pipe(debounceTime(350), takeUntilDestroyed(inject(DestroyRef))).subscribe((f) =>
      this.router.navigate([], {
        queryParams: { q: f.q || null, categoria: f.categoria || null, localidad: f.localidad || null, pagina: null },
        queryParamsHandling: 'merge',
        replaceUrl: true,
      }),
    );
  }
}
