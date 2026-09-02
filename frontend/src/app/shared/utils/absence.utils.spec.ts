import { AbsenceLike, expandAbsence, expandAbsenceDates, RECURRING_CAP_DAYS, semesterKeyOf } from './absence.utils';

function makeAbsence(overrides: Partial<AbsenceLike> = {}): AbsenceLike {
  return {
    startDate: '2024-07-15',
    endDate: '2024-07-15',
    rrule: null,
    isRecurring: false,
    dayFraction: 1,
    ...overrides,
  };
}

// Fixiertes "heute", damit Cap-Berechnungen deterministisch sind.
const TODAY = new Date(2024, 5, 14);

describe('semesterKeyOf', () => {
  it('attributes August through December to YYYY/S1', () => {
    expect(semesterKeyOf('2024-08-01')).toBe('2024/S1');
    expect(semesterKeyOf('2024-12-31')).toBe('2024/S1');
  });

  it('attributes January through July to YYYY-1/S2', () => {
    expect(semesterKeyOf('2024-01-31')).toBe('2023/S2');
    expect(semesterKeyOf('2024-07-31')).toBe('2023/S2');
  });
});

describe('expandAbsenceDates', () => {
  it('counts every calendar day of a non-recurring range except weekends', () => {
    // 2024-07-19 = Fr, 20/21 = Sa/So, 22 = Mo.
    const dates = expandAbsenceDates(makeAbsence({ startDate: '2024-07-19', endDate: '2024-07-22' }));
    expect(dates).toEqual(['2024-07-19', '2024-07-22']);
  });

  it('returns an empty set for a range that lies entirely on a weekend', () => {
    const absence = makeAbsence({ startDate: '2024-07-20', endDate: '2024-07-21' });
    expect(expandAbsenceDates(absence)).toEqual([]);
    expect(expandAbsence(absence).weekendOnly).toBeTrue();
    expect(expandAbsence(absence).invalid).toBeFalse();
  });

  it('splits a range across the semester boundary per date', () => {
    // 29.–31.7.2024 = Mo–Mi → 2023/S2; 1./2.8.2024 = Do/Fr → 2024/S1.
    const exp = expandAbsence(makeAbsence({ startDate: '2024-07-29', endDate: '2024-08-02' }));
    expect(exp.daysBySemester.get('2023/S2')).toBe(3);
    expect(exp.daysBySemester.get('2024/S1')).toBe(2);
    expect(exp.totalDays).toBe(5);
  });

  it('expands a weekly rule to every occurrence up to UNTIL, not a single day', () => {
    const absence = makeAbsence({
      startDate: '2024-08-05',
      endDate: '2024-08-05',
      rrule: 'DTSTART:20240805T000000Z;FREQ=WEEKLY;BYDAY=MO;UNTIL=20241231T235959Z',
      isRecurring: true,
    });
    const exp = expandAbsence(absence);
    expect(exp.dates.length).toBe(22);
    expect(exp.dates[0]).toBe('2024-08-05');
    expect(exp.dates[exp.dates.length - 1]).toBe('2024-12-30');
    expect(exp.totalDays).toBe(22);
    expect(exp.recurring?.isOngoing).toBeFalse();
    expect(exp.recurring?.untilDate).toBe('2024-12-31');
  });

  it('caps a recurring rule without UNTIL at today + 365 days', () => {
    const absence = makeAbsence({
      startDate: '2024-06-03',
      endDate: '2024-06-03',
      rrule: 'DTSTART:20240603T000000Z;FREQ=WEEKLY;BYDAY=MO',
      isRecurring: true,
    });
    const exp = expandAbsence(absence, TODAY);
    expect(exp.recurring?.isOngoing).toBeTrue();
    expect(exp.recurring?.capDate).toBe('2025-06-14');
    // Montage 2024-06-03 bis 2025-06-09.
    expect(exp.dates.length).toBe(54);
    expect(exp.totalDays).toBe(54);
  });

  it('drops legacy weekend entries from BYDAY', () => {
    const absence = makeAbsence({
      startDate: '2024-07-15',
      endDate: '2024-07-15',
      rrule: 'DTSTART:20240715T000000Z;FREQ=WEEKLY;BYDAY=MO,SA;UNTIL=20240815T235959Z',
      isRecurring: true,
    });
    const dates = expandAbsenceDates(absence);
    expect(dates).toEqual(['2024-07-15', '2024-07-22', '2024-07-29', '2024-08-05', '2024-08-12']);
  });

  it('falls back to the DTSTART weekday for weekly rules without BYDAY', () => {
    const absence = makeAbsence({
      startDate: '2024-07-16',
      endDate: '2024-07-16',
      rrule: 'DTSTART:20240716T000000Z;FREQ=WEEKLY;UNTIL=20240729T235959Z',
      isRecurring: true,
    });
    expect(expandAbsenceDates(absence)).toEqual(['2024-07-16', '2024-07-23']);
  });

  it('expands daily rules every day but still removes weekends', () => {
    const absence = makeAbsence({
      startDate: '2024-07-15',
      endDate: '2024-07-15',
      rrule: 'DTSTART:20240715T000000Z;FREQ=DAILY;UNTIL=20240721T235959Z',
      isRecurring: true,
    });
    expect(expandAbsenceDates(absence)).toEqual(['2024-07-15', '2024-07-16', '2024-07-17', '2024-07-18', '2024-07-19']);
  });

  it('counts an unparseable rrule as 0 days but flags it invalid instead of weekend-only', () => {
    const absence = makeAbsence({
      startDate: '2024-07-15',
      endDate: '2024-07-15',
      rrule: 'FREQ=MONTHLY',
      isRecurring: true,
    });
    const exp = expandAbsence(absence);
    expect(exp.dates).toEqual([]);
    expect(exp.totalDays).toBe(0);
    expect(exp.invalid).toBeTrue();
    expect(exp.weekendOnly).toBeFalse();
    expect(exp.recurring).toBeNull();
  });

  it('treats a recurring absence without any rrule as invalid', () => {
    const exp = expandAbsence(makeAbsence({ isRecurring: true }));
    expect(exp.invalid).toBeTrue();
    expect(exp.totalDays).toBe(0);
  });

  it('treats a non-recurring absence with swapped dates as invalid', () => {
    const exp = expandAbsence(makeAbsence({ startDate: '2024-07-19', endDate: '2024-07-15' }));
    expect(exp.invalid).toBeTrue();
    expect(exp.totalDays).toBe(0);
  });

  it('applies the day fraction to every counted date', () => {
    // Mo–Mi 2024-07-15..17, je 0.5 Tage.
    const exp = expandAbsence(makeAbsence({ startDate: '2024-07-15', endDate: '2024-07-17', dayFraction: 0.5 }));
    expect(exp.totalDays).toBe(1.5);
    expect(exp.daysBySemester.get('2023/S2')).toBe(1.5);
  });

  it('never expands a recurring rule before its DTSTART', () => {
    const absence = makeAbsence({
      startDate: '2024-07-15',
      endDate: '2024-07-15',
      rrule: 'DTSTART:20240801T000000Z;FREQ=DAILY;UNTIL=20240802T235959Z',
      isRecurring: true,
    });
    expect(expandAbsenceDates(absence)).toEqual(['2024-08-01', '2024-08-02']);
  });
});

describe('RECURRING_CAP_DAYS', () => {
  it('is 365', () => {
    expect(RECURRING_CAP_DAYS).toBe(365);
  });
});
