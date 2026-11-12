import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule],
  template: `
    <main class="contenedor">
      <form class="tarjeta" [formGroup]="form" (ngSubmit)="ingresar()">
        <h1>SICAT SF</h1>
        <p class="sub">Panel de administración</p>

        <label for="email">Email</label>
        <input id="email" type="email" formControlName="email" autocomplete="username" />

        <label for="password">Contraseña</label>
        <input id="password" type="password" formControlName="password" autocomplete="current-password" />

        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }

        <button type="submit" [disabled]="form.invalid || enviando()">
          {{ enviando() ? 'Ingresando…' : 'Ingresar' }}
        </button>
      </form>
    </main>
  `,
  styles: `
    .contenedor { min-height: 100vh; display: grid; place-items: center; padding: 16px; background: #f7f8fa; }
    .tarjeta { width: 100%; max-width: 360px; display: flex; flex-direction: column; gap: 8px;
      padding: 24px; background: #fff; border: 1px solid #d5dae1; border-radius: 10px; }
    h1 { margin: 0; font-size: 22px; }
    .sub { margin: 0 0 12px; color: #5a626e; }
    label { font-size: 14px; font-weight: 500; }
    input { padding: 8px 10px; border: 1px solid #b8c0cc; border-radius: 6px; font: inherit; }
    button { margin-top: 12px; padding: 10px; border: 0; border-radius: 6px; background: #2f5bd3; color: #fff;
      font: inherit; font-weight: 600; cursor: pointer; }
    button:disabled { opacity: .6; cursor: default; }
    .error { margin: 4px 0 0; color: #b42318; font-size: 14px; }
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
