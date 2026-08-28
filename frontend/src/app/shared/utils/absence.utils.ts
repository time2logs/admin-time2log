import { formatLocalDate } from './date.utils';

/** Horizont in Tagen, bis zu dem wiederkehrende Absenzen ohne UNTIL expandiert werden. */
export const RECURRING_CAP_DAYS = 365;

const MS_PER_DAY = 86_400_000;

const RRULE_DTSTART = /DTSTART:(\d{8})/;
const RRULE_FREQ = /FREQ=([^;\s]+)/i;
const RRULE_BYDAY = /BYDAY=([^;\s]+)/i;
const RRULE_UNTIL = /UNTIL=(\d{8})/;

const WEEKDAY_CODES: Record<string, number> = { SU: 0, MO: 1, TU: 2, WE: 3, TH: 4, FR: 5, SA: 6 };

export interface AbsenceLike {
  startDate: string;
  endDate: string;
  rrule: string | null;
  isRecurring: boolean;
  dayFraction: number;
}

export interface RecurringMeta {
  occurrenceCount: number;
  untilDate: string | null;
  isOngoing: boolean;
  capDate: string;
}

export interface ExpandedAbsence {
  dates: string[];
  dateSet: Set<string>;
  daysBySemester: Map<string, number>;
  totalDays: number;
  weekendOnly: boolean;
  invalid: boolean;
  recurring: RecurringMeta | null;
}

interface ParsedRrule {
  dtstartMs: number;
  freq: 'DAILY' | 'WEEKLY';
  byday: Set<number> | null;
  untilMs: number | null;
}

function parseUtcDate(dateStr: string): number | null {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) return null;
  const ms = Date.parse(`${dateStr}T00:00:00Z`);
  return Number.isNaN(ms) ? null : ms;
}

function parseCompactDate(compact: string): number | null {
  const ms = Date.parse(`${compact.slice(0, 4)}-${compact.slice(4, 6)}-${compact.slice(6, 8)}T00:00:00Z`);
  return Number.isNaN(ms) ? null : ms;
}

function toIsoDate(ms: number): string {
  return new Date(ms).toISOString().slice(0, 10);
}

function isWeekend(ms: number): boolean {
  const dow = new Date(ms).getUTCDay();
  return dow === 0 || dow === 6;
}

function parseRrule(rrule: string | null, fallbackStartMs: number | null): ParsedRrule | null {
  if (!rrule) return null;

  const dtstartMatch = RRULE_DTSTART.exec(rrule);
  const dtstartMs = dtstartMatch ? parseCompactDate(dtstartMatch[1]) : fallbackStartMs;
  if (dtstartMs === null) return null;

  const freqMatch = RRULE_FREQ.exec(rrule);
  const freq = freqMatch ? freqMatch[1].trim().toUpperCase() : null;
  if (freq !== 'DAILY' && freq !== 'WEEKLY') return null;

  const bydayMatch = RRULE_BYDAY.exec(rrule);
  let byday: Set<number> | null = null;
  if (bydayMatch) {
    const days = bydayMatch[1]
      .split(',')
      .map(token => WEEKDAY_CODES[token.trim().toUpperCase().slice(-2)])
      .filter((n): n is number => n !== undefined);
    byday = days.length > 0 ? new Set(days) : null;
  }

  const untilMatch = RRULE_UNTIL.exec(rrule);
  return { dtstartMs, freq, byday, untilMs: untilMatch ? parseCompactDate(untilMatch[1]) : null };
}

function capMsFor(today: Date): number {
  const todayMs = parseUtcDate(formatLocalDate(today));
  const base = todayMs ?? Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate());
  return base + RECURRING_CAP_DAYS * MS_PER_DAY;
}

/** Semester-Schlüssel eines Kalenderdatums: Aug–Dez → YYYY/S1, Jan–Jul → YYYY−1/S2. */
export function semesterKeyOf(dateStr: string): string {
  const year = Number(dateStr.slice(0, 4));
  const month = Number(dateStr.slice(5, 7));
  return month >= 8 ? `${year}/S1` : `${year - 1}/S2`;
}

function expand(absence: AbsenceLike, today: Date): { dates: string[]; invalid: boolean; rule: ParsedRrule | null } {
  const dates = new Set<string>();
  const push = (ms: number) => {
    if (!isWeekend(ms)) dates.add(toIsoDate(ms));
  };

  if (!absence.isRecurring) {
    const startMs = parseUtcDate(absence.startDate);
    const endMs = parseUtcDate(absence.endDate);
    if (startMs === null || endMs === null || endMs < startMs) {
      return { dates: [], invalid: true, rule: null };
    }
    for (let ms = startMs; ms <= endMs; ms += MS_PER_DAY) push(ms);
    return { dates: [...dates].sort(), invalid: false, rule: null };
  }

  const rule = parseRrule(absence.rrule, parseUtcDate(absence.startDate));
  if (!rule) {
    return { dates: [], invalid: true, rule: null };
  }

  const boundMs = rule.untilMs ?? capMsFor(today);
  const dtstartDow = new Date(rule.dtstartMs).getUTCDay();
  for (let ms = rule.dtstartMs; ms <= boundMs; ms += MS_PER_DAY) {
    const dow = new Date(ms).getUTCDay();
    if (rule.freq === 'WEEKLY') {
      if (rule.byday ? !rule.byday.has(dow) : dow !== dtstartDow) continue;
    } else if (rule.byday && !rule.byday.has(dow)) {
      continue;
    }
    push(ms);
  }
  return { dates: [...dates].sort(), invalid: false, rule };
}

/**
 * Gezählte Abwesenheitstage einer Absenz: Werktage (ohne Sa/So), dedupliziert und
 * aufsteigend sortiert. Nicht-wiederkehrend = Bereich [start, end]; wiederkehrend =
 * rrule-Vorkommen ab DTSTART bis UNTIL, ohne UNTIL bis heute + 365 Tage.
 */
export function expandAbsenceDates(absence: AbsenceLike, today: Date = new Date()): string[] {
  return expand(absence, today).dates;
}

/** Expansion inklusive Semester-Zuordnung pro Datum, Tagesbruchteilen und Anzeige-Metadaten. */
export function expandAbsence(absence: AbsenceLike, today: Date = new Date()): ExpandedAbsence {
  const { dates, invalid, rule } = expand(absence, today);
  const fraction = absence.dayFraction || 1;

  const daysBySemester = new Map<string, number>();
  let totalDays = 0;
  for (const date of dates) {
    const key = semesterKeyOf(date);
    daysBySemester.set(key, (daysBySemester.get(key) ?? 0) + fraction);
    totalDays += fraction;
  }

  let recurring: RecurringMeta | null = null;
  if (rule) {
    recurring = {
      occurrenceCount: dates.length,
      untilDate: rule.untilMs !== null ? toIsoDate(rule.untilMs) : null,
      isOngoing: rule.untilMs === null,
      capDate: toIsoDate(rule.untilMs ?? capMsFor(today)),
    };
  }

  return {
    dates,
    dateSet: new Set(dates),
    daysBySemester,
    totalDays,
    weekendOnly: !invalid && totalDays === 0,
    invalid,
    recurring,
  };
}
