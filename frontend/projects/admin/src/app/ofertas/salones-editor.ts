import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { CaracteristicasEditor } from '../comun/caracteristicas-editor';
import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { Confirmar } from '../core/confirmar';
import { EstadoChip } from '../core/estado';
import { Salon } from '../core/modelos';

/** Salones para eventos de la oferta, con capacidades por formato y sus características. */
@Component({
  selector: 'app-salones-editor',
  imports: [ReactiveFormsModule, MatExpansionModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, CaracteristicasEditor, EstadoChip],
  template: `
    <div class="panel">
      <mat-accordion multi>
        @for (s of salones(); track s.id) {
          <mat-expansion-panel>
            <mat-expansion-panel-header>
              <mat-panel-title>{{ s.nombre }} @if (!s.activo) { <app-estado valor="inactivo" /> }</mat-panel-title>
              <mat-panel-description>{{ capacidades(s) }}</mat-panel-description>
            </mat-expansion-panel-header>
            <ng-template matExpansionPanelContent>
              @let f = formDe(s);
              <form class="formulario" [formGroup]="f" (ngSubmit)="guardar(s, f)">
                  <mat-form-field class="ancho-completo"><mat-label>Nombre</mat-label><input matInput formControlName="nombre" /></mat-form-field>
                  <mat-form-field><mat-label>Capacidad auditorio</mat-label><input matInput type="number" min="0" formControlName="capAuditorio" /></mat-form-field>
                  <mat-form-field><mat-label>Capacidad banquete</mat-label><input matInput type="number" min="0" formControlName="capBanquete" /></mat-form-field>
                  <mat-form-field><mat-label>Capacidad cóctel</mat-label><input matInput type="number" min="0" formControlName="capCoctel" /></mat-form-field>
                  <mat-form-field><mat-label>Superficie (m²)</mat-label><input matInput type="number" min="0" formControlName="m2" /></mat-form-field>
                  <mat-form-field><mat-label>Estado de conservación</mat-label><input matInput formControlName="estadoConservacion" /></mat-form-field>
                  <mat-form-field><mat-label>Estado de funcionamiento</mat-label><input matInput formControlName="estadoFuncionamiento" /></mat-form-field>
                @if (editable()) {
                  <div class="ancho-completo botones">
                    @if (s.activo) {
                      <button mat-button type="button" (click)="darDeBaja(s)">Dar de baja</button>
                    } @else {
                      <button mat-button type="button" (click)="guardar(s, f, true)">Reactivar</button>
                    }
                    <button mat-flat-button type="submit" [disabled]="f.invalid || f.pristine">Guardar salón</button>
                  </div>
                }
              </form>
              <h4>Características del salón</h4>
              <app-caracteristicas-editor [ruta]="'/salones/' + s.id + '/caracteristicas'" [editable]="editable()" />
            </ng-template>
          </mat-expansion-panel>
        } @empty {
          <p class="vacio">Sin salones.</p>
        }
      </mat-accordion>
      @if (editable()) {
        <form class="nuevo" [formGroup]="nuevo" (ngSubmit)="crear()">
          <mat-form-field><mat-label>Nombre del nuevo salón</mat-label><input matInput formControlName="nombre" /></mat-form-field>
          <button mat-flat-button type="submit" [disabled]="nuevo.invalid"><mat-icon>add</mat-icon> Agregar salón</button>
        </form>
      }
    </div>
  `,
  styles: `
    .panel { padding: 16px; }
    .botones { display: flex; justify-content: flex-end; gap: 8px; }
    .formulario { padding: 8px 0; }
    h4 { margin: 8px 0 0; font: var(--mat-sys-title-small); }
    .nuevo { display: flex; gap: 12px; align-items: center; margin-top: 16px; flex-wrap: wrap; }
    .nuevo mat-form-field { flex: 1 1 280px; max-width: 420px; }
  `,
})
export class SalonesEditor implements OnInit {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly confirmar = inject(Confirmar);
  private readonly fb = inject(NonNullableFormBuilder);

  readonly ofertaId = input.required<number>();
  readonly editable = input(true);

  protected readonly salones = signal<Salon[]>([]);
  private readonly forms = new Map<number, FormGroup>();
  protected readonly nuevo = this.fb.group({ nombre: ['', [Validators.required, Validators.maxLength(200)]] });

  ngOnInit(): void {
    this.cargar();
  }

  private cargar(): void {
    this.forms.clear();
    this.api.get<Salon[]>(`/ofertas/${this.ofertaId()}/salones`).subscribe({
      next: (l) => this.salones.set(l),
      error: (e) => this.avisos.error(e),
    });
  }

  protected capacidades(s: Salon): string {
    const c = [
      s.capAuditorio && `${s.capAuditorio} auditorio`,
      s.capBanquete && `${s.capBanquete} banquete`,
      s.capCoctel && `${s.capCoctel} cóctel`,
      s.m2 && `${s.m2} m²`,
    ].filter(Boolean);
    return c.join(' · ');
  }

  protected formDe(s: Salon): FormGroup {
    let f = this.forms.get(s.id);
    if (!f) {
      f = this.fb.group({
        nombre: [s.nombre, [Validators.required, Validators.maxLength(200)]],
        capAuditorio: [s.capAuditorio, Validators.min(0)],
        capBanquete: [s.capBanquete, Validators.min(0)],
        capCoctel: [s.capCoctel, Validators.min(0)],
        m2: [s.m2, Validators.min(0)],
        estadoConservacion: [s.estadoConservacion ?? ''],
        estadoFuncionamiento: [s.estadoFuncionamiento ?? ''],
      });
      if (!this.editable()) {
        f.disable();
      }
      this.forms.set(s.id, f);
    }
    return f;
  }

  protected guardar(s: Salon, f: FormGroup, reactivar = false): void {
    const v = f.getRawValue();
    const cuerpo = {
      ...v,
      estadoConservacion: v.estadoConservacion || null,
      estadoFuncionamiento: v.estadoFuncionamiento || null,
      activo: reactivar ? true : s.activo,
    };
    this.api.put<Salon>(`/salones/${s.id}`, cuerpo).subscribe({
      next: (r) => {
        f.markAsPristine();
        this.salones.update((l) => l.map((x) => (x.id === r.id ? r : x)));
        this.avisos.ok('Salón guardado');
      },
      error: (e) => this.avisos.error(e),
    });
  }

  protected darDeBaja(s: Salon): void {
    this.confirmar.pedir('Dar de baja', `¿Dar de baja el salón "${s.nombre}"?`, 'Dar de baja').subscribe((ok) => {
      if (ok) {
        this.api.delete(`/salones/${s.id}`).subscribe({
          next: () => {
            this.avisos.ok('Salón dado de baja');
            this.cargar();
          },
          error: (e) => this.avisos.error(e),
        });
      }
    });
  }

  protected crear(): void {
    this.api.post<Salon>(`/ofertas/${this.ofertaId()}/salones`, { nombre: this.nuevo.getRawValue().nombre.trim() }).subscribe({
      next: () => {
        this.nuevo.reset();
        this.avisos.ok('Salón creado');
        this.cargar();
      },
      error: (e) => this.avisos.error(e),
    });
  }
}
