import { Component, OnInit, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormArray, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { Api } from '../core/api';
import { Avisos } from '../core/avisos';
import { Catalogos } from '../core/catalogos';
import { CanalContacto } from '../core/modelos';

/** Canales de contacto de un establecimiento o de un oferente. Se guarda la lista completa (PUT). */
@Component({
  selector: 'app-contactos-editor',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatCheckboxModule, MatButtonModule, MatIconModule],
  template: `
    <div class="editor">
      @for (fila of filas.controls; track fila; let i = $index) {
        <div class="fila" [formGroup]="fila">
          <mat-form-field class="tipo">
            <mat-label>Tipo</mat-label>
            <mat-select formControlName="tipoCanalId">
              @for (t of tipos(); track t.id) { <mat-option [value]="t.id">{{ t.nombre }}</mat-option> }
            </mat-select>
          </mat-form-field>
          <mat-form-field class="valor">
            <mat-label>Valor</mat-label>
            <input matInput formControlName="valor" />
          </mat-form-field>
          <mat-form-field class="uso">
            <mat-label>Uso</mat-label>
            <input matInput formControlName="uso" placeholder="Reservas, administración…" />
          </mat-form-field>
          <mat-checkbox formControlName="esPublico">Público</mat-checkbox>
          @if (editable()) {
            <div class="mover">
              <button mat-icon-button type="button" aria-label="Subir" [disabled]="i === 0" (click)="mover(i, -1)"><mat-icon>arrow_upward</mat-icon></button>
              <button mat-icon-button type="button" aria-label="Quitar" (click)="quitar(i)"><mat-icon>delete</mat-icon></button>
            </div>
          }
        </div>
      } @empty {
        <p class="vacio">Sin canales de contacto.</p>
      }
      @if (editable()) {
        <div class="acciones-editor">
          <button mat-stroked-button type="button" (click)="agregar()"><mat-icon>add</mat-icon> Agregar canal</button>
          <span class="espacio"></span>
          <button mat-flat-button type="button" (click)="guardar()" [disabled]="filas.invalid || filas.pristine || guardando()">Guardar contactos</button>
        </div>
      }
      <p class="suave nota">Solo los marcados como "Público" se muestran en el sitio.</p>
    </div>
  `,
  styleUrl: './editores.scss',
  styles: `.tipo { width: 170px; } .valor { flex: 2 1 220px; } .uso { flex: 1 1 160px; }`,
})
export class ContactosEditor implements OnInit {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly fb = inject(NonNullableFormBuilder);

  /** p. ej. /establecimientos/5/contactos */
  readonly ruta = input.required<string>();
  readonly editable = input(true);

  protected readonly tipos = toSignal(inject(Catalogos).de('tipo-canal'), { initialValue: [] });
  protected readonly filas = new FormArray<ReturnType<ContactosEditor['fila']>>([]);
  protected readonly guardando = signal(false);

  ngOnInit(): void {
    this.api.get<CanalContacto[]>(this.ruta()).subscribe({ next: (l) => this.cargar(l), error: (e) => this.avisos.error(e) });
  }

  private fila(c?: CanalContacto) {
    return this.fb.group({
      id: [c?.id ?? (null as number | null)],
      tipoCanalId: [c?.tipoCanalId ?? (null as number | null), Validators.required],
      valor: [c?.valor ?? '', [Validators.required, Validators.maxLength(500)]],
      uso: [c?.uso ?? ''],
      esPublico: [c?.esPublico ?? true],
    });
  }

  private cargar(lista: CanalContacto[]): void {
    this.filas.clear();
    lista.forEach((c) => this.filas.push(this.fila(c)));
    if (!this.editable()) {
      this.filas.disable();
    }
    this.filas.markAsPristine();
  }

  protected agregar(): void {
    this.filas.push(this.fila());
    this.filas.markAsDirty();
  }

  protected quitar(i: number): void {
    this.filas.removeAt(i);
    this.filas.markAsDirty();
  }

  protected mover(i: number, d: number): void {
    const c = this.filas.at(i);
    this.filas.removeAt(i);
    this.filas.insert(i + d, c);
    this.filas.markAsDirty();
  }

  protected guardar(): void {
    const cuerpo = this.filas.getRawValue().map((c, i) => ({ ...c, uso: c.uso || null, orden: i + 1 }));
    this.guardando.set(true);
    this.api.put<CanalContacto[]>(this.ruta(), cuerpo).subscribe({
      next: (l) => {
        this.guardando.set(false);
        this.cargar(l);
        this.avisos.ok('Contactos guardados');
      },
      error: (e) => {
        this.guardando.set(false);
        this.avisos.error(e);
      },
    });
  }
}
