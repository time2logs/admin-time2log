import {inject, Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {Invite} from '@app/core/models/system-admin.models';
import {HttpClient} from '@angular/common/http';
import {environment} from '@env/environment';
import {Profile} from '@app/core/models/profile.models';

@Injectable({
  providedIn: 'root',
})
export class SystemAdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/system-admin`;

  createInvite(email: string): Observable<Invite> {
    return this.http.post<Invite>(`${this.baseUrl}/invites`, { email });
  }

  listInvites(userRole: string): Observable<Invite[]> {
    return this.http.get<Invite[]>(`${this.baseUrl}/invites/${userRole}`);
  }

  deleteInvite(inviteId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/invites/${inviteId}`);
  }

  getAdmins(): Observable<Profile[]> {
  return this.http.get<Profile[]>(`${this.baseUrl}/admins`);
  }
}
