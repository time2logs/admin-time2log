import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '@services/auth.service';
import { OnboardingService } from '@services/onboarding.service';
import { combineLatest, filter, firstValueFrom, take } from 'rxjs';

type State = 'loading' | 'invalid' | 'expired' | 'wrong_user' | 'success';

@Component({
  selector: 'app-accept-invite',
  standalone: true,
  imports: [RouterLink, TranslateModule],
  templateUrl: './accept-invite.html',
})
export class AcceptInviteComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly onboardingService = inject(OnboardingService);

  protected readonly state = signal<State>('loading');

  async ngOnInit(): Promise<void> {
    const token = this.route.snapshot.queryParamMap.get('invite_token');
    if (!token) {
      this.state.set('invalid');
      return;
    }

    const [, user] = await firstValueFrom(
      combineLatest([this.authService.isInitialized$, this.authService.currentUser$]).pipe(
        filter(([initialized]) => initialized),
        take(1)
      )
    );

    if (!user) {
      const redirectTo = `/auth/accept-invite?invite_token=${encodeURIComponent(token)}`;
      await this.router.navigate(['/auth/login'], { queryParams: { redirectTo } });
      return;
    }

    try {
      await firstValueFrom(this.onboardingService.acceptInvite(token));
      this.state.set('success');
      setTimeout(() => this.router.navigate(['/dashboard']), 1200);
    } catch (err: unknown) {
      console.error('acceptInvite failed', err);
      const haystack = this.errorHaystack(err);
      if (haystack.includes('email_mismatch')) {
        this.state.set('wrong_user');
      } else if (haystack.includes('invite_expired')) {
        this.state.set('expired');
      } else {
        this.state.set('invalid');
      }
    }
  }

  private errorHaystack(err: unknown): string {
    const e = err as { error?: { detail?: string; supabaseResponse?: string; message?: string } };
    return [e?.error?.detail, e?.error?.supabaseResponse, e?.error?.message]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();
  }

  async signOutAndRetry(): Promise<void> {
    await firstValueFrom(this.authService.logout());
    const token = this.route.snapshot.queryParamMap.get('invite_token') ?? '';
    const redirectTo = `/auth/accept-invite?invite_token=${encodeURIComponent(token)}`;
    await this.router.navigate(['/auth/login'], { queryParams: { redirectTo } });
  }
}
