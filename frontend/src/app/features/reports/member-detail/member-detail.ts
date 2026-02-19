import { Location } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { OrganizationService } from '@services/organization.service';
import { ReportService } from '@services/report.service';
import { Profile } from '@app/core/models/profile.models';
import { CurriculumOverview, MemberActivityRecord, ReportStatus } from '@app/core/models/report.models';
import { Calendar } from '@app/shared/calendar/calendar';
import { Profession } from '@app/core/models/organizations.models';

@Component({
  selector: 'app-member-detail',
  standalone: true,
  imports: [TranslateModule, Calendar],
  templateUrl: './member-detail.html',
})
export class MemberDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly location = inject(Location);
  private readonly organizationService = inject(OrganizationService);
  private readonly reportService = inject(ReportService);

  protected readonly member = signal<Profile | null>(null);
  protected readonly selectedDate = signal(new Date().toISOString().slice(0, 10));
  protected readonly monthRecords = signal<MemberActivityRecord[]>([]);
  protected readonly allRecords = signal<MemberActivityRecord[]>([]);
  protected readonly selectedDayRecords = signal<MemberActivityRecord[]>([]);
  protected readonly curriculum = signal<CurriculumOverview | null>(null);
  protected readonly isLoadingRecords = signal(false);

  protected readonly statusMap = computed<Record<string, ReportStatus>>(() => {
    const map: Record<string, ReportStatus> = {};
    const byDate = new Map<string, MemberActivityRecord[]>();
    for (const r of this.monthRecords()) {
      const arr = byDate.get(r.entryDate) ?? [];
      arr.push(r);
      byDate.set(r.entryDate, arr);
    }
    for (const [date, records] of byDate) {
      const hasBad = records.some(r => r.rating !== null && r.rating <= 2);
      map[date] = hasBad ? 'bad_rating' : 'reported';
    }

    // Mark past weekdays without records as 'missing'
    const today = new Date().toISOString().slice(0, 10);
    const year = this.currentYear();
    const month = this.currentMonth();
    const lastDay = new Date(year, month + 1, 0).getDate();
    for (let d = 1; d <= lastDay; d++) {
      const date = new Date(year, month, d);
      const dateStr = date.toISOString().slice(0, 10);
      if (dateStr >= today) break;
      const dow = date.getDay();
      if (dow === 0 || dow === 6) continue; // skip weekends
      if (!map[dateStr]) {
        map[dateStr] = 'missing';
      }
    }
    return map;
  });

  protected readonly currentYear = signal(new Date().getFullYear());
  protected readonly currentMonth = signal(new Date().getMonth());

  protected readonly activityProgress = computed<{ id: string; label: string; hours: number }[]>(() => {
    const map = new Map<string, { label: string; hours: number }>();
    for (const r of this.allRecords()) {
      if (!r.curriculumActivityId) continue;
      const entry = map.get(r.curriculumActivityId);
      if (entry) {
        entry.hours += r.hours;
      } else {
        map.set(r.curriculumActivityId, { label: r.activityLabel || '—', hours: r.hours });
      }
    }
    return Array.from(map.entries())
      .map(([id, { label, hours }]) => ({ id, label, hours }))
      .sort((a, b) => b.hours - a.hours);
  });

  protected readonly maxActivityHours = computed(() =>
    Math.max(1, ...this.activityProgress().map(a => a.hours))
  );

  protected readonly competencyHours = computed<Map<string, number>>(() => {
    const curriculum = this.curriculum();
    if (!curriculum) return new Map();
    const activityMap = new Map(this.activityProgress().map(a => [a.id, a.hours]));
    const map = new Map<string, number>();
    for (const node of curriculum.nodes) {
      if (node.nodeType !== 'activity') continue;
      const h = activityMap.get(node.id) ?? 0;
      if (h === 0) continue;
      for (const cid of node.competencyIds) {
        map.set(cid, (map.get(cid) ?? 0) + h);
      }
    }
    return map;
  });

  protected readonly maxCompetencyHours = computed(() => {
    const vals = this.curriculum()?.competencies.map(c => this.competencyHours().get(c.id) ?? 0) ?? [];
    return Math.max(1, ...vals);
  });

  private organizationId = '';
  private userId = '';

  ngOnInit(): void {
    this.userId = this.route.snapshot.params['userId'];
    this.organizationId = this.route.snapshot.queryParams['organizationId'] ?? '';

    this.loadMember();
    this.loadCurriculum();
    this.loadMonthRecords(this.selectedDate());
    this.loadAllRecords();
    this.loadDayRecords(this.selectedDate());
  }

  protected goBack(): void {
    this.location.back();
  }

  protected onDateSelected(date: string): void {
    this.selectedDate.set(date);
    this.loadDayRecords(date);
  }

  protected onMonthChanged(event: { year: number; month: number }): void {
    this.currentYear.set(event.year);
    this.currentMonth.set(event.month - 1);
    const from = `${event.year}-${String(event.month).padStart(2, '0')}-01`;
    const lastDay = new Date(event.year, event.month, 0).getDate();
    const to = `${event.year}-${String(event.month).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`;
    this.loadMonthRecords(from, to);
  }

  private loadMember(): void {
    this.organizationService.getOrganizationMembers(this.organizationId).subscribe({
      next: (members) => {
        const m = members.find(p => p.id === this.userId);
        if (m) this.member.set(m);
      },
    });
  }

  private loadCurriculum(): void {
    this.organizationService.getProfessions(this.organizationId).subscribe({
      next: (professions) => this.loadCurriculumForFirstProfession(professions),
    });
  }

  private loadCurriculumForFirstProfession(professions: Profession[]): void {
    if (professions.length === 0) return;
    this.reportService.getCurriculum(this.organizationId, professions[0].id).subscribe({
      next: (curriculum) => this.curriculum.set(curriculum),
    });
  }

  private loadMonthRecords(dateOrFrom: string, to?: string): void {
    const d = new Date(dateOrFrom);
    const year = d.getFullYear();
    const month = d.getMonth() + 1;
    const from = to ? dateOrFrom : `${year}-${String(month).padStart(2, '0')}-01`;
    const toDate = to ?? `${year}-${String(month).padStart(2, '0')}-${new Date(year, month, 0).getDate()}`;

    this.reportService.getMemberRecordsByRange(this.organizationId, this.userId, from, toDate).subscribe({
      next: (records) => this.monthRecords.set(records),
    });
  }

  private loadAllRecords(): void {
    this.reportService.getMemberRecordsByRange(this.organizationId, this.userId, '2020-01-01', '2099-12-31').subscribe({
      next: (records) => this.allRecords.set(records),
    });
  }

  private loadDayRecords(date: string): void {
    this.isLoadingRecords.set(true);
    this.reportService.getMemberRecordsByDate(this.organizationId, this.userId, date).subscribe({
      next: (records) => {
        this.selectedDayRecords.set(records);
        this.isLoadingRecords.set(false);
      },
      error: () => this.isLoadingRecords.set(false),
    });
  }
}
