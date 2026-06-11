export type ReportStatus = 'reported' | 'bad_rating' | 'missing';

export interface DailyMemberReport {
  userId: string;
  firstName: string;
  lastName: string;
  status: ReportStatus;
  totalHours: number;
  recordCount: number;
  minRating: number | null;
}

export interface MemberActivityRecord {
  id: string;
  entryDate: string;
  curriculumActivityId: string | null;
  activityLabel: string;
  hours: number;
  notes: string;
  rating: number | null;
  teamId: string | null;
  location: string | null;
}

export interface CurriculumNodeDto {
  id: string;
  parentId: string | null;
  nodeType: 'category' | 'activity';
  key: string;
  label: string;
  sortOrder: number;
  competencyIds: string[];
}

export interface CompetencyDto {
  id: string;
  code: string;
  description: string;
}

export interface CurriculumOverview {
  nodes: CurriculumNodeDto[];
  competencies: CompetencyDto[];
}

export interface ActivitySummary {
  activityId: string;
  activityName: string;
  totalHours: number;
}

export interface LocationSummary {
  location: string;
  totalHours: number;
}

export interface RatingSummary {
  activityId: string;
  activityName: string;
  averageRating: number;
}

export interface NgxChartEntry {
  name: string;
  value: number;
}

export interface MemberAbsence {
  id: string;
  absenceTypeId: string;
  startDate: string;
  endDate: string;
  rrule: string | null;
  isRecurring: boolean;
  dayFraction: number;
  notes: string | null;
  currentSemester: string | null;
}

export interface AbsenceTypeMeta {
  id: string;
  labelKey: string;
}

/** Fallback-Farbe, wenn die gewählte Palette keine Farbe für einen Typ liefert. */
export const DEFAULT_ABSENCE_COLOR = '#9ca3af';

/** Reihenfolge bestimmt die Säulen-Reihenfolge im Diagramm (Farben kommen aus der Palette). */
export const ABSENCE_TYPES: AbsenceTypeMeta[] = [
  { id: 'sick', labelKey: 'reports.memberDetail.absences.type.sick' },
  { id: 'vacation', labelKey: 'reports.memberDetail.absences.type.vacation' },
  { id: 'military', labelKey: 'reports.memberDetail.absences.type.military' },
  { id: 'uk', labelKey: 'reports.memberDetail.absences.type.uk' },
  { id: 'berufsschule', labelKey: 'reports.memberDetail.absences.type.berufsschule' },
  { id: 'custom', labelKey: 'reports.memberDetail.absences.type.custom' },
];

/** Lookup nach Typ-Id für die Einzelliste (einmal aufgebaut, nicht pro Recompute). */
export const ABSENCE_TYPE_BY_ID = new Map<string, AbsenceTypeMeta>(
  ABSENCE_TYPES.map(t => [t.id, t])
);
