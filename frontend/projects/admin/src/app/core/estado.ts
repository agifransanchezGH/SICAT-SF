import { Component, computed, input } from '@angular/core';

const TEXTOS: Record<string, string> = {
  BORRADOR: 'Borrador',
  PUBLICADO: 'Publicado',
  OCULTO: 'Oculto',
  PENDIENTE: 'Sin geocodificar',
  AUTOMATICO: 'Ubicación automática',
  APROXIMADO: 'Ubicación aproximada',
  NO_ENCONTRADO: 'Dirección no encontrada',
  VERIFICADO: 'Ubicación verificada',
  inactivo: 'Dado de baja',
};

/** Chip de color para estados de publicación, de geocodificación y bajas. */
@Component({
  selector: 'app-estado',
  template: `<span class="estado" [class]="valor()">{{ texto() }}</span>`,
  styles: `:host { display: inline-block; }`,
})
export class EstadoChip {
  readonly valor = input.required<string | null | undefined>();
  protected readonly texto = computed(() => TEXTOS[this.valor() ?? ''] ?? this.valor() ?? '');
}
