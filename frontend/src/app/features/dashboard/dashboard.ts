import { Component, inject, OnInit, signal, computed, effect } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { FormsModule } from '@angular/forms';
import { NgClass } from '@angular/common';
import { NgxChartsModule, LegendPosition, Color, ScaleType } from '@swimlane/ngx-charts';
import { OrganizationService } from '@services/organization.service';
import { Profile } from '@app/core/models/profile.models';
import { Organization } from '@app/core/models/organizations.models';
import { ReportService } from '@services/report.service';
import { NgxChartEntry, LocationSummary } from '@app/core/models/report.models';
import { forkJoin } from 'rxjs';
import { HostListener } from '@angular/core';


type DateRange = '30d' | '90d' | '1y' | 'all';
type FilterMode = 'range' | 'semester';

interface MemberWithActivity {
  id: string;
  name: string;
  organizationId: string;
  lastEntry: Date | null;
  daysInactive: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [TranslateModule, FormsModule, NgClass, NgxChartsModule],
  templateUrl: './dashboard.html',
})
export class DashboardComponent implements OnInit {
  private readonly organizationService = inject(OrganizationService);
  private readonly reportService = inject(ReportService);

  protected readonly organizations = signal<Organization[]>([]);
  protected readonly membersWithActivity = signal<MemberWithActivity[]>([]);
  protected readonly inactiveDays = signal(7);

  // Chart state
  protected readonly selectedOrgId = signal<string | null>(null);
  protected readonly selectedUserId = signal<string | null>(null);
  protected readonly range = signal<DateRange>('30d');
  protected readonly filterMode = signal<FilterMode>('range');
  protected readonly allSemesters = ['1', '2', '3', '4', '5', '6', '7', '8'] as const;
  protected readonly selectedSemesters = signal<string[]>([]);
  protected readonly activityChartData = signal<NgxChartEntry[]>([]);
  protected readonly locationChartData = signal<NgxChartEntry[]>([]);
  protected readonly chartLoading = signal(false);

  protected readonly orgMembers = computed(() => {
    const orgId = this.selectedOrgId();
    if (!orgId) return [];
    return this.membersWithActivity().filter((m) => m.organizationId === orgId);
  });

  protected readonly legendPosition = LegendPosition.Below;

  protected readonly colorScheme: Color = {
    name: 'time2log',
    selectable: true,
    group: ScaleType.Ordinal,
    domain: [
      '#29b6d6',
      '#1a8fa8',
      '#5ecde0',
      '#4a7fc1',
      '#2ea89e',
      '#6090d8',
      '#3db8ad',
      '#5580c8',
    ],
  };

  protected readonly totalMemberCount = computed(() => this.membersWithActivity().length);

  protected readonly inactiveRanking = computed(() => {
    const members = this.membersWithActivity();
    const days = this.inactiveDays();
    const cutoff = new Date();
    cutoff.setDate(cutoff.getDate() - days);

    return members
      .filter((m) => m.lastEntry === null || m.lastEntry < cutoff)
      .sort((a, b) => b.daysInactive - a.daysInactive);
  });

  constructor() {
    effect(() => {
      const orgId = this.selectedOrgId();
      const userId = this.selectedUserId();
      // Read all reactive deps so the effect re-runs on any change.
      const range = this.range();
      const mode = this.filterMode();
      const semesters = this.selectedSemesters();
      if (!orgId || !userId) return;
      this.loadActivityChart(orgId, userId, range, mode, semesters);
      this.loadLocationChart(orgId, userId, range, mode, semesters);
    });

  }

  ngOnInit(): void {
    this.loadOrganizations();
  }

  protected onDaysChange(days: number): void {
    this.inactiveDays.set(Number(days));
  }

  protected selectOrg(orgId: string): void {
    this.selectedOrgId.set(orgId);
    this.selectedUserId.set(null);
  }

  protected selectUser(userId: string): void {
    const member = this.membersWithActivity().find((m) => m.id === userId);
    if (member) {
      this.selectedOrgId.set(member.organizationId);
    }
    this.selectedUserId.set(userId || null);
  }

  protected setRange(range: DateRange): void {
    this.range.set(range);
  }

  protected setFilterMode(mode: FilterMode): void {
    if (this.filterMode() === mode) return;
    this.filterMode.set(mode);
    // Clear the other filter's state to avoid confusing overlaps.
    if (mode === 'range') {
      this.selectedSemesters.set([]);
    } else {
      this.range.set('all');
    }
  }

