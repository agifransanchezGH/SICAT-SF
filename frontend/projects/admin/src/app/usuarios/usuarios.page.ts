import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { Avisos } from '../core/avisos';
import { Pagina, Rol, Usuario } from '../core/modelos';

const ROLES: { valor: Rol; texto: string }[] = [
  { valor: 'ADMIN', texto: 'Administrador' },
  { valor: 'CARGADOR', texto: 'Carga de datos' },
  { valor: 'CONSULTA', texto: 'Solo consulta' },
];

@Component({
  selector: 'app-usuario-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ usuario ? 'Editar usuario' : 'Nuevo usuario' }}</h2>
    <mat-dialog-content>
      <form class="campos" [formGroup]="form">
        <mat-form-field><mat-label>Nombre completo</mat-label><input matInput formControlName="nombreCompleto" /></mat-form-field>
        <mat-form-field><mat-label>Email</mat-label><input matInput type="email" formControlName="email" autocomplete="off" /></mat-form-field>
        <mat-form-field>
          <mat-label>{{ usuario ? 'Nueva contraseña (opcional)' : 'Contraseña' }}</mat-label>
          <input matInput type="password" formControlName="password" autocomplete="new-password" />
          <mat-hint>Mínimo 8 caracteres</mat-hint>
        </mat-form-field>
        <mat-form-field>
          <mat-label>Rol</mat-label>
          <mat-select formControlName="rol">
            @for (r of roles; track r.valor) { <mat-option [value]="r.valor">{{ r.texto }}</mat-option> }
          </mat-select>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-flat-button (click)="guardar()" [disabled]="form.invalid || guardando()">Guardar</button>
    </mat-dialog-actions>
  `,
  styles: `.campos { display: flex; flex-direction: column; gap: 8px; padding-top: 4px; min-width: min(420px, 80vw); }`,
})
class UsuarioDialog {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly ref = inject(MatDialogRef<UsuarioDialog, Usuario>);
  protected readonly usuario = inject<Usuario | null>(MAT_DIALOG_DATA);
  protected readonly roles = ROLES;
  protected readonly guardando = signal(false);
  protected readonly form = inject(NonNullableFormBuilder).group({
    nombreCompleto: [this.usuario?.nombreCompleto ?? '', [Validators.required, Validators.maxLength(200)]],
    email: [this.usuario?.email ?? '', [Validators.required, Validators.email]],
    password: ['', this.usuario ? [Validators.minLength(8), Validators.maxLength(72)] : [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    rol: [this.usuario?.rol ?? ('CARGADOR' as Rol), Validators.required],
  });

  protected guardar(): void {
    const f = this.form.getRawValue();
    const cuerpo = { ...f, password: f.password || null };
    this.guardando.set(true);
    const pedido = this.usuario ? this.api.put<Usuario>(`/usuarios/${this.usuario.id}`, cuerpo) : this.api.post<Usuario>('/usuarios', cuerpo);
    pedido.subscribe({
      next: (u) => {
        this.avisos.ok('Usuario guardado');
        this.ref.close(u);
      },
      error: (e) => {
        this.guardando.set(false);
        this.avisos.error(e);
      },
    });
  }
}

/** Usuarios del panel y sus roles. Solo ADMIN. */
@Component({
  selector: 'app-usuarios-page',
  imports: [DatePipe, MatTableModule, MatPaginatorModule, MatButtonModule, MatIconModule, MatSlideToggleModule, MatTooltipModule],
  template: `
    <section class="pagina">
      <div class="encabezado">
        <h1>Usuarios</h1>
        <button mat-flat-button (click)="editar(null)"><mat-icon>person_add</mat-icon> Nuevo</button>
      </div>
      <div class="tarjeta">
        <table mat-table [dataSource]="pagina()?.contenido ?? []" class="tabla">
          <ng-container matColumnDef="nombre">
            <th mat-header-cell *matHeaderCellDef>Nombre</th>
            <td mat-cell *matCellDef="let u"><strong>{{ u.nombreCompleto }}</strong><div class="suave">{{ u.email }}</div></td>
          </ng-container>
          <ng-container matColumnDef="rol">
            <th mat-header-cell *matHeaderCellDef>Rol</th>
            <td mat-cell *matCellDef="let u">{{ textoRol(u.rol) }}</td>
          </ng-container>
          <ng-container matColumnDef="acceso">
            <th mat-header-cell *matHeaderCellDef>Último acceso</th>
            <td mat-cell *matCellDef="let u">{{ u.ultimoAcceso ? (u.ultimoAcceso | date: 'dd/MM/yyyy HH:mm') : 'Nunca' }}</td>
          </ng-container>
          <ng-container matColumnDef="activo">
            <th mat-header-cell *matHeaderCellDef>Activo</th>
            <td mat-cell *matCellDef="let u">
              <mat-slide-toggle [checked]="u.activo" [disabled]="u.id === yo" (change)="cambiarEstado(u, $event.checked)" aria-label="Activo" />
            </td>
          </ng-container>
          <ng-container matColumnDef="acciones">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let u"><button mat-icon-button matTooltip="Editar" (click)="editar(u)"><mat-icon>edit</mat-icon></button></td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let u; columns: columnas"></tr>
        </table>
        <mat-paginator [length]="pagina()?.totalElementos ?? 0" [pageIndex]="pagina()?.pagina ?? 0" [pageSize]="20" [hidePageSize]="true" (page)="cargar($event)" />
      </div>
    </section>
  `,
})
export class UsuariosPage {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly dialog = inject(MatDialog);
  protected readonly yo = inject(AuthService).usuario()?.id;

  protected readonly columnas = ['nombre', 'rol', 'acceso', 'activo', 'acciones'];
  protected readonly pagina = signal<Pagina<Usuario> | null>(null);
  private numero = 0;

  constructor() {
    this.cargar();
  }

  protected textoRol(r: Rol): string {
    return ROLES.find((x) => x.valor === r)?.texto ?? r;
  }

  protected cargar(e?: PageEvent): void {
    if (e) {
      this.numero = e.pageIndex;
    }
    this.api.get<Pagina<Usuario>>('/usuarios', { page: this.numero, size: 20 }).subscribe({
      next: (p) => this.pagina.set(p),
      error: (err) => this.avisos.error(err),
    });
  }

  protected editar(u: Usuario | null): void {
    this.dialog.open(UsuarioDialog, { data: u }).afterClosed().subscribe((r?: Usuario) => r && this.cargar());
  }

  protected cambiarEstado(u: Usuario, activo: boolean): void {
    this.api.patch<Usuario>(`/usuarios/${u.id}/estado`, { activo }).subscribe({
      next: () => {
        this.avisos.ok(activo ? 'Usuario activado' : 'Usuario desactivado');
        this.cargar();
      },
      error: (e) => {
        this.avisos.error(e);
        this.cargar();
      },
    });
  }
}
