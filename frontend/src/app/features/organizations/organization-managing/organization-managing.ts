import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { OrganizationService } from '@services/organization.service';
import { TeamService } from '@services/team.service';
import { ToastService } from '@services/toast.service';
import {Organization, Profession} from '@app/core/models/organizations.models';
import { Profile } from '@app/core/models/profile.models';
import { Team } from '@app/core/models/team.models';

type Tab = 'members' | 'curriculums' | 'teams' | 'settings';

@Component({
  selector: 'app-organization-managing',
  standalone: true,
  imports: [TranslateModule, FormsModule],
  templateUrl: './organization-managing.html',
})
export class OrganizationManaging implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly organizationService = inject(OrganizationService);
  private readonly teamService = inject(TeamService);
  private readonly toast = inject(ToastService);
  private readonly translate = inject(TranslateService);

  protected readonly activeTab = signal<Tab>('members');
  protected readonly organization = signal<Organization | null>(null);

  protected readonly members = signal<Profile[]>([]);
  protected readonly showInviteForm = signal(false);
  protected readonly inviteUserId = signal('');
  protected readonly inviteRole = signal('member');
  protected readonly isInviting = signal(false);

  protected readonly professions = signal<Profession[]>([]);
  protected readonly showCreateProfession = signal(false);
  protected readonly newProfessionKey = signal('');
  protected readonly newProfessionLabel = signal('');
  protected readonly isCreatingProfession = signal(false);

  protected readonly teams = signal<Team[]>([]);
  protected readonly showCreateTeam = signal(false);
  protected readonly newTeamName = signal('');
  protected readonly newTeamProfessionId = signal('');
  protected readonly isCreatingTeam = signal(false);

  protected readonly showDeleteConfirm = signal(false);
  protected readonly isDeleting = signal(false);

  private organizationId = '';

  ngOnInit(): void {
    this.organizationId = this.route.snapshot.params['id'];

    const tabParam = this.route.snapshot.queryParams['tab'] as Tab;
    if (tabParam && (['members', 'curriculums', 'teams', 'settings'] as Tab[]).includes(tabParam)) {
      this.activeTab.set(tabParam);
    }

    this.loadOrganization();
    this.loadMembers();
    this.loadProfessions();
    this.loadTeams();
  }

  protected setTab(tab: Tab): void {
    this.activeTab.set(tab);
    this.router.navigate([], { queryParams: { tab }, queryParamsHandling: 'merge' });
  }

  protected goBack(): void {
    this.router.navigate(['/organizations']);
  }

  protected toggleInviteForm(): void {
    this.showInviteForm.update((v) => !v);
  }

  protected invite(): void {
    const userId = this.inviteUserId().trim();
    if (!userId) return;

    this.isInviting.set(true);

    this.organizationService.inviteToOrganization(this.organizationId, userId, this.inviteRole()).subscribe({
      next: () => {
        this.inviteUserId.set('');
        this.inviteRole.set('member');
        this.isInviting.set(false);
        this.toast.success(this.translate.instant('toast.inviteSuccess'));
        this.loadMembers();
      },
      error: () => {
        this.isInviting.set(false);
      },
    });
  }

  protected removeMember(member: Profile): void {
    this.organizationService.removeOrganizationMember(this.organizationId, member.id).subscribe({
      next: () => {
        this.toast.success(this.translate.instant('toast.memberRemoved'));
        this.loadMembers();
      },
    });
  }

  protected deleteOrganization(): void {
    this.isDeleting.set(true);
    this.organizationService.deleteOrganization(this.organizationId).subscribe({
      next: () => {
        this.toast.success(this.translate.instant('toast.organizationDeleted'));
        this.router.navigate(['/organizations']);
      },
      error: () => this.isDeleting.set(false),
    });
  }

  private loadOrganization(): void {
    this.organizationService.getOrganizations().subscribe({
      next: (orgs) => {
        const org = orgs.find((o) => o.id === this.organizationId);
        if (org) this.organization.set(org);
      },
    });
  }

  private loadMembers(): void {
    this.organizationService.getOrganizationMembers(this.organizationId).subscribe({
      next: (members) => this.members.set(members),
    });
  }

  private loadProfessions(): void {
    this.organizationService.getProfessions(this.organizationId).subscribe({
      next: (professions) => this.professions.set(professions),
    });
  }

  protected toggleCreateProfession(): void {
    this.showCreateProfession.update((v) => !v);
  }

  protected createProfession(): void {
    const key = this.newProfessionKey().trim();
    const label = this.newProfessionLabel().trim();
    if (!key || !label) return;

    this.isCreatingProfession.set(true);
    this.organizationService.createProfession(this.organizationId, key, label).subscribe({
      next: () => {
        this.newProfessionKey.set('');
        this.newProfessionLabel.set('');
        this.isCreatingProfession.set(false);
        this.showCreateProfession.set(false);
        this.toast.success(this.translate.instant('toast.professionCreated'));
        this.loadProfessions();
      },
      error: () => {
        this.isCreatingProfession.set(false);
      },
    });
  }

  protected openProfession(profession: Profession): void {
    this.router.navigate(['/organizations', this.organizationId, 'professions', profession.id]);
  }

  protected toggleCreateTeam(): void {
    this.showCreateTeam.update((v) => !v);
  }

  protected createTeam(): void {
    const name = this.newTeamName().trim();
    const professionId = this.newTeamProfessionId();
    if (!name || !professionId) return;

    this.isCreatingTeam.set(true);
    this.teamService.createTeam(this.organizationId, professionId, name).subscribe({
      next: () => {
        this.newTeamName.set('');
        this.newTeamProfessionId.set('');
        this.isCreatingTeam.set(false);
        this.showCreateTeam.set(false);
        this.toast.success(this.translate.instant('toast.teamCreated'));
        this.loadTeams();
      },
      error: () => {
        this.isCreatingTeam.set(false);
      },
    });
  }

  protected deleteTeam(team: Team): void {
    this.teamService.deleteTeam(this.organizationId, team.id).subscribe({
      next: () => {
        this.toast.success(this.translate.instant('toast.teamDeleted'));
        this.loadTeams();
      },
    });
  }

  protected openTeam(team: Team): void {
    this.router.navigate(['/organizations', this.organizationId, 'teams', team.id]);
  }

  protected openMemberReport(member: Profile): void {
    this.router.navigate(['/reports/members', member.id], {
      queryParams: { organizationId: this.organizationId },
    });
  }

  private loadTeams(): void {
    this.teamService.getTeams(this.organizationId).subscribe({
      next: (teams) => this.teams.set(teams),
    });
  }
}
