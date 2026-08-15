import { DatePipe, Location } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { NgxChartsModule, Color, ScaleType } from '@swimlane/ngx-charts';
import { OrganizationService } from '@services/organization.service';
import { PaletteService } from '@services/palette.service';
import { ReportService } from '@services/report.service';
import { TeamService } from '@services/team.service';
import { Profile } from '@app/core/models/profile.models';
import { Team } from '@app/core/models/team.models';
import { ABSENCE_TYPES, ABSENCE_TYPE_BY_ID, CurriculumOverview, DEFAULT_ABSENCE_COLOR, MemberAbsence, MemberActivityRecord, NgxChartEntry, ReportStatus } from '@app/core/models/report.models';
import { Calendar } from '@app/shared/calendar/calendar';
import { FormatHoursPipe } from '@app/shared/pipes/format-hours.pipe';
import { formatLocalDate } from '@app/shared/utils/date.utils';
import { roundHours } from '@app/shared/utils/format-hours.utils';

const OPEN_ACTIVITY_HOURS_THRESHOLD = 10;

type OpenActivityFilter = 'notPerformed' | 'underThreshold';

interface ActivityHours {
  id: string;
  label: string;
  hours: number;
}

function mergeActivitiesByLabel(activities: ActivityHours[]): ActivityHours[] {
  const byLabel = new Map<string, ActivityHours>();
  for (const activity of activities) {
    const key = activity.label.trim().toLocaleLowerCase();
    const merged = byLabel.get(key);
    if (merged) {
      merged.hours = roundHours(merged.hours + activity.hours);
    } else {
      byLabel.set(key, { ...activity });
    }
  }
  return Array.from(byLabel.values());
}

interface TeamCompetencyGroup {
  teamId: string | null;
  teamName: string;
  curriculum: CurriculumOverview | null;
  activityProgress: ActivityHours[];
  maxActivityHours: number;
  competencyHours: Map<string, number>;
  maxCompetencyHours: number;
  underThresholdActivities: ActivityHours[];
  notPerformedActivities: ActivityHours[];
  hasCurriculumActivities: boolean;
}

