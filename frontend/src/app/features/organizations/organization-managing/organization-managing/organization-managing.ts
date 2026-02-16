import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { OrganizationService } from '@services/organization.service';
import { ActivityService } from '@services/activity.service';
import { ToastService } from '@services/toast.service';
import { Organization } from '@app/core/models/organizations.models';
import { Profile } from '@app/core/models/profile.models';
import { PreDefinedActivity } from '@app/core/models/activity.models';

type Tab = 'members' | 'activities' | 'settings';

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
  private readonly activityService = inject(ActivityService);
  private readonly toast = inject(ToastService);
  private readonly translate = inject(TranslateService);

  protected readonly activeTab = signal<Tab>('members');
  protected readonly organization = signal<Organization | null>(null);

  protected readonly members = signal<Profile[]>([]);
  protected readonly showInviteForm = signal(false);
  protected readonly inviteUserId = signal('');
  protected readonly inviteRole = signal('member');
  protected readonly isInviting = signal(false);

  protected readonly activities = signal<PreDefinedActivity[]>([]);
  protected readonly showActivityForm = signal(false);
  protected readonly editingActivity = signal<PreDefinedActivity | null>(null);
  protected readonly activityKey = signal('');
  protected readonly activityLabel = signal('');
  protected readonly activityDescription = signal('');
  protected readonly activityCategory = signal('');
  protected readonly activityIsActive = signal(true);

  protected readonly showDeleteConfirm = signal(false);
  protected readonly isDeleting = signal(false);

  private organizationId = '';

  ngOnInit(): void {
    this.organizationId = this.route.snapshot.params['id'];
    this.loadOrganization();
    this.loadMembers();
    this.loadActivities();
  }

  protected setTab(tab: Tab): void {
    this.activeTab.set(tab);
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

  protected openAddActivityForm(): void {
    this.editingActivity.set(null);
    this.activityKey.set('');
    this.activityLabel.set('');
    this.activityDescription.set('');
    this.activityCategory.set('');
    this.activityIsActive.set(true);
    this.showActivityForm.set(true);
  }

  protected openEditActivityForm(activity: PreDefinedActivity): void {
    this.editingActivity.set(activity);
    this.activityKey.set(activity.key);
    this.activityLabel.set(activity.label);
    this.activityDescription.set(activity.description ?? '');
    this.activityCategory.set(activity.category ?? '');
    this.activityIsActive.set(activity.isActive);
    this.showActivityForm.set(true);
  }

  protected cancelActivityForm(): void {
    this.showActivityForm.set(false);
    this.editingActivity.set(null);
  }

  protected saveActivity(): void {
    const editing = this.editingActivity();
    if (editing) {
      this.activityService
        .updateActivity(this.organizationId, editing.id, {
          key: this.activityKey(),
          label: this.activityLabel(),
          description: this.activityDescription(),
          category: this.activityCategory(),
          isActive: this.activityIsActive(),
        })
        .subscribe({
          next: () => {
            this.showActivityForm.set(false);
            this.editingActivity.set(null);
            this.toast.success(this.translate.instant('toast.activityUpdated'));
            this.loadActivities();
          },
        });
    } else {
      this.activityService
        .createActivity(this.organizationId, {
          key: this.activityKey(),
          label: this.activityLabel(),
          description: this.activityDescription(),
          category: this.activityCategory(),
        })
        .subscribe({
          next: () => {
            this.showActivityForm.set(false);
            this.toast.success(this.translate.instant('toast.activityCreated'));
            this.loadActivities();
          },
        });
    }
  }

  protected deleteActivity(activity: PreDefinedActivity): void {
    this.activityService.deleteActivity(this.organizationId, activity.id).subscribe({
      next: () => {
        this.toast.success(this.translate.instant('toast.activityDeleted'));
        this.loadActivities();
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

  private loadActivities(): void {
    this.activityService.getActivities(this.organizationId).subscribe({
      next: (activities) => this.activities.set(activities),
    });
  }
}
