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
import { ayudaDe, enlaceDe, problemaDe } from 'shared';

import { avisarSiHayCambios } from '../core/cambios-pendientes';

/** Canales de contacto de un establecimiento o de un oferente. Se guarda la lista completa (PUT). */
@Component({
  selector: 'app-contactos-editor',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatCheckboxModule, MatButtonModule, MatIconModule],
  template: `
    <div class="lista-editable" style="--columnas: minmax(130px, 170px) minmax(200px, 2fr) minmax(140px, 1fr) 84px 88px">
      <p class="lista-ayuda">Los canales marcados como "Público" se muestran en el sitio; el resto queda para el equipo. "Uso" dice para qué sirve cada canal: reservas, administración…</p>
      @if (filas.length) {
        <div class="te-cabecera" aria-hidden="true">
          <span>Tipo</span><span>Valor</span><span>Uso</span><span>Público</span><span></span>
        </div>
      }
      @for (fila of filas.controls; track fila; let i = $index) {
        <div class="te-fila" [formGroup]="fila" role="group" [attr.aria-label]="'Contacto ' + (i + 1)">
          <div data-etiqueta="Tipo">
            <mat-form-field>
              <mat-select formControlName="tipoCanalId" [aria-label]="'Tipo del contacto ' + (i + 1)">
                @for (t of tipos(); track t.id) { <mat-option [value]="t.id">{{ t.nombre }}</mat-option> }
              </mat-select>
            </mat-form-field>
          </div>
          @let tipo = nombreTipo(fila.controls.tipoCanalId.value);
          @let valor = fila.controls.valor.value;
          @let problema = problemaDe(tipo, valor);
          @let enlace = valor.trim() ? enlaceDe(tipo, valor) : null;
          <div data-etiqueta="Valor">
            <mat-form-field>
              <input matInput formControlName="valor" [attr.aria-label]="'Valor del contacto ' + (i + 1)" />
              @if (enlace?.href) {
                <a matIconSuffix mat-icon-button [href]="enlace!.href" target="_blank" rel="noopener"
                   [attr.aria-label]="'Probar enlace: ' + enlace!.href" [title]="'Probar enlace: ' + enlace!.href">
                  <mat-icon>open_in_new</mat-icon>
                </a>
              }
              @if (problema) {
                <mat-hint class="aviso">{{ problema }}</mat-hint>
              } @else if (ayudaDe(tipo); as ayuda) {
                <mat-hint>{{ ayuda }}</mat-hint>
              }
            </mat-form-field>
          </div>
          <div data-etiqueta="Uso">
            <mat-form-field>
              <input matInput formControlName="uso" [attr.aria-label]="'Uso del contacto ' + (i + 1)" />
            </mat-form-field>
          </div>
          <div class="te-control" data-etiqueta="Público">
            <mat-checkbox formControlName="esPublico" [aria-label]="'Contacto ' + (i + 1) + ' público'"><span class="solo-angosto">Público</span></mat-checkbox>
          </div>
          <div class="te-control te-acciones">
            @if (editable()) {
              <button mat-icon-button type="button" [attr.aria-label]="'Subir contacto ' + (i + 1)" [disabled]="i === 0" (click)="mover(i, -1)"><mat-icon>arrow_upward</mat-icon></button>
              <button mat-icon-button type="button" [attr.aria-label]="'Quitar contacto ' + (i + 1)" (click)="quitar(i)"><mat-icon>delete</mat-icon></button>
            }
          </div>
        </div>
      } @empty {
        <p class="te-vacio">Sin canales de contacto.</p>
      }
      @if (editable()) {
        <div class="te-pie">
          <button mat-button type="button" class="te-agregar" (click)="agregar()"><mat-icon>add</mat-icon> Agregar canal</button>
          @if (filas.dirty) { <span class="suave cambios" role="status">Hay cambios sin guardar</span> }
          <button mat-flat-button type="button" (click)="guardar()" [disabled]="filas.invalid || filas.pristine || guardando()">Guardar contactos</button>
        </div>
      }
    </div>
  `,
  styleUrl: './editores.scss',
  styles: `.cambios { font-size: 13px; }`,
})
export class ContactosEditor implements OnInit {
  private readonly avisoDeCambios = avisarSiHayCambios('Contactos', () => this.filas.dirty);
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly fb = inject(NonNullableFormBuilder);

  /** p. ej. /establecimientos/5/contactos */
  readonly ruta = input.required<string>();
  readonly editable = input(true);

  protected readonly tipos = toSignal(inject(Catalogos).de('tipo-canal'), { initialValue: [] });
  protected readonly filas = new FormArray<ReturnType<ContactosEditor['fila']>>([]);
  protected readonly guardando = signal(false);
  protected readonly enlaceDe = enlaceDe;
  protected readonly ayudaDe = ayudaDe;
  protected readonly problemaDe = problemaDe;

  protected nombreTipo(id: number | null): string {
    return this.tipos().find((t) => t.id === id)?.nombre ?? '';
  }

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
