import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { OrganizationService } from '@services/organization.service';
import { TeamService } from '@services/team.service';
import { ToastService } from '@services/toast.service';
import { Team } from '@app/core/models/team.models';
import { Profile } from '@app/core/models/profile.models';

@Component({
  selector: 'app-team-managing',
  standalone: true,
  imports: [TranslateModule],
  templateUrl: './team-managing.html',
})
export class TeamManaging implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly organizationService = inject(OrganizationService);
  private readonly teamService = inject(TeamService);
  private readonly toast = inject(ToastService);
  private readonly translate = inject(TranslateService);

  protected readonly team = signal<Team | null>(null);
  protected readonly orgMembers = signal<Profile[]>([]);
  protected readonly teamMemberIds = signal<Set<string>>(new Set());
  protected readonly togglingId = signal<string | null>(null);
  protected readonly showSelection = signal(false);

  protected readonly teamMembers = computed(() => {
    const ids = this.teamMemberIds();
    return this.orgMembers().filter((m) => ids.has(m.id));
  });

  protected readonly selectionList = computed(() => {
    const ids = this.teamMemberIds();
    return this.orgMembers().map((m) => ({
      ...m,
      inTeam: ids.has(m.id),
    }));
  });

  private organizationId = '';
  private teamId = '';

  ngOnInit(): void {
    this.organizationId = this.route.snapshot.params['id'];
    this.teamId = this.route.snapshot.params['teamId'];
    this.loadTeam();
    this.loadOrgMembers();
    this.loadTeamMembers();
  }

  protected goBack(): void {
    if (this.showSelection()) {
      this.showSelection.set(false);
      return;
    }
    this.router.navigate(['/organizations', this.organizationId], { queryParams: { tab: 'teams' } });
  }

  protected openMemberReport(member: Profile): void {
    this.router.navigate(['/reports/members', member.id], {
      queryParams: { organizationId: this.organizationId },
    });
  }

  protected openSelection(): void {
    this.showSelection.set(true);
  }

  protected toggleMember(member: Profile & { inTeam: boolean }): void {
    if (this.togglingId()) return;

    this.togglingId.set(member.id);

    if (member.inTeam) {
      this.teamService.removeTeamMember(this.organizationId, this.teamId, member.id).subscribe({
        next: () => {
          this.teamMemberIds.update((ids) => {
            const next = new Set(ids);
            next.delete(member.id);
            return next;
          });
          this.togglingId.set(null);
        },
        error: () => this.togglingId.set(null),
      });
    } else {
      this.teamService.addTeamMember(this.organizationId, this.teamId, member.id, 'user').subscribe({
        next: () => {
          this.teamMemberIds.update((ids) => {
            const next = new Set(ids);
            next.add(member.id);
            return next;
          });
          this.togglingId.set(null);
        },
        error: () => this.togglingId.set(null),
      });
    }
  }

  private loadTeam(): void {
    this.teamService.getTeams(this.organizationId).subscribe({
      next: (teams) => {
        const team = teams.find((t) => t.id === this.teamId);
        if (team) this.team.set(team);
      },
    });
  }

  private loadOrgMembers(): void {
    this.organizationService.getOrganizationMembers(this.organizationId).subscribe({
      next: (members) => this.orgMembers.set(members),
    });
  }

  private loadTeamMembers(): void {
    this.teamService.getTeamMembers(this.organizationId, this.teamId).subscribe({
      next: (members) => this.teamMemberIds.set(new Set(members.map((m) => m.id))),
    });
  }
}
