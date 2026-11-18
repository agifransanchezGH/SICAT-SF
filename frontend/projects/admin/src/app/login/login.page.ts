import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ActivatedRoute, Router } from '@angular/router';

import { Puente } from 'shared';

import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatProgressBarModule, Puente],
  template: `
    <main class="contenedor">
      <section class="banda" aria-hidden="true">
        <div class="texto">
          <p class="marca">SICAT SF</p>
          <p class="bajada">Panel de turismo de la ciudad de Santa Fe</p>
        </div>
        <sicat-puente class="puente" />
      </section>
      <form class="login" [formGroup]="form" (ngSubmit)="ingresar()">
        @if (enviando()) {
          <mat-progress-bar mode="indeterminate" class="progreso" aria-label="Ingresando" />
        }
        <h1>Ingresar al panel</h1>

        <mat-form-field>
          <mat-label>Email</mat-label>
          <input matInput type="email" formControlName="email" autocomplete="username" spellcheck="false" />
        </mat-form-field>

        <mat-form-field>
          <mat-label>Contraseña</mat-label>
          <input matInput type="password" formControlName="password" autocomplete="current-password" />
        </mat-form-field>

        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }

        <button mat-flat-button type="submit" [disabled]="enviando()">Ingresar</button>
      </form>
    </main>
  `,
  styles: `
    .contenedor { min-height: 100vh; display: grid; grid-template-columns: minmax(0, 1.1fr) minmax(320px, 1fr); }
    .banda { position: relative; background: var(--sicat-rio); color: #fff; overflow: hidden; display: flex; flex-direction: column; justify-content: center; padding: 48px; }
    .texto { position: relative; z-index: 1; }
    .marca { margin: 0; font-weight: 800; font-stretch: var(--sicat-ancho-titular); font-size: clamp(44px, 6vw, 80px); line-height: 0.95; letter-spacing: -0.02em; }
    .bajada { margin: 12px 0 0; font-size: 18px; color: var(--sicat-rio-claro); max-width: 26ch; }
    .puente { position: absolute; left: 0; right: 0; bottom: 0; color: #fff; opacity: 0.55; }
    .login { position: relative; align-self: center; justify-self: center; width: 100%; max-width: 380px; display: flex; flex-direction: column; gap: 8px; padding: 32px 24px; box-sizing: border-box; }
    .progreso { position: absolute; top: 0; left: 24px; right: 24px; }
    h1 { margin: 0 0 12px; font-size: 26px; font-weight: 700; font-stretch: var(--sicat-ancho-nombre); }
    .error { margin: 0 0 4px; color: var(--sicat-error); }
    button[type=submit] { height: 44px; margin-top: 4px; }
    @media (max-width: 760px) {
      .contenedor { grid-template-columns: 1fr; grid-template-rows: auto 1fr; }
      .banda { padding: 28px 20px 56px; }

      .login { align-self: start; }
    }
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
    // El botón no se deshabilita: al tocarlo con datos faltantes se marca qué falta.
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Completá el email y la contraseña.');
      return;
    }
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
