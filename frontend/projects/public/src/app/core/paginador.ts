import { Injectable } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';

@Injectable()
export class PaginadorEnEspanol extends MatPaginatorIntl {
  override itemsPerPageLabel = 'Por página';
  override nextPageLabel = 'Siguiente';
  override previousPageLabel = 'Anterior';
  override firstPageLabel = 'Primera';
  override lastPageLabel = 'Última';
  override getRangeLabel = (pagina: number, tamanio: number, total: number): string => {
    if (total === 0) {
      return '0 de 0';
    }
    const desde = pagina * tamanio + 1;
    const hasta = Math.min(total, (pagina + 1) * tamanio);
    return `${desde}–${hasta} de ${total}`;
  };
}
