import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ReportService } from '@services/report.service';
import { Profile } from '@app/core/models/profile.models';
import { DailyMemberReport, ReportStatus } from '@app/core/models/report.models';
import { Calendar } from '@app/shared/calendar/calendar';
import { FormatHoursPipe } from '@app/shared/pipes/format-hours.pipe';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [TranslatePipe, Calendar, FormatHoursPipe],
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
    const ids = new Set(this.members().map(m => m.id));
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

  protected badgeClasses(status: ReportStatus): string {
    switch (status) {
      case 'reported':
        return 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400';
      case 'under_target':
        return 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-400';
      case 'bad_rating':
      case 'bad_rating_under_target':
      case 'missing':
        return 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400';
      default:
        return 'bg-gray-100 dark:bg-gray-700/50 text-gray-600 dark:text-gray-400';
    }
  }
}
