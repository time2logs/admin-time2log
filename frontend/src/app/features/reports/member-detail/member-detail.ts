import { Location } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { OrganizationService } from '@services/organization.service';
import { ReportService } from '@services/report.service';
import { TeamService } from '@services/team.service';
import { Profile } from '@app/core/models/profile.models';
import { Team } from '@app/core/models/team.models';
import { CurriculumOverview, MemberActivityRecord, ReportStatus } from '@app/core/models/report.models';
import { Calendar } from '@app/shared/calendar/calendar';
import { formatLocalDate } from '@app/shared/utils/date.utils';

interface TeamCompetencyGroup {
  teamId: string | null;
  teamName: string;
  curriculum: CurriculumOverview | null;
  activityProgress: { id: string; label: string; hours: number }[];
  maxActivityHours: number;
  competencyHours: Map<string, number>;
  maxCompetencyHours: number;
}

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
  private readonly teamService = inject(TeamService);

  protected readonly member = signal<Profile | null>(null);
  protected readonly selectedDate = signal(formatLocalDate(new Date()));
  protected readonly monthRecords = signal<MemberActivityRecord[]>([]);
  protected readonly allRecords = signal<MemberActivityRecord[]>([]);
  protected readonly selectedDayRecords = signal<MemberActivityRecord[]>([]);
  protected readonly teams = signal<Team[]>([]);
  protected readonly curriculaByProfession = signal<Map<string, CurriculumOverview>>(new Map());
  protected readonly fallbackProfessionId = signal<string | null>(null);
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

    const today = formatLocalDate(new Date());
    const year = this.currentYear();
    const month = this.currentMonth();
    const lastDay = new Date(year, month + 1, 0).getDate();
    for (let d = 1; d <= lastDay; d++) {
      const date = new Date(year, month, d);
      const dateStr = formatLocalDate(date);
      if (dateStr >= today) break;
      const dow = date.getDay();
      if (dow === 0 || dow === 6) continue;
      if (!map[dateStr]) {
        map[dateStr] = 'missing';
      }
    }
    return map;
  });

  protected readonly currentYear = signal(new Date().getFullYear());
  protected readonly currentMonth = signal(new Date().getMonth());

  protected readonly teamGroups = computed<TeamCompetencyGroup[]>(() => {
    const records = this.allRecords();
    const teams = this.teams();
    const curricula = this.curriculaByProfession();
    const fallbackProfessionId = this.fallbackProfessionId();

    const byTeam = new Map<string | null, MemberActivityRecord[]>();
    for (const r of records) {
      const key = r.teamId ?? null;
      const arr = byTeam.get(key) ?? [];
      arr.push(r);
      byTeam.set(key, arr);
    }

    const groups: TeamCompetencyGroup[] = [];
    for (const [teamId, recs] of byTeam) {
      const activityMap = new Map<string, { label: string; hours: number }>();
      for (const r of recs) {
        if (!r.curriculumActivityId) continue;
        const entry = activityMap.get(r.curriculumActivityId);
        if (entry) {
          entry.hours += r.hours;
        } else {
          activityMap.set(r.curriculumActivityId, { label: r.activityLabel || '—', hours: r.hours });
        }
      }
      const activityProgress = Array.from(activityMap.entries())
        .map(([id, { label, hours }]) => ({ id, label, hours }))
        .sort((a, b) => b.hours - a.hours);

      if (activityProgress.length === 0) continue;

      const team = teamId ? teams.find(t => t.id === teamId) : null;
      const curriculum = team
        ? curricula.get(team.professionId) ?? null
        : fallbackProfessionId ? curricula.get(fallbackProfessionId) ?? null : null;

      const competencyHours = new Map<string, number>();
      if (curriculum) {
        const hoursById = new Map(activityProgress.map(a => [a.id, a.hours]));
        for (const node of curriculum.nodes) {
          if (node.nodeType !== 'activity') continue;
          const h = hoursById.get(node.id) ?? 0;
          if (h === 0) continue;
          for (const cid of node.competencyIds) {
            competencyHours.set(cid, (competencyHours.get(cid) ?? 0) + h);
          }
        }
      }

      groups.push({
        teamId,
        teamName: team?.name ?? 'Ohne Team',
        curriculum,
        activityProgress,
        maxActivityHours: Math.max(1, ...activityProgress.map(a => a.hours)),
        competencyHours,
        maxCompetencyHours: Math.max(
          1,
          ...(curriculum?.competencies.map(c => competencyHours.get(c.id) ?? 0) ?? [])
        ),
      });
    }

    return groups.sort((a, b) => a.teamName.localeCompare(b.teamName));
  });

  private organizationId = '';
  private userId = '';

  ngOnInit(): void {
    this.userId = this.route.snapshot.params['userId'];
    this.organizationId = this.route.snapshot.queryParams['organizationId'] ?? '';

    this.loadMember();
    this.loadTeamsAndCurricula();
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
    this.organizationService.getOnlyOrganizationMembers(this.organizationId).subscribe({
      next: (members) => {
        const m = members.find(p => p.id === this.userId);
        if (m) this.member.set(m);
      },
    });
  }

  private loadTeamsAndCurricula(): void {
    this.teamService.getTeams(this.organizationId).subscribe({
      next: (teams) => {
        this.teams.set(teams);
        const professionIds = new Set(teams.map(t => t.professionId));
        this.organizationService.getProfessions(this.organizationId).subscribe({
          next: (professions) => {
            if (professions.length > 0) {
              this.fallbackProfessionId.set(professions[0].id);
              professionIds.add(professions[0].id);
            }
            for (const professionId of professionIds) {
              this.reportService.getCurriculum(this.organizationId, professionId).subscribe({
                next: (curriculum) => {
                  const next = new Map(this.curriculaByProfession());
                  next.set(professionId, curriculum);
                  this.curriculaByProfession.set(next);
                },
              });
            }
          },
        });
      },
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
