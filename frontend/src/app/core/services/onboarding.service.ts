import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

export interface OnboardingInvite {
  organizationName: string;
  email: string;
  role: string;
}

export interface CompleteOnboardingPayload {
  token: string;
  firstName: string;
  lastName: string;
  password: string;
}

@Injectable({ providedIn: 'root' })
export class OnboardingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/onboarding`;
  private readonly inviteBaseUrl = `${environment.apiBaseUrl}/invites`;

  getInvite(token: string): Observable<OnboardingInvite> {
    return this.http.get<OnboardingInvite>(`${this.baseUrl}/invite`, { params: { token } });
  }

  complete(payload: CompleteOnboardingPayload): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/complete`, payload);
  }

  acceptInvite(token: string): Observable<void> {
    return this.http.post<void>(`${this.inviteBaseUrl}/accept`, { token });
  }
}
