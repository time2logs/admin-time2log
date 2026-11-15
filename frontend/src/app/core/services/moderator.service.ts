import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Profile } from '@app/core/models/profile.models';

@Injectable({ providedIn: 'root' })
export class ModeratorService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/organizations`;

  getAssignedLearners(organizationId: string, moderatorId: string): Observable<Profile[]> {
    return this.http.get<Profile[]>(`${this.baseUrl}/${organizationId}/moderators/${moderatorId}/learners`);
  }

  assignLearner(organizationId: string, moderatorId: string, learnerId: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${organizationId}/moderators/${moderatorId}/learners`, { learnerId });
  }

  unassignLearner(organizationId: string, moderatorId: string, learnerId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${organizationId}/moderators/${moderatorId}/learners/${learnerId}`);
  }
}
