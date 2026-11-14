import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormGroup, NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { HorariosEditor } from '../comun/horarios-editor';
import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { Confirmar } from '../core/confirmar';
import { VisitaGuiada } from '../core/modelos';

/** Visitas guiadas de la oferta (modalidad, reserva) con sus propios horarios. */
@Component({
  selector: 'app-visitas-editor',
  imports: [ReactiveFormsModule, MatExpansionModule, MatFormFieldModule, MatInputModule, MatCheckboxModule, MatButtonModule, MatIconModule, HorariosEditor],
  template: `
    <div class="panel">
      <mat-accordion multi>
        @for (v of visitas(); track v.id) {
          <mat-expansion-panel>
            <mat-expansion-panel-header>
              <mat-panel-title>{{ v.modalidad || 'Visita guiada' }}</mat-panel-title>
              <mat-panel-description>
                {{ v.habilitada ? 'Habilitada' : 'No habilitada' }}{{ v.requiereReserva ? ' · con reserva' : '' }} · {{ v.horarios.length }} franjas
              </mat-panel-description>
            </mat-expansion-panel-header>
            <ng-template matExpansionPanelContent>
              @let f = formDe(v);
              <form class="fila-form" [formGroup]="f" (ngSubmit)="guardar(v, f)">
                <mat-form-field><mat-label>Modalidad</mat-label><input matInput formControlName="modalidad" placeholder="Grupal con guía" /></mat-form-field>
                <mat-checkbox formControlName="requiereReserva">Requiere reserva</mat-checkbox>
                <mat-checkbox formControlName="habilitada">Habilitada</mat-checkbox>
                @if (editable()) {
                  <span class="espacio"></span>
                  <button mat-button type="button" (click)="eliminar(v)">Eliminar</button>
                  <button mat-flat-button type="submit" [disabled]="f.pristine">Guardar</button>
                }
              </form>
              <h4>Horarios de la visita</h4>
              <app-horarios-editor [ruta]="'/visitas-guiadas/' + v.id + '/horarios'" [editable]="editable()" />
            </ng-template>
          </mat-expansion-panel>
        } @empty {
          <p class="vacio">Sin visitas guiadas.</p>
        }
      </mat-accordion>
      @if (editable()) {
        <button mat-stroked-button class="agregar" (click)="crear()"><mat-icon>add</mat-icon> Agregar visita guiada</button>
      }
    </div>
  `,
  styles: `
    .panel { padding: 16px; }
    .fila-form { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; padding: 8px 0; }
    .fila-form mat-form-field { flex: 1 1 260px; }
    .espacio { flex: 1; }
    h4 { margin: 8px 0 0; font: var(--mat-sys-title-small); }
    .agregar { margin-top: 16px; }
  `,
})
export class VisitasEditor implements OnInit {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly confirmar = inject(Confirmar);
  private readonly fb = inject(NonNullableFormBuilder);

  readonly ofertaId = input.required<number>();
  readonly editable = input(true);

  protected readonly visitas = signal<VisitaGuiada[]>([]);
  private readonly forms = new Map<number, FormGroup>();

  ngOnInit(): void {
    this.cargar();
  }

  private cargar(): void {
    this.forms.clear();
    this.api.get<VisitaGuiada[]>(`/ofertas/${this.ofertaId()}/visitas-guiadas`).subscribe({
      next: (l) => this.visitas.set(l),
      error: (e) => this.avisos.error(e),
    });
  }

  protected formDe(v: VisitaGuiada): FormGroup {
    let f = this.forms.get(v.id);
    if (!f) {
      f = this.fb.group({ modalidad: [v.modalidad ?? ''], requiereReserva: [v.requiereReserva], habilitada: [v.habilitada] });
      if (!this.editable()) {
        f.disable();
      }
      this.forms.set(v.id, f);
    }
    return f;
  }

  protected guardar(v: VisitaGuiada, f: FormGroup): void {
    const valor = f.getRawValue();
    this.api.put<VisitaGuiada>(`/visitas-guiadas/${v.id}`, { ...valor, modalidad: valor.modalidad || null }).subscribe({
      next: (r) => {
        f.markAsPristine();
        this.visitas.update((l) => l.map((x) => (x.id === r.id ? { ...r, horarios: x.horarios } : x)));
        this.avisos.ok('Visita guiada guardada');
      },
      error: (e) => this.avisos.error(e),
    });
  }

  protected eliminar(v: VisitaGuiada): void {
    this.confirmar.pedir('Eliminar visita guiada', 'Se elimina la visita y sus horarios.', 'Eliminar').subscribe((ok) => {
      if (ok) {
        this.api.delete(`/visitas-guiadas/${v.id}`).subscribe({
          next: () => {
            this.avisos.ok('Visita guiada eliminada');
            this.cargar();
          },
          error: (e) => this.avisos.error(e),
        });
      }
    });
  }

  protected crear(): void {
    this.api.post<VisitaGuiada>(`/ofertas/${this.ofertaId()}/visitas-guiadas`, { habilitada: true }).subscribe({
      next: () => {
        this.avisos.ok('Visita guiada creada');
        this.cargar();
      },
      error: (e) => this.avisos.error(e),
    });
  }
}
