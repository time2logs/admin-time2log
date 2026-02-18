export interface CurriculumImport {
  id: string
  version: string
  status: 'pending' | 'applied' | 'failed'
  error: string | null
  created_at: string
}
