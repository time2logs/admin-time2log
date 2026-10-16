import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { finalize } from 'rxjs';
import { OrganizationService } from '@services/organization.service';
import { MentorService } from '@services/mentor.service';
import { Profile } from '@app/core/models/profile.models';

@Component({
  selector: 'app-mentor-managing',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './mentor-managing.html',
})
export class MentorManaging implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly organizationService = inject(OrganizationService);
  private readonly mentorService = inject(MentorService);

  protected readonly mentor = signal<Profile | null>(null);
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
  private mentorId = '';

  ngOnInit(): void {
    this.organizationId = this.route.snapshot.params['id'];
    this.mentorId = this.route.snapshot.params['mentorId'];
    this.loadMentor();
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
      ? this.mentorService.unassignLearner(this.organizationId, this.mentorId, learner.id)
      : this.mentorService.assignLearner(this.organizationId, this.mentorId, learner.id);

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

  private loadMentor(): void {
    this.organizationService.getOrganizationMembers(this.organizationId).subscribe({
      next: (members) => this.mentor.set(members.find((m) => m.id === this.mentorId) ?? null),
    });
  }

  private loadLearners(): void {
    this.organizationService.getOnlyOrganizationMembers(this.organizationId).subscribe({
      next: (learners) => this.learners.set(learners),
    });
  }

  private loadAssignedLearners(): void {
    this.mentorService.getAssignedLearners(this.organizationId, this.mentorId).subscribe({
      next: (learners) => this.assignedIds.set(new Set(learners.map((l) => l.id))),
    });
  }
}
