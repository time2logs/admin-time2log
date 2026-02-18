import { Component, input, output, computed, signal, OnInit, inject } from '@angular/core';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ReportStatus } from '@app/core/models/report.models';

interface CalendarDay {
  date: string; // 'YYYY-MM-DD'
  day: number;
  isCurrentMonth: boolean;
  isToday: boolean;
  isWeekend: boolean;
  isFuture: boolean;
}

@Component({
  selector: 'app-calendar',
  standalone: true,
  imports: [TranslateModule],
  templateUrl: './calendar.html',
})
export class Calendar implements OnInit {
  private readonly translate = inject(TranslateService);

  readonly statusMap = input<Record<string, ReportStatus>>({});
  readonly selectedDate = input<string>('');

  readonly dateSelected = output<string>();
  readonly monthChanged = output<{ year: number; month: number }>();

  protected readonly weekdays = [
    'reports.calendar.mon',
    'reports.calendar.tue',
    'reports.calendar.wed',
    'reports.calendar.thu',
    'reports.calendar.fri',
    'reports.calendar.sat',
    'reports.calendar.sun',
  ];

  protected readonly today = new Date().toISOString().slice(0, 10);
  protected readonly currentYear = signal(new Date().getFullYear());
  protected readonly currentMonth = signal(new Date().getMonth()); // 0-indexed

  protected readonly monthLabel = computed(() => {
    const d = new Date(this.currentYear(), this.currentMonth(), 1);
    const locale = this.translate.currentLang === 'de' ? 'de-CH' : 'en-US';
    return d.toLocaleDateString(locale, { month: 'long', year: 'numeric' });
  });

  protected readonly days = computed<CalendarDay[]>(() => {
    const year = this.currentYear();
    const month = this.currentMonth();
    const today = this.today;
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);

    // Monday-based: 0=Mon, 6=Sun
    let startOffset = firstDay.getDay() - 1;
    if (startOffset < 0) startOffset = 6;

    const days: CalendarDay[] = [];

    // Fill leading days from previous month
    const prevMonthLast = new Date(year, month, 0);
    for (let i = startOffset - 1; i >= 0; i--) {
      const d = new Date(year, month - 1, prevMonthLast.getDate() - i);
      days.push(this.buildDay(d, false, today));
    }

    // Current month days
    for (let i = 1; i <= lastDay.getDate(); i++) {
      const d = new Date(year, month, i);
      days.push(this.buildDay(d, true, today));
    }

    // Fill trailing days
    const remaining = 42 - days.length;
    for (let i = 1; i <= remaining; i++) {
      const d = new Date(year, month + 1, i);
      days.push(this.buildDay(d, false, today));
    }

    return days;
  });

  ngOnInit(): void {
    if (this.selectedDate()) {
      const d = new Date(this.selectedDate());
      this.currentYear.set(d.getFullYear());
      this.currentMonth.set(d.getMonth());
    }
  }

  protected prevMonth(): void {
    let m = this.currentMonth() - 1;
    let y = this.currentYear();
    if (m < 0) { m = 11; y--; }
    this.currentMonth.set(m);
    this.currentYear.set(y);
    this.monthChanged.emit({ year: y, month: m + 1 });
  }

  protected nextMonth(): void {
    let m = this.currentMonth() + 1;
    let y = this.currentYear();
    if (m > 11) { m = 0; y++; }
    this.currentMonth.set(m);
    this.currentYear.set(y);
    this.monthChanged.emit({ year: y, month: m + 1 });
  }

  protected selectDate(day: CalendarDay): void {
    if (!day.isCurrentMonth) return;
    this.dateSelected.emit(day.date);
  }

  protected getStatus(date: string): ReportStatus | null {
    return this.statusMap()[date] ?? null;
  }

  private buildDay(d: Date, isCurrentMonth: boolean, today: string): CalendarDay {
    const date = d.toISOString().slice(0, 10);
    const dow = d.getDay();
    return {
      date,
      day: d.getDate(),
      isCurrentMonth,
      isToday: date === today,
      isWeekend: dow === 0 || dow === 6,
      isFuture: date > today,
    };
  }
}
