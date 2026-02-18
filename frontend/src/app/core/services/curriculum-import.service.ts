import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { CurriculumImport } from '@app/core/models/curriculum-import.models';

@Injectable({ providedIn: 'root' })
export class CurriculumImportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/organizations`;

  getImports(organizationId: string, professionId: string): Observable<CurriculumImport[]> {
    return this.http.get<CurriculumImport[]>(
      `${this.baseUrl}/${organizationId}/professions/${professionId}/curriculum-imports`
    );
  }

  createImport(organizationId: string, professionId: string, payload: object): Observable<CurriculumImport> {
    return this.http.post<CurriculumImport>(
      `${this.baseUrl}/${organizationId}/professions/${professionId}/curriculum-imports`,
      payload
    );
  }

  applyImport(organizationId: string, professionId: string, importId: string): Observable<CurriculumImport> {
    return this.http.post<CurriculumImport>(
      `${this.baseUrl}/${organizationId}/professions/${professionId}/curriculum-imports/${importId}/apply`,
      {}
    );
  }
}
