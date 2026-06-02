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
import { MemberActivityRecord, CurriculumOverview } from '@app/core/models/report.models';
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
    ...overrides,
  };
}

describe('MemberDetail computed signals', () => {
  let fixture: ComponentFixture<MemberDetail>;
  let component: MemberDetail;

  beforeEach(async () => {
    const orgServiceSpy = jasmine.createSpyObj('OrganizationService', ['getOrganizationMembers', 'getProfessions', 'getOnlyOrganizationMembers']);
    orgServiceSpy.getOrganizationMembers.and.returnValue(of([]));
    orgServiceSpy.getProfessions.and.returnValue(of([]));
    orgServiceSpy.getOnlyOrganizationMembers.and.returnValue(of([]));

    const reportServiceSpy = jasmine.createSpyObj('ReportService', [
      'getMemberRecordsByDate',
      'getMemberRecordsByRange',
      'getCurriculum',
    ]);
    reportServiceSpy.getMemberRecordsByDate.and.returnValue(of([]));
    reportServiceSpy.getMemberRecordsByRange.and.returnValue(of([]));
    reportServiceSpy.getCurriculum.and.returnValue(of(null));

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
});
