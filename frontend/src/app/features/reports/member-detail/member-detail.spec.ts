import { TestBed } from '@angular/core/testing';
import { ComponentFixture } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { provideRouter } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { Location } from '@angular/common';
import { of } from 'rxjs';
import { MemberDetail } from './member-detail';
import { OrganizationService } from '@services/organization.service';
import { ReportService } from '@services/report.service';
import { TeamService } from '@services/team.service';
import { MemberActivityRecord, CurriculumOverview, MemberAbsence } from '@app/core/models/report.models';
import { Team } from '@app/core/models/team.models';

function makeRecord(overrides: Partial<MemberActivityRecord> = {}): MemberActivityRecord {
  return {
    id: crypto.randomUUID(),
    entryDate: '2024-01-15',
    curriculumActivityId: null,
    activityLabel: '',
    hours: 1,
    notes: '',
    rating: null,
    teamId: null,
    location: null,
    ...overrides,
  };
}

function makeAbsence(overrides: Partial<MemberAbsence> = {}): MemberAbsence {
  return {
    id: crypto.randomUUID(),
    absenceTypeId: 'vacation',
    startDate: '2099-01-10',
    endDate: '2099-01-10',
    rrule: null,
    isRecurring: false,
    dayFraction: 1,
    notes: null,
    currentSemester: null,
    ...overrides,
  };
}

