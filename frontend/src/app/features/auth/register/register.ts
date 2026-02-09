import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '@services/auth.service';
import { firstValueFrom } from 'rxjs';

type State = 'form' | 'success';
type ErrorType = 'email_exists' | 'generic' | null;

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, TranslateModule],
  templateUrl: './register.html',
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);

  protected readonly state = signal<State>('form');
  protected readonly isLoading = signal(false);
  protected readonly errorType = signal<ErrorType>(null);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  async onSubmit(): Promise<void> {
    if (this.form.invalid) return;

    this.isLoading.set(true);
    this.errorType.set(null);

    try {
      const { name, email, password } = this.form.getRawValue();
      const { error } = await firstValueFrom(this.authService.register(email, password, name));

      if (error) {
        this.errorType.set(this.mapError(error.message));
        return;
      }

      this.state.set('success');
    } catch {
      this.errorType.set('generic');
    } finally {
      this.isLoading.set(false);
    }
  }

  private mapError(message: string): ErrorType {
    const msg = message.toLowerCase();

    if (msg.includes('already registered') || msg.includes('already exists') || msg.includes('duplicate')) {
      return 'email_exists';
    }

    return 'generic';
  }
}
