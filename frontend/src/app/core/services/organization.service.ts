import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Invite, Organization, Profession } from '@app/core/models/organizations.models';
import { Profile } from '@app/core/models/profile.models';

@Injectable({ providedIn: 'root' })
export class OrganizationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/organizations`;

  getOrganizations(): Observable<Organization[]> {
    return this.http.get<Organization[]>(this.baseUrl);
  }

  getOrganizationMembers(id: string): Observable<Profile[]> {
    return this.http.get<Profile[]>(`${this.baseUrl}/${id}/members`);
  }

  createOrganization(name: string): Observable<Organization> {
    return this.http.post<Organization>(this.baseUrl, { name });
  }

  deleteOrganization(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  private static readonly ROLE_MAPPING: Record<string, string> = {
    member: 'user',
    admin: 'admin',
  };

  createInvite(organizationId: string, email: string, userRole: string): Observable<Invite> {
    const backendRole = OrganizationService.ROLE_MAPPING[userRole] ?? userRole;
    return this.http.post<Invite>(`${this.baseUrl}/${organizationId}/invites`, { email, userRole: backendRole });
  }

  listInvites(organizationId: string): Observable<Invite[]> {
    return this.http.get<Invite[]>(`${this.baseUrl}/${organizationId}/invites`);
  }

  deleteInvite(organizationId: string, inviteId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${organizationId}/invites/${inviteId}`);
  }

  removeOrganizationMember(organizationId: string, userId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${organizationId}/members/${userId}`);
  }

  getProfessions(organizationId: string): Observable<Profession[]> {
    return this.http.get<Profession[]>(`${this.baseUrl}/${organizationId}/professions`);
  }

  createProfession(organizationId: string, key: string, label: string): Observable<Profession> {
    return this.http.post<Profession>(`${this.baseUrl}/${organizationId}/professions`, { key, label });
  }
}
