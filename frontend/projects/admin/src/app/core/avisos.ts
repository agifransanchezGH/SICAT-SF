import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

/** Mensajes al usuario. Traduce los errores RFC 7807 de la API a un texto legible. */
@Injectable({ providedIn: 'root' })
export class Avisos {
  private readonly snack = inject(MatSnackBar);

  ok(mensaje: string): void {
    this.snack.open(mensaje, 'Cerrar', { duration: 3000 });
  }

  error(e: unknown, prefijo?: string): void {
    const texto = mensajeDeError(e);
    this.snack.open(prefijo ? `${prefijo}: ${texto}` : texto, 'Cerrar', { duration: 8000, panelClass: 'aviso-error' });
  }
}

/** "detail" del Problem Details, más la lista "errores" si viene; o un mensaje genérico. */
export function mensajeDeError(e: unknown): string {
  if (e instanceof HttpErrorResponse) {
    const p = e.error as { detail?: string; title?: string; errores?: string[] } | null;
    if (e.status === 0) {
      return 'No se pudo conectar con el servidor';
    }
    if (e.status === 403) {
      return 'No tenés permiso para hacer esto';
    }
    const base = p?.detail ?? p?.title ?? `Error ${e.status}`;
    return p?.errores?.length ? `${base}: ${p.errores.join(' · ')}` : base;
  }
  return 'Ocurrió un error inesperado';
}

/** Lista de errores del Problem Details (p. ej. requisitos de publicación faltantes). */
export function erroresDe(e: unknown): string[] {
  return e instanceof HttpErrorResponse ? ((e.error as { errores?: string[] } | null)?.errores ?? []) : [];
}
