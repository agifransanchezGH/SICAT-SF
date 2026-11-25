import { Injectable, inject } from '@angular/core';
import { NavigationCancel, NavigationEnd, NavigationError, Router } from '@angular/router';
import { filter } from 'rxjs';

/** Ruta sin la query ni el fragmento: cambiar de pestaña o de filtro no es "otra página". */
const ruta = (url: string) => url.split(/[?#]/)[0];

/**
 * Pila de páginas visitadas dentro del panel, para que las flechas de "volver" lleven a la página de la que se
 * vino (el mapa, una cola, la ficha del oferente…) y no siempre al mismo listado. Los cambios de query en la misma
 * página (pestaña de la ficha, filtros) reemplazan la entrada en vez de sumar otra, así "volver" no recorre
 * pestañas. Sin página anterior (se entró por un enlace directo), cada pantalla usa su destino por defecto.
 */
@Injectable({ providedIn: 'root' })
export class HistorialNavegacion {
  private readonly router = inject(Router);
  private readonly pila: string[] = [];
  private volviendo = false;

  constructor() {
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd || e instanceof NavigationCancel || e instanceof NavigationError))
      .subscribe((e) => {
        if (e instanceof NavigationEnd) {
          this.registrar(e.urlAfterRedirects);
        } else {
          this.volviendo = false; // se canceló (p. ej. cambios sin guardar): la pila queda como estaba
        }
      });
  }

  /** Agrega la página a la pila (o reemplaza la última si es la misma ruta con otra query). */
  registrar(url: string): void {
    if (this.volviendo) {
      this.volviendo = false;
      this.pila.pop(); // la página de la que se volvió
    }
    const ultima = this.pila.at(-1);
    if (ultima !== undefined && ruta(ultima) === ruta(url)) {
      this.pila[this.pila.length - 1] = url;
    } else {
      this.pila.push(url);
      if (this.pila.length > 50) {
        this.pila.shift();
      }
    }
  }

  /** La página anterior dentro del panel, o null si se entró directo a esta. */
  anterior(): string | null {
    return this.pila.length > 1 ? this.pila[this.pila.length - 2] : null;
  }

  /** Vuelve a la página anterior; si no hay, va al destino por defecto de la pantalla. */
  volver(porDefecto: string): void {
    const anterior = this.anterior();
    if (anterior) {
      this.volviendo = true;
      this.router.navigateByUrl(anterior);
    } else {
      this.router.navigateByUrl(porDefecto);
    }
  }
}
