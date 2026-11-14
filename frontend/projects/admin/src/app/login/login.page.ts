import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatProgressBarModule],
  template: `
    <main class="contenedor">
      <form class="tarjeta login" [formGroup]="form" (ngSubmit)="ingresar()">
        @if (enviando()) {
          <mat-progress-bar mode="indeterminate" class="progreso" />
        }
        <h1>SICAT SF</h1>
        <p class="suave">Panel de administración</p>

        <mat-form-field>
          <mat-label>Email</mat-label>
          <input matInput type="email" formControlName="email" autocomplete="username" />
        </mat-form-field>

        <mat-form-field>
          <mat-label>Contraseña</mat-label>
          <input matInput type="password" formControlName="password" autocomplete="current-password" />
        </mat-form-field>

        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }

        <button mat-flat-button type="submit" [disabled]="form.invalid || enviando()">Ingresar</button>
      </form>
    </main>
  `,
  styles: `
    .contenedor { min-height: 100vh; display: grid; place-items: center; padding: 16px; box-sizing: border-box; }
    .login { position: relative; width: 100%; max-width: 380px; display: flex; flex-direction: column; gap: 12px; padding: 28px; box-sizing: border-box; }
    .progreso { position: absolute; top: 0; left: 0; right: 0; }
    h1 { margin: 0; font: var(--mat-sys-headline-medium); }
    p { margin: 0 0 8px; }
    .error { color: var(--sicat-error); }
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected ingresar(): void {
    const { email, password } = this.form.getRawValue();
    this.enviando.set(true);
    this.error.set(null);
    this.auth.iniciarSesion(email, password).subscribe({
      next: () => {
        const volver = this.route.snapshot.queryParamMap.get('volver');
        this.router.navigateByUrl(volver && volver.startsWith('/') ? volver : '/');
      },
      error: (e: unknown) => {
        this.enviando.set(false);
        this.error.set(
          e instanceof HttpErrorResponse && e.status === 401
            ? 'Email o contraseña incorrectos.'
            : 'No se pudo conectar con el servidor. Intentá de nuevo.',
        );
      },
    });
  }
}