@Component({
  selector: 'app-member-detail',
  standalone: true,
  imports: [TranslatePipe, Calendar, NgxChartsModule, DatePipe, FormatHoursPipe],
  templateUrl: './member-detail.html',
})
export class MemberDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly location = inject(Location);
  private readonly organizationService = inject(OrganizationService);
  private readonly reportService = inject(ReportService);
  private readonly teamService = inject(TeamService);
  private readonly translate = inject(TranslateService);
  private readonly paletteService = inject(PaletteService);

  protected readonly member = signal<Profile | null>(null);
  protected readonly selectedDate = signal(formatLocalDate(new Date()));
  protected readonly monthRecords = signal<MemberActivityRecord[]>([]);
  protected readonly allRecords = signal<MemberActivityRecord[]>([]);
  protected readonly selectedDayRecords = signal<MemberActivityRecord[]>([]);
  protected readonly teams = signal<Team[]>([]);
  protected readonly curriculaByProfession = signal<Map<string, CurriculumOverview>>(new Map());
  protected readonly fallbackProfessionId = signal<string | null>(null);
  protected readonly isLoadingRecords = signal(false);
  protected readonly selectedLocation = signal('');
  protected readonly availableLocations = signal<string[]>([]);
  protected readonly hasLocationOptions = computed(() => this.availableLocations().length > 0);

  protected readonly openActivityFilter = signal<OpenActivityFilter>('notPerformed');
  protected readonly openActivityThreshold = OPEN_ACTIVITY_HOURS_THRESHOLD;

  /** Alle Absenzen des Members (ungefiltert); Filterung erfolgt clientseitig. */
  protected readonly absences = signal<MemberAbsence[]>([]);
  protected readonly selectedAbsenceSemester = signal('');

  /** Auswählbare Semester aus den vorhandenen Absenzen (nicht aus activity_records). */
  protected readonly availableSemesters = computed(() =>
    [...new Set(
      this.absences()
        .map(a => a.currentSemester)
        .filter((s): s is string => s !== null && s !== '')
    )].sort()
  );

  /** Nach gewähltem Semester gefilterte Absenzen ('' = alle). */
  private readonly filteredAbsences = computed(() => {
    const semester = this.selectedAbsenceSemester();
    const all = this.absences();
    return semester ? all.filter(a => a.currentSemester === semester) : all;
  });

  /** Tage je Absenz-Typ (nur belegte Typen), Basis für Diagramm und Farbschema. */
  private readonly absenceTotals = computed(() => {
    const days = new Map<string, number>();
    for (const a of this.filteredAbsences()) {
      days.set(a.absenceTypeId, (days.get(a.absenceTypeId) ?? 0) + this.absenceDays(a));
    }
    return ABSENCE_TYPES
      .map(meta => ({ meta, days: Math.round((days.get(meta.id) ?? 0) * 100) / 100 }))
      .filter(e => e.days > 0);
  });

  protected readonly absenceChartData = computed<NgxChartEntry[]>(() =>
    this.absenceTotals().map(e => ({ name: this.translate.instant(e.meta.labelKey), value: e.days }))
  );

  protected readonly absenceColorScheme = computed<Color>(() => ({
    name: 'absence',
    selectable: true,
    group: ScaleType.Ordinal,
    domain: this.paletteService.domain(),
  }));

  /**
   * Farbe je Absenz-Typ aus der gewählten Palette, in Säulen-Reihenfolge —
   * so stimmen die Punkte in der Liste mit den Säulen im Diagramm überein.
   */
  private readonly absenceTypeColor = computed(() => {
    const palette = this.paletteService.domain();
    const map = new Map<string, string>();
    this.absenceTotals().forEach((e, i) => map.set(e.meta.id, palette[i % palette.length]));
    return map;
  });

  protected readonly absenceEntries = computed(() => {
    const colorByType = this.absenceTypeColor();
    return this.filteredAbsences().map(a => {
      const meta = ABSENCE_TYPE_BY_ID.get(a.absenceTypeId);
      return {
        id: a.id,
        typeLabel: meta ? this.translate.instant(meta.labelKey) : a.absenceTypeId,
        color: colorByType.get(a.absenceTypeId) ?? DEFAULT_ABSENCE_COLOR,
        startDate: a.startDate,
        endDate: a.endDate,
        isRange: a.startDate !== a.endDate,
        isRecurring: a.isRecurring,
        dayFraction: a.dayFraction,
        notes: a.notes,
      };
    });
  });

  protected readonly statusMap = computed<Record<string, ReportStatus>>(() => {
    const map: Record<string, ReportStatus> = {};
    const byDate = new Map<string, MemberActivityRecord[]>();
    for (const r of this.monthRecords()) {
      const arr = byDate.get(r.entryDate) ?? [];
      arr.push(r);
      byDate.set(r.entryDate, arr);
    }
    for (const [date, records] of byDate) {
      const totalHours = roundHours(records.reduce((sum, r) => sum + r.hours, 0));
      const hasBad = records.some(r => r.rating !== null && r.rating <= 2);
      const underTarget = totalHours < this.targetHours();
      if (hasBad && underTarget) {
        map[date] = 'bad_rating_under_target';
      } else if (hasBad) {
        map[date] = 'bad_rating';
      } else if (underTarget) {
        map[date] = 'under_target';
      } else {
        map[date] = 'reported';
      }
    }

    const year = this.currentYear();
    const month = this.currentMonth();
    const lastDay = new Date(year, month + 1, 0).getDate();

    // Abwesenheitstage markieren (haben Vorrang vor "fehlend", aber nicht vor Einträgen).
    for (let d = 1; d <= lastDay; d++) {
      const dateStr = formatLocalDate(new Date(year, month, d));
      if (map[dateStr]) continue;
      if (this.absences().some(a => this.absenceCoversDate(a, dateStr))) {
        map[dateStr] = 'absence';
      }
    }

    const today = formatLocalDate(new Date());
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

  /** Abwesenheiten, die auf den aktuell gewählten Tag fallen (Semesterfilter ignoriert). */
  protected readonly selectedDayAbsences = computed(() => {
    const date = this.selectedDate();
    const colorByType = this.absenceTypeColor();
    return this.absences()
      .filter(a => this.absenceCoversDate(a, date))
      .map(a => {
        const meta = ABSENCE_TYPE_BY_ID.get(a.absenceTypeId);
        return {
          id: a.id,
          typeLabel: meta ? this.translate.instant(meta.labelKey) : a.absenceTypeId,
          color: colorByType.get(a.absenceTypeId) ?? DEFAULT_ABSENCE_COLOR,
          dayFraction: a.dayFraction,
          isRecurring: a.isRecurring,
          notes: a.notes,
        };
      });
  });

  protected readonly currentYear = signal(new Date().getFullYear());
  protected readonly currentMonth = signal(new Date().getMonth());
  protected readonly targetHours = signal(8);

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
          entry.hours = roundHours(entry.hours + r.hours);
        } else {
          activityMap.set(r.curriculumActivityId, { label: r.activityLabel || '—', hours: roundHours(r.hours) });
        }
      }
      const team = teamId ? teams.find(t => t.id === teamId) : null;
      const curriculum = team
        ? curricula.get(team.professionId) ?? null
        : fallbackProfessionId ? curricula.get(fallbackProfessionId) ?? null : null;

      const hoursById = new Map(Array.from(activityMap, ([id, entry]) => [id, entry.hours] as const));

      const curriculumActivities: ActivityHours[] = (curriculum?.nodes ?? [])
        .filter(node => node.nodeType === 'activity')
        .sort((a, b) => a.sortOrder - b.sortOrder)
        .map(node => ({ id: node.id, label: node.label, hours: hoursById.get(node.id) ?? 0 }));

      const labelById = new Map(curriculumActivities.map(a => [a.id, a.label]));

      const activityProgress = Array.from(activityMap.entries())
        .map(([id, { label, hours }]) => ({ id, label: labelById.get(id) ?? label, hours }))
        .sort((a, b) => b.hours - a.hours);

      if (activityProgress.length === 0 && curriculumActivities.length === 0) continue;

      const underThresholdActivities = mergeActivitiesByLabel(curriculumActivities)
        .filter(a => a.hours < OPEN_ACTIVITY_HOURS_THRESHOLD)
        .sort((a, b) => a.hours - b.hours);

      const competencyHours = new Map<string, number>();
      if (curriculum) {
        for (const node of curriculum.nodes) {
          if (node.nodeType !== 'activity') continue;
          const h = hoursById.get(node.id) ?? 0;
          if (Math.abs(h) < 0.0001) continue;
          for (const cid of node.competencyIds) {
            competencyHours.set(cid, roundHours((competencyHours.get(cid) ?? 0) + h));
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
        underThresholdActivities,
        notPerformedActivities: underThresholdActivities.filter(a => Math.abs(a.hours) < 0.0001),
        hasCurriculumActivities: curriculumActivities.length > 0,
      });
    }

    return groups.sort((a, b) => a.teamName.localeCompare(b.teamName));
  });

  private organizationId = '';
  private userId = '';

  ngOnInit(): void {
    this.userId = this.route.snapshot.params['userId'];
    this.organizationId = this.route.snapshot.queryParams['organizationId'] ?? '';

    this.loadTargetHours();
    this.loadMember();
    this.loadTeamsAndCurricula();
    this.loadMonthRecords(this.selectedDate());
    this.loadAllRecords();
    this.loadAbsences();
  }

  protected onAbsenceSemesterSelected(semester: string): void {
    this.selectedAbsenceSemester.set(semester);
  }

  private loadTargetHours(): void {
    if (!this.organizationId) return;
    this.organizationService.getTargetHours(this.organizationId).subscribe({
      next: (res) => this.targetHours.set(res.targetHours ?? 8),
    });
  }

  private loadAbsences(): void {
    this.reportService.getMemberAbsences(this.organizationId, this.userId).subscribe({
      next: (absences) => this.absences.set(absences),
    });
  }

  /**
   * Anzahl abwesender Tage einer Absenz × Tagesbruchteil. Ohne rrule zählt der
   * volle Bereich [start, end]; mit rrule nur die passenden Wochentage (BYDAY).
   */
  private absenceDays(a: MemberAbsence): number {
    const start = new Date(a.startDate + 'T00:00:00');
    const end = new Date(a.endDate + 'T00:00:00');
    if (isNaN(start.getTime()) || isNaN(end.getTime()) || end < start) return 0;

    const byday = this.parseByDay(a.rrule);
    let count = 0;
    for (const d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
      if (byday && !byday.has(d.getDay())) continue;
      count++;
    }
    return count * (a.dayFraction || 1);
  }

  /**
   * Ob eine Absenz den gegebenen Tag ('YYYY-MM-DD') abdeckt: innerhalb [start, end]
   * und – falls eine rrule mit BYDAY vorliegt – am passenden Wochentag.
   */
  private absenceCoversDate(a: MemberAbsence, date: string): boolean {
    if (date < a.startDate || date > a.endDate) return false;
    const byday = this.parseByDay(a.rrule);
    if (!byday) return true;
    return byday.has(new Date(date + 'T00:00:00').getDay());
  }

  /** Parst BYDAY (z. B. "MO,WE" oder "2MO") aus einer iCal-rrule zu Wochentagen (0=So). */
  private parseByDay(rrule: string | null): Set<number> | null {
    if (!rrule) return null;
    const match = /BYDAY=([^;]+)/i.exec(rrule);
    if (!match) return null;
    const map: Record<string, number> = { SU: 0, MO: 1, TU: 2, WE: 3, TH: 4, FR: 5, SA: 6 };
    const days = match[1]
      .split(',')
      .map(token => map[token.trim().toUpperCase().slice(-2)])
      .filter((n): n is number => n !== undefined);
    return days.length ? new Set(days) : null;
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

  protected onOpenActivityFilterSelected(filter: OpenActivityFilter): void {
    this.openActivityFilter.set(filter);
  }

  protected onLocationSelected(location: string): void {
    this.selectedLocation.set(location);
    this.loadAllRecords();
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
      next: (records) => {
        this.monthRecords.set(records);
        if (!to) this.setSelectedDayFromMonth(records, this.selectedDate());
      },
    });
  }

  private normalizeLocationOptions(records: MemberActivityRecord[]): string[] {
    const byNormalized = new Map<string, string>();
    for (const record of records) {
      const label = record.location?.trim();
      if (!label) continue;
      const key = label.toLocaleLowerCase();
      if (!byNormalized.has(key)) {
        byNormalized.set(key, label);
      }
    }
    return Array.from(byNormalized.values()).sort((a, b) => a.localeCompare(b));
  }

  private loadAllRecords(): void {
    this.reportService.getMemberRecordsByRange(this.organizationId, this.userId, '2020-01-01', '2099-12-31', this.selectedLocation()).subscribe({
      next: (records) => {
        this.allRecords.set(records);
        if (!this.selectedLocation()) {
          this.availableLocations.set(this.normalizeLocationOptions(records));
        }
      },
    });
  }

  private loadDayRecords(date: string): void {
    const selectedMonth = date.slice(0, 7);
    if (this.monthRecords().some((record) => record.entryDate.startsWith(selectedMonth))) {
      this.setSelectedDayFromMonth(this.monthRecords(), date);
      return;
    }

    this.isLoadingRecords.set(true);
    this.reportService.getMemberRecordsByDate(this.organizationId, this.userId, date).subscribe({
      next: (records) => {
        this.selectedDayRecords.set(records);
        this.isLoadingRecords.set(false);
      },
      error: () => this.isLoadingRecords.set(false),
    });
  }

  private setSelectedDayFromMonth(records: MemberActivityRecord[], date: string): void {
    this.selectedDayRecords.set(records.filter((record) => record.entryDate === date));
    this.isLoadingRecords.set(false);
  }
}
