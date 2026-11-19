import { Injectable, inject, signal } from '@angular/core';
import { EMPTY, Subject, catchError, debounceTime, switchMap } from 'rxjs';

import { Api } from './api';
import { ContadoresPanel } from './modelos';

/**
 * Avisa que se escribió algo en la API (POST, PUT, PATCH o DELETE con éxito). Lo emite el interceptor;
 * así los contadores del menú se actualizan tras cualquier acción sin que cada pantalla lo pida.
 */
@Injectable({ providedIn: 'root' })
export class CambiosEnDatos {
  readonly ocurrio = new Subject<void>();
}

/** Pendientes de trabajo del menú (GET /contadores). Se piden de a uno aunque lleguen varios avisos juntos. */
@Injectable({ providedIn: 'root' })
export class Contadores {
  private readonly api = inject(Api);
  private readonly pedidos = new Subject<void>();
  readonly valores = signal<ContadoresPanel | null>(null);

  constructor() {
    this.pedidos
      .pipe(
        debounceTime(400),
        // Un error no rompe el menú: queda el último valor conocido.
        switchMap(() => this.api.get<ContadoresPanel>('/contadores').pipe(catchError(() => EMPTY))),
      )
      .subscribe((c) => this.valores.set(c));
    inject(CambiosEnDatos).ocurrio.subscribe(() => this.refrescar());
  }

  refrescar(): void {
    this.pedidos.next();
  }
}
