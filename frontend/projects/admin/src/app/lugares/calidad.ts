import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

import { CalidadUbicacion, Coordenadas, Lugar, NivelCalidad } from '../core/modelos';

const TEXTO: Record<NivelCalidad, { texto: string; icono: string }> = {
  BAJA: { texto: 'Dudosa', icono: 'error_outline' },
  MEDIA: { texto: 'Revisar', icono: 'help_outline' },
  ALTA: { texto: 'Probable', icono: 'check' },
  VERIFICADA: { texto: 'Verificada', icono: 'verified' },
};

/** Señal de calidad de una ubicación: ícono y palabra (no depende solo del color); los motivos van en el título. */
@Component({
  selector: 'app-calidad',
  imports: [MatIconModule],
  template: `
    @if (calidad(); as c) {
      <span class="calidad" [class]="'calidad ' + c.nivel" [title]="c.motivos.join('. ')">
        <mat-icon aria-hidden="true">{{ textos[c.nivel].icono }}</mat-icon>{{ textos[c.nivel].texto }}
        <span class="cdk-visually-hidden">: {{ c.motivos.join('. ') }}</span>
      </span>
    }
  `,
  styles: `
    .calidad {
      display: inline-flex; align-items: center; gap: 3px; padding: 1px 8px 1px 5px; border-radius: 999px;
      font-size: 12px; font-weight: 600; white-space: nowrap; line-height: 20px;
    }
    .calidad mat-icon { font-size: 15px; width: 15px; height: 15px; }
    .BAJA { background: #f8e6e4; color: var(--sicat-error); }
    .MEDIA { background: #f6ecd8; color: var(--sicat-alerta); }
    .ALTA { background: var(--sicat-rio-claro); color: var(--sicat-rio); }
    .VERIFICADA { background: #e4eedf; color: var(--sicat-ok); }
  `,
})
export class CalidadChip {
  readonly calidad = input<CalidadUbicacion | undefined>();
  protected readonly textos = TEXTO;
}

/** Cuerpo del PUT /lugares/{id} a partir de un lugar del listado, con otras coordenadas y estado. */
export function cuerpoLugar(l: Lugar, coordenadas: Coordenadas | null, estadoGeo: string | null) {
  return {
    nombre: l.nombre,
    padreId: l.padreId ?? null,
    tipoLugarId: l.tipoLugarId ?? null,
    localidadId: l.localidadId ?? null,
    calle: l.calle ?? null,
    altura: l.altura ?? null,
    km: l.km ?? null,
    calleCruce: l.calleCruce ?? null,
    complemento: l.complemento ?? null,
    coordenadas,
    geometria: l.geometria ?? null,
    estadoGeo,
  };
}
