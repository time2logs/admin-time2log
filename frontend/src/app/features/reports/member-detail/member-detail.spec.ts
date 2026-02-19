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
import { MemberActivityRecord, CurriculumOverview } from '@app/core/models/report.models';

function makeRecord(overrides: Partial<MemberActivityRecord> = {}): MemberActivityRecord {
  return {
    id: crypto.randomUUID(),
    entryDate: '2024-01-15',
    curriculumActivityId: null,
    activityLabel: '',
    hours: 1,
    notes: '',
    rating: null,
    ...overrides,
  };
}

describe('MemberDetail computed signals', () => {
  let fixture: ComponentFixture<MemberDetail>;
  let component: MemberDetail;

  beforeEach(async () => {
    const orgServiceSpy = jasmine.createSpyObj('OrganizationService', ['getOrganizationMembers', 'getProfessions']);
    orgServiceSpy.getOrganizationMembers.and.returnValue(of([]));
    orgServiceSpy.getProfessions.and.returnValue(of([]));

    const reportServiceSpy = jasmine.createSpyObj('ReportService', [
      'getMemberRecordsByDate',
      'getMemberRecordsByRange',
      'getCurriculum',
    ]);
    reportServiceSpy.getMemberRecordsByDate.and.returnValue(of([]));
    reportServiceSpy.getMemberRecordsByRange.and.returnValue(of([]));
    reportServiceSpy.getCurriculum.and.returnValue(of(null));

    await TestBed.configureTestingModule({
      imports: [MemberDetail],
      providers: [
        // Intercept all HTTP requests so AuthService cannot reach Supabase
        // even if a real session is present in the test browser's localStorage.
        provideHttpClientTesting(),
        provideTranslateService(),
        provideRouter([]),
        { provide: OrganizationService, useValue: orgServiceSpy },
        { provide: ReportService, useValue: reportServiceSpy },
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

    // Do NOT call detectChanges() here. We only test the computed signal logic,
    // not the data-loading side effects of ngOnInit. Skipping detectChanges()
    // prevents ngOnInit from running, which means no real or mocked HTTP calls
    // are triggered and the signals stay in their clean initial state ([]/null).
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
      // Use a future month so the "missing weekday" loop adds nothing.
      c().currentYear.set(2099);
      c().currentMonth.set(0);
      c().monthRecords.set([
        makeRecord({ entryDate: '2099-01-10', rating: 3 }),
        makeRecord({ entryDate: '2099-01-11', rating: 4 }),
      ]);
      expect(Object.keys(c().statusMap())).toHaveSize(2);
    });
  });

  describe('activityProgress', () => {
    it('aggregates hours per activity, sorted descending', () => {
      const actId1 = 'act-1';
      const actId2 = 'act-2';
      c().allRecords.set([
        makeRecord({ curriculumActivityId: actId1, activityLabel: 'First Aid', hours: 3 }),
        makeRecord({ curriculumActivityId: actId1, hours: 2 }),
        makeRecord({ curriculumActivityId: actId2, activityLabel: 'CPR', hours: 6 }),
      ]);
      const progress = c().activityProgress();
      expect(progress).toHaveSize(2);
      expect(progress[0].id).toBe(actId2);
      expect(progress[0].hours).toBe(6);
      expect(progress[1].id).toBe(actId1);
      expect(progress[1].hours).toBe(5);
    });

    it('skips records without an activity id', () => {
      c().allRecords.set([
        makeRecord({ curriculumActivityId: null, hours: 8 }),
        makeRecord({ curriculumActivityId: 'act-1', hours: 2 }),
      ]);
      expect(c().activityProgress()).toHaveSize(1);
    });

    it('returns empty array when no records exist', () => {
      c().allRecords.set([]);
      expect(c().activityProgress()).toHaveSize(0);
    });
  });

  describe('competencyHours', () => {
    const curriculum: CurriculumOverview = {
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

    beforeEach(() => {
      c().curriculum.set(curriculum);
      c().allRecords.set([
        makeRecord({ curriculumActivityId: 'act-1', hours: 4 }),
        makeRecord({ curriculumActivityId: 'act-2', hours: 2 }),
      ]);
    });

    it('sums hours from all activities contributing to each competency', () => {
      const map: Map<string, number> = c().competencyHours();
      // comp-A gets hours from act-1 (4h) + act-2 (2h) = 6h
      expect(map.get('comp-A')).toBe(6);
      // comp-B only from act-1 = 4h
      expect(map.get('comp-B')).toBe(4);
    });

    it('ignores category nodes', () => {
      c().allRecords.set([makeRecord({ curriculumActivityId: 'cat-1', hours: 10 })]);
      const map: Map<string, number> = c().competencyHours();
      expect(map.size).toBe(0);
    });

    it('returns empty map when curriculum is null', () => {
      c().curriculum.set(null);
      expect(c().competencyHours().size).toBe(0);
    });
  });
});
