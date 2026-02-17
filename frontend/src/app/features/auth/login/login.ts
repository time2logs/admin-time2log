import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '@services/auth.service';
import { firstValueFrom } from 'rxjs';

type ErrorType = 'credentials' | 'email_not_confirmed' | 'generic' | null;

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, TranslateModule],
  templateUrl: './login.html',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly isLoading = signal(false);
  protected readonly errorType = signal<ErrorType>(null);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  async onSubmit(): Promise<void> {
    if (this.form.invalid) return;

    this.isLoading.set(true);
    this.errorType.set(null);

    try {
      const { email, password } = this.form.getRawValue();
      const { error } = await firstValueFrom(this.authService.login(email, password));

      if (error) {
        this.errorType.set(this.mapError(error.message));
        return;
      }

      await this.router.navigate(['/dashboard']);
    } catch {
      this.errorType.set('generic');
    } finally {
      this.isLoading.set(false);
    }
  }

  async signInWithGoogle(): Promise<void> {
    this.isLoading.set(true);
    this.errorType.set(null);
    try {
      await this.authService.signInWithGoogle();
    } catch {
      this.errorType.set('generic');
      this.isLoading.set(false);
    }
  }

  private mapError(message: string): ErrorType {
    const msg = message.toLowerCase();

    if (msg.includes('email not confirmed')) {
      return 'email_not_confirmed';
    }

    if (msg.includes('invalid') || msg.includes('credentials')) {
      return 'credentials';
    }

    return 'generic';
  }
}
