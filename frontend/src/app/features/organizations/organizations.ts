import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { OrganizationService } from '@services/organization.service';
import { Organization } from '@app/core/models/organizations.models';

@Component({
  selector: 'app-organizations',
  standalone: true,
  imports: [TranslateModule, FormsModule],
  templateUrl: './organizations.html',
})
export class OrganizationsComponent implements OnInit {
  private readonly organizationService = inject(OrganizationService);
  private readonly router = inject(Router);

  protected readonly organizations = signal<Organization[]>([]);
  protected readonly newName = signal('');
  protected readonly isCreating = signal(false);

  ngOnInit(): void {
    this.loadOrganizations();
  }

  protected createOrganization(): void {
    const name = this.newName().trim();
    if (!name) return;

    this.isCreating.set(true);
    this.organizationService.createOrganization(name).subscribe({
      next: () => {
        this.newName.set('');
        this.loadOrganizations();
        this.isCreating.set(false);
      },
      error: () => this.isCreating.set(false),
    });
  }

  protected openOrganization(org: Organization): void {
    this.router.navigate(['/organizations', org.id]);
  }

  private loadOrganizations(): void {
    this.organizationService.getOrganizations().subscribe({
      next: (orgs) => this.organizations.set(orgs),
    });
  }
}
