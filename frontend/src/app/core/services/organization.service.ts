import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { Organization } from '@app/core/models/organizations.models';
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

  private static readonly ROLE_MAPPING: Record<string, string> = {
    member: 'user',
    admin: 'admin',
  };

  inviteToOrganization(organizationId: string, userId: string, userRole: string): Observable<void> {
    const backendRole = OrganizationService.ROLE_MAPPING[userRole] ?? userRole;
    return this.http.post<void>(`${this.baseUrl}/${organizationId}/invite`, { userId, userRole: backendRole });
  }
}
