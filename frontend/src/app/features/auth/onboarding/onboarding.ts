import { Component, inject, signal, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '@services/auth.service';
import { OnboardingService } from '@services/onboarding.service';
import { firstValueFrom } from 'rxjs';

type State = 'loading' | 'form' | 'success' | 'invalid';
type ErrorType = 'generic' | null;

@Component({
  selector: 'app-onboarding',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, TranslateModule],
  templateUrl: './onboarding.html',
})
export class OnboardingComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly onboardingService = inject(OnboardingService);

  protected readonly state = signal<State>('loading');
  protected readonly isLoading = signal(false);
  protected readonly errorType = signal<ErrorType>(null);
  protected readonly email = signal('');
  protected readonly organizationName = signal('');

  private token = '';

  protected readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.minLength(2)]],
    lastName: ['', [Validators.required, Validators.minLength(2)]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  async ngOnInit(): Promise<void> {
    this.token = this.route.snapshot.queryParamMap.get('invite_token') ?? '';
    if (!this.token) {
      this.state.set('invalid');
      return;
    }

    try {
      const invite = await firstValueFrom(this.onboardingService.getInvite(this.token));
      this.email.set(invite.email);
      this.organizationName.set(invite.organizationName);
      this.state.set('form');
    } catch {
      this.state.set('invalid');
    }
  }

  async onSubmit(): Promise<void> {
    if (this.form.invalid) return;

    this.isLoading.set(true);
    this.errorType.set(null);

    try {
      const { firstName, lastName, password } = this.form.getRawValue();
      await firstValueFrom(
        this.onboardingService.complete({ token: this.token, firstName, lastName, password })
      );

      const { error } = await firstValueFrom(this.authService.login(this.email(), password));
      if (error) {
        this.state.set('success');
        return;
      }

      await this.router.navigate(['/dashboard']);
    } catch {
      this.errorType.set('generic');
    } finally {
      this.isLoading.set(false);
    }
  }
}