  protected toggleSemester(semester: string): void {
    const current = this.selectedSemesters();
    if (current.includes(semester)) {
      this.selectedSemesters.set(current.filter((s) => s !== semester));
    } else {
      this.selectedSemesters.set([...current, semester].sort());
    }
  }

  protected isSemesterSelected(semester: string): boolean {
    return this.selectedSemesters().includes(semester);
  }

  protected getDateParams(range: DateRange): { from?: string; to?: string } {
    const today = new Date();
    const pad = (n: number) => String(n).padStart(2, '0');
    const fmt = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
    const to = fmt(today);

    if (range === 'all') return {};

    const from = new Date(today);
    if (range === '30d') from.setDate(today.getDate() - 30);
    else if (range === '90d') from.setDate(today.getDate() - 90);
    else if (range === '1y') from.setFullYear(today.getFullYear() - 1);

    return { from: fmt(from), to };
  }

  private loadLocationChart(orgId: string, userId: string, range: DateRange, mode: FilterMode, semesters: string[]): void {
    const useSemesters = mode === 'semester' && semesters.length > 0;
    const { from, to } = useSemesters ? {} as { from?: string; to?: string } : this.getDateParams(range);
    this.reportService.getLocationSummary(orgId, userId, from, to, useSemesters ? semesters : undefined).subscribe({
      next: (data) => {
        this.locationChartData.set(
          data.map((l: LocationSummary) => ({ name: l.location, value: l.totalHours }))
        );
      },
    });
  }

  chartView = signal<[number, number]>(this.getChartSize());

  private getChartSize(): [number, number] {
    const width = window.innerWidth;
    if (width < 640) return [width - 48, 300];      // Mobile
    if (width < 1024) return [500, 340];             // Tablet
    return [560, 380];                               // Desktop
  }

  @HostListener('window:resize')
  onResize() {
    this.chartView.set(this.getChartSize());
  }


  private loadActivityChart(orgId: string, userId: string, range: DateRange, mode: FilterMode, semesters: string[]): void {
    if (mode === 'semester' && semesters.length === 0) {
      this.activityChartData.set([]);
      this.locationChartData.set([]);
      return;
    }
    this.chartLoading.set(true);
    const useSemesters = mode === 'semester' && semesters.length > 0;
    const { from, to } = useSemesters ? {} as { from?: string; to?: string } : this.getDateParams(range);
    this.reportService.getActivitySummary(orgId, userId, from, to, useSemesters ? semesters : undefined).subscribe({
      next: (data) => {
        this.activityChartData.set(
          data.map((a) => ({ name: `${a.activityName} (${a.totalHours}h)`, value: a.totalHours }))
        );
        this.chartLoading.set(false);
      },
      error: () => this.chartLoading.set(false),
    });
  }

  private loadOrganizations(): void {
    this.organizationService.getOrganizations().subscribe({
      next: (orgs) => {
        this.organizations.set(orgs);
        if (orgs.length > 0) {
          this.selectedOrgId.set(orgs[0].id);
        }
        this.loadAllMembersWithActivity(orgs);
      },
    });
  }

  private loadAllMembersWithActivity(orgs: Organization[]): void {
    if (orgs.length === 0) {
      this.membersWithActivity.set([]);
      return;
    }

    const memberRequests = orgs.map((org) =>
      this.organizationService.getOnlyOrganizationMembers(org.id)
    );

    forkJoin(memberRequests).subscribe({
      next: (results) => {
        const allMembers = results.flatMap((members, orgIndex) =>
          members.map((m: Profile) => ({ ...m, organizationId: orgs[orgIndex].id }))
        );

        if (allMembers.length === 0) {
          this.membersWithActivity.set([]);
          return;
        }

        const lastEntryRequests = allMembers.map((m) =>
          this.reportService.getLastEntryDate(m.organizationId, m.id)
        );

        forkJoin(lastEntryRequests).subscribe({
          next: (dates) => {
            const now = new Date();
            const membersWithActivity: MemberWithActivity[] = allMembers.map((m, i) => {
              const lastEntry = dates[i];
              const daysInactive = lastEntry
                ? Math.floor((now.getTime() - lastEntry.getTime()) / (1000 * 60 * 60 * 24))
                : Infinity;

              return {
                id: m.id,
                name: `${m.firstName} ${m.lastName}`.trim() || 'Unbekannt',
                organizationId: m.organizationId,
                lastEntry,
                daysInactive,
              };
            });

            this.membersWithActivity.set(membersWithActivity);
            if (!this.selectedUserId() && membersWithActivity.length > 0) {
              this.selectUser(membersWithActivity[0].id);
            }
          },
        });

      },
    });
  }
}