describe('MemberDetail computed signals', () => {
  let fixture: ComponentFixture<MemberDetail>;
  let component: MemberDetail;
  let reportServiceSpy: jasmine.SpyObj<ReportService>;

  beforeEach(async () => {
    const orgServiceSpy = jasmine.createSpyObj('OrganizationService', ['getOrganizationMembers', 'getProfessions', 'getOnlyOrganizationMembers']);
    orgServiceSpy.getOrganizationMembers.and.returnValue(of([]));
    orgServiceSpy.getProfessions.and.returnValue(of([]));
    orgServiceSpy.getOnlyOrganizationMembers.and.returnValue(of([]));

    reportServiceSpy = jasmine.createSpyObj('ReportService', [
      'getMemberRecordsByDate',
      'getMemberRecordsByRange',
      'getCurriculum',
      'getLocationSummary',
      'getMemberAbsences',
    ]);
    reportServiceSpy.getMemberRecordsByDate.and.returnValue(of([]));
    reportServiceSpy.getMemberRecordsByRange.and.returnValue(of([]));
    reportServiceSpy.getCurriculum.and.returnValue(of({ nodes: [], competencies: [] }));
    reportServiceSpy.getLocationSummary.and.returnValue(of([]));
    reportServiceSpy.getMemberAbsences.and.returnValue(of([]));

    const teamServiceSpy = jasmine.createSpyObj('TeamService', ['getTeams']);
    teamServiceSpy.getTeams.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [MemberDetail],
      providers: [
        provideHttpClientTesting(),
        provideTranslateService(),
        provideRouter([]),
        { provide: OrganizationService, useValue: orgServiceSpy },
        { provide: ReportService, useValue: reportServiceSpy },
        { provide: TeamService, useValue: teamServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { params: { userId: 'user-1' }, queryParams: { organizationId: 'org-1' } },
          },
        },
        { provide: Location, useValue: { back: jasmine.createSpy() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MemberDetail);
    component = fixture.componentInstance;
  });

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const c = () => component as any;

  describe('statusMap', () => {
    beforeEach(() => {
      c().targetHours.set(0);
    });

    it('marks a date as reported when all ratings are above 2', () => {
      c().monthRecords.set([
        makeRecord({ entryDate: '2024-01-10', rating: 3 }),
        makeRecord({ entryDate: '2024-01-10', rating: 5 }),
      ]);
      expect(c().statusMap()['2024-01-10']).toBe('reported');
    });

    it('marks a date as bad_rating when any rating is 2 or below', () => {
      c().monthRecords.set([
        makeRecord({ entryDate: '2024-01-10', rating: 5 }),
        makeRecord({ entryDate: '2024-01-10', rating: 2 }),
      ]);
      expect(c().statusMap()['2024-01-10']).toBe('bad_rating');
    });

    it('marks a date as bad_rating when rating is 1', () => {
      c().monthRecords.set([makeRecord({ entryDate: '2024-01-10', rating: 1 })]);
      expect(c().statusMap()['2024-01-10']).toBe('bad_rating');
    });

    it('ignores null ratings when determining status', () => {
      c().monthRecords.set([makeRecord({ entryDate: '2024-01-10', rating: null })]);
      expect(c().statusMap()['2024-01-10']).toBe('reported');
    });

    it('produces one entry per distinct date', () => {
      c().currentYear.set(2099);
      c().currentMonth.set(0);
      c().monthRecords.set([
        makeRecord({ entryDate: '2099-01-10', rating: 3 }),
        makeRecord({ entryDate: '2099-01-11', rating: 4 }),
      ]);
      expect(Object.keys(c().statusMap())).toHaveSize(2);
    });

    it('marks an absence day as absence', () => {
      c().currentYear.set(2099);
      c().currentMonth.set(0);
      c().absences.set([makeAbsence({ startDate: '2099-01-12', endDate: '2099-01-12' })]);
      expect(c().statusMap()['2099-01-12']).toBe('absence');
    });

    it('lets an activity record take precedence over an absence on the same day', () => {
      c().currentYear.set(2099);
      c().currentMonth.set(0);
      c().monthRecords.set([makeRecord({ entryDate: '2099-01-12', rating: 4 })]);
      c().absences.set([makeAbsence({ startDate: '2099-01-12', endDate: '2099-01-12' })]);
      expect(c().statusMap()['2099-01-12']).toBe('reported');
    });

    it('marks each weekday within an absence range but skips weekends', () => {
      c().currentYear.set(2099);
      c().currentMonth.set(0);
      // 2099-01-10 = Sa, 11 = So, 12 = Mo.
      c().absences.set([makeAbsence({ startDate: '2099-01-10', endDate: '2099-01-12' })]);
      const map = c().statusMap();
      expect(map['2099-01-10']).toBeUndefined();
      expect(map['2099-01-11']).toBeUndefined();
      expect(map['2099-01-12']).toBe('absence');
    });

    it('only marks matching weekdays for a recurring absence with BYDAY', () => {
      c().currentYear.set(2099);
      c().currentMonth.set(0);
      // 2099-01-12 is a Monday; UNTIL covers the whole week.
      c().absences.set([makeAbsence({
        startDate: '2099-01-12',
        endDate: '2099-01-12',
        rrule: 'DTSTART:20990112T000000Z;FREQ=WEEKLY;BYDAY=MO;UNTIL=20990118T235959Z',
        isRecurring: true,
      })]);
      const map = c().statusMap();
      expect(map['2099-01-12']).toBe('absence');
      expect(map['2099-01-13']).toBeUndefined();
    });

    it('marks later occurrences of a recurring absence beyond its stored end date', () => {
      c().currentYear.set(2099);
      c().currentMonth.set(0);
      c().absences.set([makeAbsence({
        startDate: '2099-01-12',
        endDate: '2099-01-12',
        rrule: 'DTSTART:20990112T000000Z;FREQ=WEEKLY;BYDAY=MO;UNTIL=20990126T235959Z',
        isRecurring: true,
      })]);
      const map = c().statusMap();
      expect(map['2099-01-12']).toBe('absence');
      expect(map['2099-01-19']).toBe('absence');
      expect(map['2099-01-26']).toBe('absence');
    });

    it('marks a date as under_target when total hours are below the target', () => {
      c().targetHours.set(8);
      c().monthRecords.set([makeRecord({ entryDate: '2024-01-10', hours: 5, rating: 3 })]);
      expect(c().statusMap()['2024-01-10']).toBe('under_target');
    });

    it('marks a date as bad_rating_under_target when it is both under target and has a bad rating', () => {
      c().targetHours.set(8);
      c().monthRecords.set([makeRecord({ entryDate: '2024-01-10', hours: 5, rating: 2 })]);
      expect(c().statusMap()['2024-01-10']).toBe('bad_rating_under_target');
    });
  });

  describe('selectedDayAbsences', () => {
    it('returns absences covering the selected date', () => {
      c().selectedDate.set('2099-01-12');
      c().absences.set([
        makeAbsence({ startDate: '2099-01-12', endDate: '2099-01-12', absenceTypeId: 'sick' }),
        makeAbsence({ startDate: '2099-02-02', endDate: '2099-02-02', absenceTypeId: 'vacation' }),
      ]);
      const result = c().selectedDayAbsences();
      expect(result).toHaveSize(1);
      expect(result[0].typeLabel).toContain('sick');
    });

    it('returns an empty array when no absence covers the selected date', () => {
      c().selectedDate.set('2099-03-15');
      c().absences.set([makeAbsence({ startDate: '2099-01-12', endDate: '2099-01-12' })]);
      expect(c().selectedDayAbsences()).toHaveSize(0);
    });
  });

  describe('absence day totals', () => {
    it('derives available semesters from the counted dates, not the stored column', () => {
      c().absences.set([makeAbsence({ startDate: '2024-07-31', endDate: '2024-08-02', currentSemester: null })]);
      expect(c().availableSemesters()).toEqual(['2023/S2', '2024/S1']);
    });

    it('splits a semester-boundary range per date across semesters', () => {
      // 29.–31.7.2024 = Mo–Mi → 2023/S2; 1./2.8.2024 = Do/Fr → 2024/S1.
      c().absences.set([makeAbsence({ startDate: '2024-07-29', endDate: '2024-08-02' })]);
      const vacationDays = () =>
        c().absenceTotals().find((e: { meta: { id: string } }) => e.meta.id === 'vacation')?.days;
      expect(vacationDays()).toBe(5);
      c().selectedAbsenceSemester.set('2023/S2');
      expect(vacationDays()).toBe(3);
      c().selectedAbsenceSemester.set('2024/S1');
      expect(vacationDays()).toBe(2);
    });

    it('rounds half days only once on the final total', () => {
      // Mo–Fr 15.–19.7.2024, je 0.5 Tage → 2.5 → gerundet 3.
      c().absences.set([makeAbsence({ startDate: '2024-07-15', endDate: '2024-07-19', dayFraction: 0.5 })]);
      const total = c().absenceTotals().find((e: { meta: { id: string } }) => e.meta.id === 'vacation');
      expect(total?.days).toBe(3);
    });

    it('counts every occurrence of a recurring rule instead of the stored single-day range', () => {
      c().absences.set([makeAbsence({
        startDate: '2024-08-05',
        endDate: '2024-08-05',
        rrule: 'DTSTART:20240805T000000Z;FREQ=WEEKLY;BYDAY=MO;UNTIL=20241231T235959Z',
        isRecurring: true,
      })]);
      const total = c().absenceTotals().find((e: { meta: { id: string } }) => e.meta.id === 'vacation');
      expect(total?.days).toBe(22);
    });

    it('keeps weekend-only absences visible with zero counted days', () => {
      c().absences.set([makeAbsence({ startDate: '2024-07-20', endDate: '2024-07-21' })]);
      expect(c().absenceEntries().length).toBe(1);
      expect(c().absenceEntries()[0].weekendOnly).toBeTrue();
      expect(c().absenceEntries()[0].workingDays).toBe(0);
      expect(c().absenceTotals()).toEqual([]);
    });
  });

  describe('teamGroups', () => {
    const team1: Team = { id: 'team-1', professionId: 'prof-1', name: 'Team A' };
    const team2: Team = { id: 'team-2', professionId: 'prof-2', name: 'Team B' };

    const curriculum1: CurriculumOverview = {
      nodes: [
        { id: 'act-1', parentId: null, nodeType: 'activity', key: 'a1', label: 'Act 1', sortOrder: 0, competencyIds: ['comp-A', 'comp-B'] },
        { id: 'act-2', parentId: null, nodeType: 'activity', key: 'a2', label: 'Act 2', sortOrder: 1, competencyIds: ['comp-A'] },
        { id: 'cat-1', parentId: null, nodeType: 'category', key: 'c1', label: 'Cat', sortOrder: 0, competencyIds: [] },
      ],
      competencies: [
        { id: 'comp-A', code: 'A', description: 'Competency A' },
        { id: 'comp-B', code: 'B', description: 'Competency B' },
      ],
    };

    const curriculum2: CurriculumOverview = {
      nodes: [
        { id: 'act-3', parentId: null, nodeType: 'activity', key: 'a3', label: 'Act 3', sortOrder: 0, competencyIds: ['comp-C'] },
      ],
      competencies: [{ id: 'comp-C', code: 'C', description: 'Competency C' }],
    };

    it('creates one group per team with records', () => {
      c().teams.set([team1, team2]);
      c().curriculaByProfession.set(new Map([['prof-1', curriculum1], ['prof-2', curriculum2]]));
      c().allRecords.set([
        makeRecord({ teamId: 'team-1', curriculumActivityId: 'act-1', hours: 4 }),
        makeRecord({ teamId: 'team-2', curriculumActivityId: 'act-3', hours: 2 }),
      ]);
      const groups = c().teamGroups();
      expect(groups).toHaveSize(2);
      expect(groups.map((g: { teamName: string }) => g.teamName).sort()).toEqual(['Team A', 'Team B']);
    });

    it('aggregates activity hours per team, sorted descending', () => {
      c().teams.set([team1]);
      c().curriculaByProfession.set(new Map([['prof-1', curriculum1]]));
      c().allRecords.set([
        makeRecord({ teamId: 'team-1', curriculumActivityId: 'act-1', hours: 3 }),
        makeRecord({ teamId: 'team-1', curriculumActivityId: 'act-1', hours: 2 }),
        makeRecord({ teamId: 'team-1', curriculumActivityId: 'act-2', hours: 6 }),
      ]);
      const progress = c().teamGroups()[0].activityProgress;
      expect(progress).toHaveSize(2);
      expect(progress[0].id).toBe('act-2');
      expect(progress[0].hours).toBe(6);
      expect(progress[1].id).toBe('act-1');
      expect(progress[1].hours).toBe(5);
    });

    it('sums competency hours across activities within a team', () => {
      c().teams.set([team1]);
      c().curriculaByProfession.set(new Map([['prof-1', curriculum1]]));
      c().allRecords.set([
        makeRecord({ teamId: 'team-1', curriculumActivityId: 'act-1', hours: 4 }),
        makeRecord({ teamId: 'team-1', curriculumActivityId: 'act-2', hours: 2 }),
      ]);
      const map: Map<string, number> = c().teamGroups()[0].competencyHours;
      expect(map.get('comp-A')).toBe(6);
      expect(map.get('comp-B')).toBe(4);
    });

    it('skips records without activity id', () => {
      c().teams.set([team1]);
      c().curriculaByProfession.set(new Map([['prof-1', curriculum1]]));
      c().allRecords.set([
        makeRecord({ teamId: 'team-1', curriculumActivityId: null, hours: 8 }),
        makeRecord({ teamId: 'team-1', curriculumActivityId: 'act-1', hours: 2 }),
      ]);
      expect(c().teamGroups()[0].activityProgress).toHaveSize(1);
    });

    it('ignores category nodes in competency mapping', () => {
      c().teams.set([team1]);
      c().curriculaByProfession.set(new Map([['prof-1', curriculum1]]));
      c().allRecords.set([makeRecord({ teamId: 'team-1', curriculumActivityId: 'cat-1', hours: 10 })]);
      const group = c().teamGroups()[0];
      expect(group.competencyHours.size).toBe(0);
    });

    it('returns empty array when no records exist', () => {
      c().allRecords.set([]);
      expect(c().teamGroups()).toHaveSize(0);
    });

    it('lists curriculum activities that were never reported as not performed', () => {
      c().teams.set([team1]);
      c().curriculaByProfession.set(new Map([['prof-1', curriculum1]]));
      c().allRecords.set([makeRecord({ teamId: 'team-1', curriculumActivityId: 'act-1', hours: 4 })]);

      const group = c().teamGroups()[0];
      expect(group.hasCurriculumActivities).toBeTrue();
      expect(group.notPerformedActivities.map((a: { id: string }) => a.id)).toEqual(['act-2']);
      expect(group.notPerformedActivities[0].hours).toBe(0);
    });

    it('sorts under-threshold activities ascending by hours, not performed first', () => {
      c().teams.set([team1]);
      c().curriculaByProfession.set(new Map([['prof-1', curriculum1]]));
      c().allRecords.set([makeRecord({ teamId: 'team-1', curriculumActivityId: 'act-1', hours: 4 })]);

      const group = c().teamGroups()[0];
      expect(group.underThresholdActivities.map((a: { id: string }) => a.id)).toEqual(['act-2', 'act-1']);
    });

    it('excludes activities that reached the threshold', () => {
      c().teams.set([team1]);
      c().curriculaByProfession.set(new Map([['prof-1', curriculum1]]));
      c().allRecords.set([
        makeRecord({ teamId: 'team-1', curriculumActivityId: 'act-1', hours: 10 }),
        makeRecord({ teamId: 'team-1', curriculumActivityId: 'act-2', hours: 12 }),
      ]);

      const group = c().teamGroups()[0];
      expect(group.underThresholdActivities).toHaveSize(0);
      expect(group.notPerformedActivities).toHaveSize(0);
    });

    const curriculumWithDuplicateLabels: CurriculumOverview = {
      nodes: [
        { id: 'act-1', parentId: null, nodeType: 'activity', key: 'a1', label: 'Planstudium', sortOrder: 0, competencyIds: [] },
        { id: 'act-2', parentId: null, nodeType: 'activity', key: 'a2', label: 'Planstudium', sortOrder: 1, competencyIds: [] },
        { id: 'act-3', parentId: null, nodeType: 'activity', key: 'a3', label: ' planstudium ', sortOrder: 2, competencyIds: [] },
        { id: 'act-4', parentId: null, nodeType: 'activity', key: 'a4', label: 'Montage', sortOrder: 3, competencyIds: [] },
      ],
      competencies: [],
    };

    it('merges open activities that share a label into a single entry', () => {
      c().teams.set([team1]);
      c().curriculaByProfession.set(new Map([['prof-1', curriculumWithDuplicateLabels]]));
      c().allRecords.set([makeRecord({ teamId: 'team-1', curriculumActivityId: 'act-1', hours: 1 })]);

      const group = c().teamGroups()[0];
      expect(group.underThresholdActivities.map((a: { label: string }) => a.label))
        .toEqual(['Montage', 'Planstudium']);
    });

    it('sums the hours of all activities sharing a label', () => {
      c().teams.set([team1]);
      c().curriculaByProfession.set(new Map([['prof-1', curriculumWithDuplicateLabels]]));
      c().allRecords.set([
        makeRecord({ teamId: 'team-1', curriculumActivityId: 'act-1', hours: 2 }),
        makeRecord({ teamId: 'team-1', curriculumActivityId: 'act-3', hours: 1.5 }),
      ]);

      const merged = c().teamGroups()[0].underThresholdActivities
        .find((a: { label: string }) => a.label === 'Planstudium');
      expect(merged.hours).toBe(3.5);
    });

    it('drops a merged label from the open activities once the summed hours reach the threshold', () => {
      c().teams.set([team1]);
      c().curriculaByProfession.set(new Map([['prof-1', curriculumWithDuplicateLabels]]));
      c().allRecords.set([
        makeRecord({ teamId: 'team-1', curriculumActivityId: 'act-1', hours: 4 }),
        makeRecord({ teamId: 'team-1', curriculumActivityId: 'act-2', hours: 4 }),
        makeRecord({ teamId: 'team-1', curriculumActivityId: 'act-3', hours: 2 }),
      ]);

      const group = c().teamGroups()[0];
      expect(group.underThresholdActivities.map((a: { label: string }) => a.label)).toEqual(['Montage']);
    });

    it('counts a merged label as performed when any of its activities has hours', () => {
      c().teams.set([team1]);
      c().curriculaByProfession.set(new Map([['prof-1', curriculumWithDuplicateLabels]]));
      c().allRecords.set([makeRecord({ teamId: 'team-1', curriculumActivityId: 'act-2', hours: 1 })]);

      const group = c().teamGroups()[0];
      expect(group.notPerformedActivities.map((a: { label: string }) => a.label)).toEqual(['Montage']);
    });

    it('keeps a team group whose records carry no curriculum activity id', () => {
      c().teams.set([team1]);
      c().curriculaByProfession.set(new Map([['prof-1', curriculum1]]));
      c().allRecords.set([makeRecord({ teamId: 'team-1', curriculumActivityId: null, hours: 8 })]);

      const group = c().teamGroups()[0];
      expect(group.activityProgress).toHaveSize(0);
      expect(group.notPerformedActivities).toHaveSize(2);
    });

    it('labels reported activities from the curriculum, not from the record text', () => {
      c().teams.set([team1]);
      c().curriculaByProfession.set(new Map([['prof-1', curriculum1]]));
      c().allRecords.set([
        makeRecord({ teamId: 'team-1', curriculumActivityId: 'act-1', activityLabel: 'Freitext', hours: 4 }),
      ]);

      expect(c().teamGroups()[0].activityProgress[0].label).toBe('Act 1');
    });

    it('falls back to the record label for activities outside the curriculum', () => {
      c().teams.set([team1]);
      c().curriculaByProfession.set(new Map([['prof-1', curriculum1]]));
      c().allRecords.set([
        makeRecord({ teamId: 'team-1', curriculumActivityId: 'act-unknown', activityLabel: 'Freitext', hours: 4 }),
      ]);

      const progress = c().teamGroups()[0].activityProgress;
      expect(progress[0].label).toBe('Freitext');
    });

    it('skips a team group with neither reported nor curriculum activities', () => {
      c().teams.set([team1]);
      c().curriculaByProfession.set(new Map([['prof-1', { nodes: [], competencies: [] }]]));
      c().allRecords.set([makeRecord({ teamId: 'team-1', curriculumActivityId: null, hours: 8 })]);

      expect(c().teamGroups()).toHaveSize(0);
    });

    it('reports no curriculum activities when the profession has none', () => {
      c().teams.set([team1]);
      c().curriculaByProfession.set(new Map([['prof-1', { nodes: [], competencies: [] }]]));
      c().allRecords.set([makeRecord({ teamId: 'team-1', curriculumActivityId: 'act-1', hours: 4 })]);

      const group = c().teamGroups()[0];
      expect(group.hasCurriculumActivities).toBeFalse();
      expect(group.underThresholdActivities).toHaveSize(0);
    });

    it('uses fallback profession curriculum for records without team id', () => {
      c().teams.set([]);
      c().fallbackProfessionId.set('prof-1');
      c().curriculaByProfession.set(new Map([['prof-1', curriculum1]]));
      c().allRecords.set([makeRecord({ teamId: null, curriculumActivityId: 'act-1', hours: 4 })]);
      const groups = c().teamGroups();
      expect(groups).toHaveSize(1);
      expect(groups[0].teamName).toBe('Ohne Team');
      expect(groups[0].competencyHours.get('comp-A')).toBe(4);
    });
  });

  describe('location filter', () => {
    it('loads all progress records when no location is selected', () => {
      c().organizationId = 'org-1';
      c().userId = 'user-1';

      c().loadAllRecords();

      expect(reportServiceSpy.getMemberRecordsByRange).toHaveBeenCalledWith(
        'org-1',
        'user-1',
        '2020-01-01',
        '2099-12-31',
        ''
      );
    });

    it('reloads progress records with the selected location', () => {
      c().organizationId = 'org-1';
      c().userId = 'user-1';

      c().onLocationSelected('Ward A');

      expect(c().selectedLocation()).toBe('Ward A');
      expect(reportServiceSpy.getMemberRecordsByRange).toHaveBeenCalledWith(
        'org-1',
        'user-1',
        '2020-01-01',
        '2099-12-31',
        'Ward A'
      );
    });

    it('merges location options by trimmed case-insensitive value', () => {
      const options = c().normalizeLocationOptions([
        { location: ' Ward A ', totalHours: 2 },
        { location: 'ward a', totalHours: 3 },
        { location: 'Station B', totalHours: 1 },
        { location: '', totalHours: 4 },
      ]);

      expect(options).toEqual(['Station B', 'Ward A']);
    });

    it('reports no location options when none are available', () => {
      c().availableLocations.set([]);

      expect(c().hasLocationOptions()).toBeFalse();
    });
  });
});
