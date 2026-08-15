import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, shareReplay, tap } from 'rxjs';
import { environment } from '@env/environment';
import { Team } from '@app/core/models/team.models';
import { Profile } from '@app/core/models/profile.models';

@Injectable({ providedIn: 'root' })
export class TeamService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/organizations`;
  private readonly teams = new Map<string, Observable<Team[]>>();
  private readonly teamMembers = new Map<string, Observable<Profile[]>>();

  getTeams(organizationId: string): Observable<Team[]> {
    const cached = this.teams.get(organizationId);
    if (cached) return cached;
    const request$ = this.http.get<Team[]>(`${this.baseUrl}/${organizationId}/teams`).pipe(
      shareReplay({ bufferSize: 1, refCount: true }),
    );
    this.teams.set(organizationId, request$);
    return request$;
  }

  createTeam(organizationId: string, professionId: string, name: string): Observable<Team> {
    return this.http.post<Team>(`${this.baseUrl}/${organizationId}/teams`, { professionId, name }).pipe(
      tap(() => this.invalidateTeams(organizationId)),
    );
  }

  deleteTeam(organizationId: string, teamId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${organizationId}/teams/${teamId}`).pipe(
      tap(() => this.invalidateTeams(organizationId)),
    );
  }

  getTeamMembers(organizationId: string, teamId: string): Observable<Profile[]> {
    const key = `${organizationId}/${teamId}`;
    const cached = this.teamMembers.get(key);
    if (cached) return cached;
    const request$ = this.http.get<Profile[]>(`${this.baseUrl}/${organizationId}/teams/${teamId}/members`).pipe(
      shareReplay({ bufferSize: 1, refCount: true }),
    );
    this.teamMembers.set(key, request$);
    return request$;
  }

  addTeamMember(organizationId: string, teamId: string, userId: string, teamRole: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${organizationId}/teams/${teamId}/members`, { userId, teamRole }).pipe(
      tap(() => this.invalidateTeamMembers(organizationId, teamId)),
    );
  }

  removeTeamMember(organizationId: string, teamId: string, userId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${organizationId}/teams/${teamId}/members/${userId}`).pipe(
      tap(() => this.invalidateTeamMembers(organizationId, teamId)),
    );
  }

  invalidateTeams(organizationId: string): void {
    this.teams.delete(organizationId);
  }

  private invalidateTeamMembers(organizationId: string, teamId: string): void {
    this.teamMembers.delete(`${organizationId}/${teamId}`);
  }
}
