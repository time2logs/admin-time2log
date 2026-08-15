import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, shareReplay, tap } from 'rxjs';
import { environment } from '@env/environment';
import { Invite, Organization, Profession, Reminder } from '@app/core/models/organizations.models';
import { Profile } from '@app/core/models/profile.models';

@Injectable({ providedIn: 'root' })
export class OrganizationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/organizations`;
  private organizations$?: Observable<Organization[]>;
  private readonly professions = new Map<string, Observable<Profession[]>>();
  private readonly members = new Map<string, Observable<Profile[]>>();
  private readonly onlyMembers = new Map<string, Observable<Profile[]>>();

  getOrganizations(): Observable<Organization[]> {
    this.organizations$ ??= this.http.get<Organization[]>(this.baseUrl).pipe(shareReplay({ bufferSize: 1, refCount: true }));
    return this.organizations$;
  }

  getOrganizationMembers(id: string): Observable<Profile[]> {
    const cached = this.members.get(id);
    if (cached) return cached;
    const request$ = this.http.get<Profile[]>(`${this.baseUrl}/${id}/members`).pipe(shareReplay({ bufferSize: 1, refCount: true }));
    this.members.set(id, request$);
    return request$;
  }

  createOrganization(name: string): Observable<Organization> {
    return this.http.post<Organization>(this.baseUrl, { name }).pipe(
      tap(() => this.invalidateOrganizations()),
    );
  }

  deleteOrganization(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`).pipe(
      tap(() => {
        this.invalidateOrganizations();
        this.invalidateMembers(id);
        this.invalidateProfessions(id);
      }),
    );
  }

  private static readonly ROLE_MAPPING: Record<string, string> = {
    member: 'user',
    moderator: 'moderator',
  };

  createInvite(organizationId: string, email: string, userRole: string, semester: string): Observable<Invite> {
    const backendRole = OrganizationService.ROLE_MAPPING[userRole] ?? userRole;
    const semesterValue = semester === '-' ? null : semester;
    return this.http.post<Invite>(`${this.baseUrl}/${organizationId}/invites`, { email, userRole: backendRole, semester: semesterValue });
  }

  listInvites(organizationId: string): Observable<Invite[]> {
    return this.http.get<Invite[]>(`${this.baseUrl}/${organizationId}/invites`);
  }

  deleteInvite(organizationId: string, inviteId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${organizationId}/invites/${inviteId}`);
  }

  removeOrganizationMember(organizationId: string, userId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${organizationId}/members/${userId}`).pipe(
      tap(() => this.invalidateMembers(organizationId)),
    );
  }

  getProfessions(organizationId: string): Observable<Profession[]> {
    const cached = this.professions.get(organizationId);
    if (cached) return cached;
    const request$ = this.http.get<Profession[]>(`${this.baseUrl}/${organizationId}/professions`).pipe(shareReplay({ bufferSize: 1, refCount: true }));
    this.professions.set(organizationId, request$);
    return request$;
  }

  createProfession(organizationId: string, key: string, label: string): Observable<Profession> {
    return this.http.post<Profession>(`${this.baseUrl}/${organizationId}/professions`, { key, label }).pipe(
      tap(() => this.invalidateProfessions(organizationId)),
    );
  }

  getOnlyOrganizationMembers(id: string): Observable<Profile[]> {
    const cached = this.onlyMembers.get(id);
    if (cached) return cached;
    const request$ = this.http.get<Profile[]>(`${this.baseUrl}/${id}/onlyMembers`).pipe(shareReplay({ bufferSize: 1, refCount: true }));
    this.onlyMembers.set(id, request$);
    return request$;
  }

  getReminder(organizationId: string): Observable<Reminder | null> {
    return this.http.get<Reminder | null>(`${this.baseUrl}/${organizationId}/reminder`);
  }

  saveReminder(organizationId: string, reminder: { channel: string; sendTime: string; idleDays: number; sendDay: string }): Observable<Reminder> {
    return this.http.put<Reminder>(`${this.baseUrl}/${organizationId}/reminder`, reminder);
  }

  transferOwnership(organizationId: string, newOwnerId: string): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/${organizationId}/owner`, { newOwnerId });
  }

  getSemesterEndDate(organizationId: string): Observable<{ semesterEndDate: string | null }> {
    return this.http.get<{ semesterEndDate: string | null }>(`${this.baseUrl}/${organizationId}/semester-end-date`);
  }

  saveSemesterEndDate(organizationId: string, semesterEndDate: string | null): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${organizationId}/semester-end-date`, { semesterEndDate });
  }

  getTargetHours(organizationId: string): Observable<{ targetHours: number | null}>{
    return this.http.get<{ targetHours: number | null }>(`${this.baseUrl}/${organizationId}/targetHours`);
  }

  saveTargetHours(organizationId: string, targetHours: number | null): Observable<void>{
    return this.http.put<void>(`${this.baseUrl}/${organizationId}/targetHours`, { targetHours });
  }

  invalidateOrganizations(): void {
    this.organizations$ = undefined;
  }

  invalidateMembers(organizationId: string): void {
    this.members.delete(organizationId);
    this.onlyMembers.delete(organizationId);
  }

  invalidateProfessions(organizationId: string): void {
    this.professions.delete(organizationId);
  }
}
