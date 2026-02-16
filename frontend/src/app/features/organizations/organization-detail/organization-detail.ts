import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { OrganizationService } from '@services/organization.service';
import { Organization } from '@app/core/models/organizations.models';
import { Profile } from '@app/core/models/profile.models';

@Component({
  selector: 'app-organization-detail',
  standalone: true,
  imports: [TranslateModule, FormsModule],
  templateUrl: './organization-detail.html',
})
export class OrganizationDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly organizationService = inject(OrganizationService);

  protected readonly organization = signal<Organization | null>(null);
  protected readonly members = signal<Profile[]>([]);
  protected readonly inviteUserId = signal('');
  protected readonly inviteRole = signal('member');
  protected readonly isInviting = signal(false);
  protected readonly inviteError = signal(false);
  protected readonly inviteSuccess = signal(false);

  private organizationId = '';

  ngOnInit(): void {
    this.organizationId = this.route.snapshot.params['id'];
    this.loadOrganization();
    this.loadMembers();
  }

  protected invite(): void {
    const userId = this.inviteUserId().trim();
    if (!userId) return;

    this.isInviting.set(true);
    this.inviteError.set(false);
    this.inviteSuccess.set(false);

    this.organizationService.inviteToOrganization(this.organizationId, userId, this.inviteRole()).subscribe({
      next: () => {
        this.inviteUserId.set('');
        this.inviteRole.set('member');
        this.inviteSuccess.set(true);
        this.isInviting.set(false);
        this.loadMembers();
      },
      error: () => {
        this.inviteError.set(true);
        this.isInviting.set(false);
      },
    });
  }

  protected goBack(): void {
    this.router.navigate(['/organizations']);
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
}
