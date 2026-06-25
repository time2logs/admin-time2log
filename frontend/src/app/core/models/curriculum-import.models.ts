export interface CurriculumImport {
  id: string
  version: string
  status: 'pending' | 'applied' | 'failed' | 'superseded'
  error: string | null
  created_at: string
}
