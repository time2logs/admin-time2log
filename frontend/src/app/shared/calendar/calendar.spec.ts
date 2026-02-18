import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideTranslateService } from '@ngx-translate/core';
import { Calendar } from './calendar';

interface CalendarDay {
  date: string;
  day: number;
  isCurrentMonth: boolean;
  isToday: boolean;
  isWeekend: boolean;
  isFuture: boolean;
}

describe('Calendar', () => {
  let fixture: ComponentFixture<Calendar>;
  let component: Calendar;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Calendar],
      providers: [provideHttpClient(), provideTranslateService()],
    }).compileComponents();

    fixture = TestBed.createComponent(Calendar);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('statusMap', {});
    fixture.componentRef.setInput('selectedDate', '');
    fixture.detectChanges();
  });

  function setMonth(year: number, month: number): CalendarDay[] {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (component as any).currentYear.set(year);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (component as any).currentMonth.set(month);
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return (component as any).days();
  }

  it('should always produce exactly 42 days', () => {
    // Test several months to ensure the grid is always 42
    const months = [
      { year: 2024, month: 0 },  // Jan: starts Monday
      { year: 2024, month: 1 },  // Feb: leap year
      { year: 2024, month: 8 },  // Sep: starts Sunday
      { year: 2023, month: 1 },  // Feb: 28 days
    ];
    for (const { year, month } of months) {
      const days = setMonth(year, month);
      expect(days.length).withContext(`${year}-${month + 1}`).toBe(42);
    }
  });

  it('should mark only days within the selected month as isCurrentMonth', () => {
    // January 2024 has 31 days
    const days = setMonth(2024, 0);
    const currentMonthDays = days.filter(d => d.isCurrentMonth);
    expect(currentMonthDays.length).toBe(31);
  });

  it('should have no offset when month starts on Monday', () => {
    // January 2024 starts on a Monday — no previous-month padding expected
    const days = setMonth(2024, 0);
    expect(days[0].isCurrentMonth).toBeTrue();
    expect(days[0].day).toBe(1);
  });

  it('should pad with previous month when month starts on Tuesday or later', () => {
    // February 2024 starts on Thursday — 3 days of January should precede it
    const days = setMonth(2024, 1);
    const prevMonthDays = days.slice(0, days.findIndex(d => d.isCurrentMonth));
    expect(prevMonthDays.length).toBe(3); // Mon, Tue, Wed from Jan
    expect(prevMonthDays.every(d => !d.isCurrentMonth)).toBeTrue();
  });

  it('should use offset of 6 when month starts on Sunday', () => {
    // September 2024 starts on Sunday — 6 days of August should precede it
    const days = setMonth(2024, 8);
    const prevMonthDays = days.slice(0, days.findIndex(d => d.isCurrentMonth));
    expect(prevMonthDays.length).toBe(6);
  });

  it('should mark Saturday and Sunday as weekend', () => {
    // January 2024: day 6 = Saturday, day 7 = Sunday, day 8 = Monday
    // Use `.day` (local day number) instead of `.date` (UTC ISO string) to avoid timezone issues
    const days = setMonth(2024, 0);
    const currentMonthDays = days.filter(d => d.isCurrentMonth);
    const sat = currentMonthDays.find(d => d.day === 6); // Jan 6 = Saturday
    const sun = currentMonthDays.find(d => d.day === 7); // Jan 7 = Sunday
    const mon = currentMonthDays.find(d => d.day === 8); // Jan 8 = Monday

    expect(sat?.isWeekend).toBeTrue();
    expect(sun?.isWeekend).toBeTrue();
    expect(mon?.isWeekend).toBeFalse();
  });

  it('should fill the rest of the grid with next month days', () => {
    const days = setMonth(2024, 0); // January 2024
    const nextMonthDays = days.filter(d => !d.isCurrentMonth && d.date > '2024-01-31');
    expect(nextMonthDays.length).toBeGreaterThan(0);
    expect(nextMonthDays.every(d => d.date.startsWith('2024-02'))).toBeTrue();
  });
});
