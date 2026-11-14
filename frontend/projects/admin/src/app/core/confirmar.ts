import { Component, Injectable, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { Observable } from 'rxjs';

interface DatosConfirmacion {
  titulo: string;
  mensaje: string;
  aceptar: string;
}

@Component({
  selector: 'app-confirmar-dialog',
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ datos.titulo }}</h2>
    <mat-dialog-content>{{ datos.mensaje }}</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">Cancelar</button>
      <button mat-flat-button [mat-dialog-close]="true">{{ datos.aceptar }}</button>
    </mat-dialog-actions>
  `,
})
class ConfirmarDialog {
  protected readonly datos = inject<DatosConfirmacion>(MAT_DIALOG_DATA);
}

/** Diálogo "¿Seguro?" para acciones como dar de baja o despublicar. */
@Injectable({ providedIn: 'root' })
export class Confirmar {
  private readonly dialog = inject(MatDialog);

  pedir(titulo: string, mensaje: string, aceptar = 'Aceptar'): Observable<boolean> {
    return this.dialog.open(ConfirmarDialog, { data: { titulo, mensaje, aceptar }, width: '420px' }).afterClosed();
  }
}
