import { Component, ElementRef, computed, forwardRef, inject, input, output, signal, viewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Subject, catchError, debounceTime, of, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { Api, Parametros } from './api';
import { Pagina } from './modelos';

export interface OpcionRemota {
  id: number;
  nombre: string;
  detalle?: string;
}

/**
 * Campo con autocompletado contra un listado paginado de la API (oferentes, lugares, establecimientos).
 * El valor del control es el id; "etiqueta" es el texto a mostrar para el valor inicial.
 */
@Component({
  selector: 'app-selector-remoto',
  imports: [MatFormFieldModule, MatInputModule, MatAutocompleteModule, MatIconModule, MatButtonModule],
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => SelectorRemoto), multi: true }],
  template: `
    <mat-form-field class="campo">
      <mat-label>{{ etiquetaCampo() }}</mat-label>
      <input #entrada matInput [matAutocomplete]="auto" [value]="texto()" [disabled]="deshabilitado()"
             (input)="buscar(entrada.value)" (focus)="buscar(entrada.value)" (blur)="tocado()" />
      @if (valor() !== null && !deshabilitado()) {
        <button matSuffix mat-icon-button type="button" aria-label="Quitar" (click)="limpiar()"><mat-icon>close</mat-icon></button>
      }
      <mat-autocomplete #auto="matAutocomplete" (optionSelected)="elegir($event)">
        @for (o of opciones(); track o.id) {
          <mat-option [value]="o">
            {{ o.nombre }}
            @if (o.detalle) { <small class="suave"> · {{ o.detalle }}</small> }
          </mat-option>
        }
      </mat-autocomplete>
      @if (ayuda()) { <mat-hint>{{ ayuda() }}</mat-hint> }
    </mat-form-field>
  `,
  styles: `:host { display: block; } .campo { width: 100%; }`,
})
export class SelectorRemoto implements ControlValueAccessor {
  private readonly api = inject(Api);

  readonly ruta = input.required<string>();
  readonly etiquetaCampo = input('Buscar', { alias: 'etiqueta' });
  readonly textoInicial = input<string | null | undefined>(null);
  readonly parametros = input<Parametros>({});
  readonly detalle = input<(item: any) => string | undefined>(() => undefined);
  readonly ayuda = input<string>('');
  /** Además del valor del control, avisa qué opción se eligió (con su nombre). */
  readonly seleccion = output<OpcionRemota>();

  protected readonly entrada = viewChild<ElementRef<HTMLInputElement>>('entrada');
  protected readonly opciones = signal<OpcionRemota[]>([]);
  protected readonly valor = signal<number | null>(null);
  private readonly elegido = signal<string | null>(null);
  protected readonly texto = computed(() => {
    const v = this.valor();
    return v == null ? '' : (this.elegido() ?? this.textoInicial() ?? `#${v}`);
  });
  protected readonly deshabilitado = signal(false);

  private readonly consultas = new Subject<string>();
  private onChange: (v: number | null) => void = () => {};
  private onTouched: () => void = () => {};

  constructor() {
    this.consultas
      .pipe(
        debounceTime(250),
        switchMap((q) =>
          this.api
            .get<Pagina<{ id: number; nombre: string }>>(this.ruta(), { ...this.parametros(), q, size: 15 })
            .pipe(catchError(() => of(null))),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((p) =>
        this.opciones.set((p?.contenido ?? []).map((i) => ({ id: i.id, nombre: i.nombre, detalle: this.detalle()(i) }))),
      );
  }

  /** Cambia el valor desde afuera (p. ej. después de crear un lugar nuevo). */
  fijar(id: number, nombre: string): void {
    this.valor.set(id);
    this.elegido.set(nombre);
    this.onChange(id);
  }

  protected buscar(q: string): void {
    this.consultas.next(q);
  }

  protected elegir(e: MatAutocompleteSelectedEvent): void {
    const o = e.option.value as OpcionRemota;
    this.fijar(o.id, o.nombre);
    this.seleccion.emit(o);
    const el = this.entrada()?.nativeElement;
    if (el) {
      el.value = o.nombre;
    }
  }

  limpiar(): void {
    this.valor.set(null);
    this.elegido.set(null);
    const el = this.entrada()?.nativeElement;
    if (el) {
      el.value = '';
    }
    this.onChange(null);
  }

  protected tocado(): void {
    // Si se escribió algo sin elegir, se vuelve a mostrar el valor elegido.
    const el = this.entrada()?.nativeElement;
    if (el && el.value !== this.texto()) {
      setTimeout(() => (el.value = this.texto()), 150);
    }
    this.onTouched();
  }

  writeValue(v: number | null): void {
    this.valor.set(v ?? null);
    this.elegido.set(null);
  }

  registerOnChange(fn: (v: number | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(d: boolean): void {
    this.deshabilitado.set(d);
  }
}
