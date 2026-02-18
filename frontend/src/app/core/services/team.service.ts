import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Team } from '@app/core/models/team.models';
import { Profile } from '@app/core/models/profile.models';

@Injectable({ providedIn: 'root' })
export class TeamService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/organizations`;

  getTeams(organizationId: string): Observable<Team[]> {
    return this.http.get<Team[]>(`${this.baseUrl}/${organizationId}/teams`);
  }

  createTeam(organizationId: string, professionId: string, name: string): Observable<Team> {
    return this.http.post<Team>(`${this.baseUrl}/${organizationId}/teams`, { professionId, name });
  }

  deleteTeam(organizationId: string, teamId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${organizationId}/teams/${teamId}`);
  }

  getTeamMembers(organizationId: string, teamId: string): Observable<Profile[]> {
    return this.http.get<Profile[]>(`${this.baseUrl}/${organizationId}/teams/${teamId}/members`);
  }

  addTeamMember(organizationId: string, teamId: string, userId: string, teamRole: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${organizationId}/teams/${teamId}/members`, { userId, teamRole });
  }

  removeTeamMember(organizationId: string, teamId: string, userId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${organizationId}/teams/${teamId}/members/${userId}`);
  }
}
