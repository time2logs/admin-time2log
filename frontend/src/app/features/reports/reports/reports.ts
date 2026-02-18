import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { ReportService } from '@services/report.service';
import { Profile } from '@app/core/models/profile.models';
import { DailyMemberReport } from '@app/core/models/report.models';
import { Calendar } from '@app/shared/calendar/calendar';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [TranslateModule, Calendar],
  templateUrl: './reports.html',
})
export class Reports {
  private readonly router = inject(Router);
  private readonly reportService = inject(ReportService);

  readonly organizationId = input.required<string>();
  readonly members = input<Profile[]>([]);

  protected readonly selectedDate = signal(new Date().toISOString().slice(0, 10));
  protected readonly isLoading = signal(false);

  private readonly dailyReport = signal<DailyMemberReport[]>([]);

  protected readonly filteredReport = computed(() => {
    const members = this.members();
    if (members.length === 0) return this.dailyReport();
    const ids = new Set(members.map(m => m.id));
    return this.dailyReport().filter(r => ids.has(r.userId));
  });

  constructor() {
    effect(() => {
      const orgId = this.organizationId();
      const date = this.selectedDate();
      this.isLoading.set(true);
      this.reportService.getDailyReport(orgId, date).subscribe({
        next: (report) => {
          this.dailyReport.set(report);
          this.isLoading.set(false);
        },
        error: () => this.isLoading.set(false),
      });
    }, { allowSignalWrites: true });
  }

  protected onDateSelected(date: string): void {
    this.selectedDate.set(date);
  }

  protected openMember(report: DailyMemberReport): void {
    this.router.navigate(['/reports/members', report.userId], {
      queryParams: { organizationId: this.organizationId() },
    });
  }
}
