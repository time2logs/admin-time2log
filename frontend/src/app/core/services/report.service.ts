import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { CurriculumOverview, DailyMemberReport, MemberActivityRecord } from '@app/core/models/report.models';
import { map } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/organizations`;

  getDailyReport(organizationId: string, date: string): Observable<DailyMemberReport[]> {
    return this.http.get<DailyMemberReport[]>(
      `${this.baseUrl}/${organizationId}/reports/daily?date=${date}`
    );
  }

  getMemberRecordsByDate(organizationId: string, userId: string, date: string): Observable<MemberActivityRecord[]> {
    return this.http.get<MemberActivityRecord[]>(
      `${this.baseUrl}/${organizationId}/reports/members/${userId}/records?date=${date}`
    );
  }

  getMemberRecordsByRange(organizationId: string, userId: string, from: string, to: string): Observable<MemberActivityRecord[]> {
    return this.http.get<MemberActivityRecord[]>(
      `${this.baseUrl}/${organizationId}/reports/members/${userId}/records?from=${from}&to=${to}`
    );
  }

  getCurriculum(organizationId: string, professionId: string): Observable<CurriculumOverview> {
    return this.http.get<CurriculumOverview>(
      `${this.baseUrl}/${organizationId}/professions/${professionId}/curriculum`
    );
  }

 getLastEntryDate(organizationId: string, userId: string): Observable<Date | null> {
  return this.http.get<string | null>(
    `${this.baseUrl}/${organizationId}/reports/members/${userId}/last-entry-date`
  ).pipe(
    map((date) => (date ? new Date(date) : null))
  );
}
}
