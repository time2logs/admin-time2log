import { Component, inject, OnInit, signal } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { OrganizationService } from '@services/organization.service';
import { Organization } from '@app/core/models/organizations.models';
import { Profile } from '@app/core/models/profile.models';
import { Reports } from '@app/features/reports/reports/reports';

@Component({
  selector: 'app-reports-page',
  standalone: true,
  imports: [TranslateModule, Reports],
  templateUrl: './reports-page.html',
})
export class ReportsPage implements OnInit {
  private readonly organizationService = inject(OrganizationService);

  protected readonly organizations = signal<Organization[]>([]);
  protected readonly selectedOrgId = signal('');
  protected readonly onlyMembers = signal<Profile[]>([]);

  ngOnInit(): void {
    this.organizationService.getOrganizations().subscribe({
      next: (orgs) => {
        this.organizations.set(orgs);
        if (orgs.length === 1) {
          this.selectOrg(orgs[0].id);
        }
      },
    });
  }

  protected selectOrg(orgId: string): void {
    this.selectedOrgId.set(orgId);
    this.organizationService.getOnlyOrganizationMembers(orgId).subscribe({
      next: (members) => this.onlyMembers.set(members),
    });
  }
}
