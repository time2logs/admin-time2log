import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { FormsModule } from '@angular/forms';
import { NgClass } from '@angular/common';
import { OrganizationService } from '@services/organization.service';
import { Profile } from '@app/core/models/profile.models';
import { Organization } from '@app/core/models/organizations.models';
import { ReportService } from '@services/report.service';
import { forkJoin } from 'rxjs';

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
  imports: [TranslateModule, FormsModule, NgClass],
  templateUrl: './dashboard.html',
})
export class DashboardComponent implements OnInit {
  private readonly organizationService = inject(OrganizationService);
  private readonly reportService = inject(ReportService);

  protected readonly organizations = signal<Organization[]>([]);
  protected readonly membersWithActivity = signal<MemberWithActivity[]>([]);
  protected readonly inactiveDays = signal(7);

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

  ngOnInit(): void {
    this.loadOrganizations();
  }

  protected onDaysChange(days: number): void {
    this.inactiveDays.set(Number(days));
  }

  private loadOrganizations(): void {
    this.organizationService.getOrganizations().subscribe({
      next: (orgs) => {
        this.organizations.set(orgs);
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
          },
        });
      },
    });
  }
}