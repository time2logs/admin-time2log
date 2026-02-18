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
