import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { PreDefinedActivity } from '@app/core/models/activity.models';

@Injectable({ providedIn: 'root' })
export class ActivityService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/organizations`;

  getActivities(organizationId: string): Observable<PreDefinedActivity[]> {
    return this.http.get<PreDefinedActivity[]>(`${this.baseUrl}/${organizationId}/activities`);
  }

  createActivity(
    organizationId: string,
    activity: { key: string; label: string; description: string; category: string },
  ): Observable<PreDefinedActivity> {
    return this.http.post<PreDefinedActivity>(`${this.baseUrl}/${organizationId}/activities`, activity);
  }

  updateActivity(
    organizationId: string,
    activityId: string,
    activity: { key: string; label: string; description: string; category: string; isActive: boolean },
  ): Observable<PreDefinedActivity> {
    return this.http.patch<PreDefinedActivity>(
      `${this.baseUrl}/${organizationId}/activities/${activityId}`,
      activity,
    );
  }

  deleteActivity(organizationId: string, activityId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${organizationId}/activities/${activityId}`);
  }
}
