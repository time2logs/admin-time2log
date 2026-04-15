import { Component, inject, OnInit, signal, HostListener } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { OrganizationService } from '@services/organization.service';
import { TeamService } from '@services/team.service';
import { ToastService } from '@services/toast.service';
import { Invite, Organization, Profession, Reminder } from '@app/core/models/organizations.models';
import { Profile } from '@app/core/models/profile.models';
import { Team } from '@app/core/models/team.models';
import {AuthService} from '@services/auth.service';

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
  protected readonly invites = signal<Invite[]>([]);
  protected readonly showInviteForm = signal(false);
  protected readonly inviteEmail = signal('');
  protected readonly inviteRole = signal('member');
  protected readonly inviteSemester = signal('1');
  protected readonly semesterOptions = ['1', '2', '3', '4', '5', '6', '7', '8'] as const;
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

  protected readonly reminderChannel = signal<string>('SMS');
  protected readonly reminderSendDay = signal<string>('FRIDAY');
  protected readonly reminderSendTime = signal<string>('08:00');
  protected readonly reminderIdleDays = signal<number>(3);
  protected readonly isSavingReminder = signal(false);
  protected readonly dayOptions = [
    { value: 'MONDAY', labelKey: 'organizationManaging.settings.reminder.days.monday' },
    { value: 'TUESDAY', labelKey: 'organizationManaging.settings.reminder.days.tuesday' },
    { value: 'WEDNESDAY', labelKey: 'organizationManaging.settings.reminder.days.wednesday' },
    { value: 'THURSDAY', labelKey: 'organizationManaging.settings.reminder.days.thursday' },
    { value: 'FRIDAY', labelKey: 'organizationManaging.settings.reminder.days.friday' },
    { value: 'SATURDAY', labelKey: 'organizationManaging.settings.reminder.days.saturday' },
    { value: 'SUNDAY', labelKey: 'organizationManaging.settings.reminder.days.sunday' },
  ] as const;

  private organizationId = '';
  protected readonly memberToConfirmRemoval = signal<string | null>(null);
  protected readonly teamToConfirmDeletion = signal<string | null>(null);
  protected readonly newOrganizationOwner = signal<string>('');
  protected readonly showTransferConfirm = signal(false);
  protected readonly isTransferring = signal(false);

  private readonly authService = inject(AuthService);
  protected readonly currentUserId = signal<string | null>(null);

  ngOnInit(): void {
    this.organizationId = this.route.snapshot.params['id'];

    const tabParam = this.route.snapshot.queryParams['tab'] as Tab;
    if (tabParam && (['members', 'curriculums', 'teams', 'settings'] as Tab[]).includes(tabParam)) {
      this.activeTab.set(tabParam);
    }

    this.loadOrganization();
    this.loadMembers();
    this.loadInvites();
    this.loadProfessions();
    this.loadTeams();
    this.loadReminder();

    this.authService.currentUser$.subscribe(user => {
      this.currentUserId.set(user?.id ?? null);
    });
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
    if (!this.showInviteForm()) {
      this.inviteEmail.set('');
      this.inviteRole.set('member');
      this.inviteSemester.set('1');
    }
  }

  protected invite(): void {
    const email = this.inviteEmail().trim();
    if (!email) return;

    this.isInviting.set(true);

    this.organizationService.createInvite(this.organizationId, email, this.inviteRole(), this.inviteSemester()).subscribe({
      next: () => {
        this.inviteEmail.set('');
        this.inviteRole.set('member');
        this.inviteSemester.set('1');
        this.isInviting.set(false);
        this.showInviteForm.set(false);
        this.toast.success(this.translate.instant('toast.inviteSent'));
        this.loadInvites();
      },
      error: () => {
        this.isInviting.set(false);
      },
    });
  }

  protected cancelInvite(invite: Invite): void {
    this.organizationService.deleteInvite(this.organizationId, invite.id).subscribe({
      next: () => {
        this.toast.success(this.translate.instant('toast.inviteCancelled'));
        this.loadInvites();
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

  private loadInvites(): void {
    this.organizationService.listInvites(this.organizationId).subscribe({
      next: (invites) => this.invites.set(invites.filter((i) => i.status === 'pending')),
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

  protected getRoleLabel(role: string): string {
    return role === 'admin'
      ? this.translate.instant('organizationManaging.members.roleAdmin')
      : this.translate.instant('organizationManaging.members.roleMember');
  }

  protected confirmRemoveMember(member: Profile, event: Event): void {
    event.stopPropagation();

    if (this.memberToConfirmRemoval() === member.id) {
      this.removeMember(member);
      this.memberToConfirmRemoval.set(null);
    } else {
      this.memberToConfirmRemoval.set(member.id);
    }
  }


protected confirmDeleteTeam(team: Team, event: Event): void {
  event.stopPropagation();

  if (this.teamToConfirmDeletion() === team.id) {
    this.deleteTeam(team);
    this.teamToConfirmDeletion.set(null);
  } else {
    this.teamToConfirmDeletion.set(team.id);
  }
}

  private loadReminder(): void {
    this.organizationService.getReminder(this.organizationId).subscribe({
      next: (reminder) => {
        if (reminder) {
          this.reminderChannel.set(reminder.channel);
          this.reminderSendDay.set(reminder.sendDay);
          this.reminderSendTime.set(reminder.sendTime);
          this.reminderIdleDays.set(reminder.idleDays);
        }
      },
    });
  }

  protected saveReminder(): void {
    this.isSavingReminder.set(true);
    this.organizationService.saveReminder(this.organizationId, {
      channel: this.reminderChannel(),
      sendTime: this.reminderSendTime(),
      idleDays: this.reminderIdleDays(),
      sendDay: this.reminderSendDay(),
    }).subscribe({
      next: () => {
        this.isSavingReminder.set(false);
        this.toast.success(this.translate.instant('toast.reminderSaved'));
      },
      error: () => {
        this.isSavingReminder.set(false);
      },
    });
  }

  @HostListener('document:click')
  protected transferOwnership(): void {
    const newOwnerId = this.newOrganizationOwner();
    if (!newOwnerId) return;

    this.isTransferring.set(true);
    this.organizationService.transferOwnership(this.organizationId, newOwnerId).subscribe({
      next: () => {
        this.toast.success(this.translate.instant('toast.ownershipTransferred'));
        this.showTransferConfirm.set(false);
        this.newOrganizationOwner.set('');
        this.isTransferring.set(false);
        this.loadOrganization();
      },
      error: () => this.isTransferring.set(false),
    });
  }


@HostListener('document:click')
    onDocumentClick(): void {
    this.memberToConfirmRemoval.set(null);
    this.teamToConfirmDeletion.set(null);
  }


}
