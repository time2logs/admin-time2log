import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { OrganizationService } from '@services/organization.service';
import { ModeratorService } from '@services/moderator.service';
import { Profile } from '@app/core/models/profile.models';

@Component({
  selector: 'app-moderator-managing',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './moderator-managing.html',
})
export class ModeratorManaging implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly organizationService = inject(OrganizationService);
  private readonly moderatorService = inject(ModeratorService);

  protected readonly moderator = signal<Profile | null>(null);
  protected readonly learners = signal<Profile[]>([]);
  protected readonly assignedIds = signal<Set<string>>(new Set());
  protected readonly togglingId = signal<string | null>(null);

  protected readonly selectionList = computed(() => {
    const ids = this.assignedIds();
    return this.learners().map((l) => ({
      ...l,
      assigned: ids.has(l.id),
    }));
  });

  protected organizationId = '';
  private moderatorId = '';

  ngOnInit(): void {
    this.organizationId = this.route.snapshot.params['id'];
    this.moderatorId = this.route.snapshot.params['moderatorId'];
    this.loadModerator();
    this.loadLearners();
    this.loadAssignedLearners();
  }

  protected goBack(): void {
    this.router.navigate(['/organizations', this.organizationId], { queryParams: { tab: 'members' } });
  }

  protected toggleLearner(learner: Profile & { assigned: boolean }): void {
    if (this.togglingId()) return;

    this.togglingId.set(learner.id);

    const request$ = learner.assigned
      ? this.moderatorService.unassignLearner(this.organizationId, this.moderatorId, learner.id)
      : this.moderatorService.assignLearner(this.organizationId, this.moderatorId, learner.id);

    request$.pipe(finalize(() => this.togglingId.set(null))).subscribe(() => {
      this.assignedIds.update((ids) => {
        const next = new Set(ids);
        if (learner.assigned) {
          next.delete(learner.id);
        } else {
          next.add(learner.id);
        }
        return next;
      });
    });
  }

  private loadModerator(): void {
    this.organizationService.getOrganizationMembers(this.organizationId).subscribe({
      next: (members) => this.moderator.set(members.find((m) => m.id === this.moderatorId) ?? null),
    });
  }

  private loadLearners(): void {
    this.organizationService.getOnlyOrganizationMembers(this.organizationId).subscribe({
      next: (learners) => this.learners.set(learners),
    });
  }

  private loadAssignedLearners(): void {
    this.moderatorService.getAssignedLearners(this.organizationId, this.moderatorId).subscribe({
      next: (learners) => this.assignedIds.set(new Set(learners.map((l) => l.id))),
    });
  }
}
