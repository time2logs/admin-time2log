import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { FormsModule } from '@angular/forms';
import { OrganizationService } from '@services/organization.service';
import { ReportService } from '@services/report.service';
import { Organization } from '@app/core/models/organizations.models';
import { DailyMemberReport } from '@app/core/models/report.models';
import { Calendar } from '@app/shared/calendar/calendar';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [TranslateModule, FormsModule, Calendar],
  templateUrl: './reports.html',
})
export class Reports implements OnInit {
  private readonly router = inject(Router);
  private readonly organizationService = inject(OrganizationService);
  private readonly reportService = inject(ReportService);

  protected readonly organizations = signal<Organization[]>([]);
  protected readonly selectedOrgId = signal('');
  protected readonly selectedDate = signal(new Date().toISOString().slice(0, 10));
  protected readonly dailyReport = signal<DailyMemberReport[]>([]);
  protected readonly isLoading = signal(false);

  ngOnInit(): void {
    this.organizationService.getOrganizations().subscribe({
      next: (orgs) => {
        this.organizations.set(orgs);
        if (orgs.length === 1) {
          this.selectedOrgId.set(orgs[0].id);
          this.loadDailyReport();
        }
      },
    });
  }

  protected selectOrg(orgId: string): void {
    this.selectedOrgId.set(orgId);
    this.loadDailyReport();
  }

  protected onDateSelected(date: string): void {
    this.selectedDate.set(date);
    this.loadDailyReport();
  }

  protected openMember(report: DailyMemberReport): void {
    this.router.navigate(['/reports/members', report.userId], {
      queryParams: { organizationId: this.selectedOrgId() },
    });
  }

  private loadDailyReport(): void {
    const orgId = this.selectedOrgId();
    if (!orgId) return;

    this.isLoading.set(true);
    this.reportService.getDailyReport(orgId, this.selectedDate()).subscribe({
      next: (report) => {
        this.dailyReport.set(report);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }
}
