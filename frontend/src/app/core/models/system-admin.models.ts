export interface Invite {
  id: string
  email: string
  userRole: string
  token: string
  status: 'pending' | 'accepted' | 'expired'
  createdAt: string
  expiresAt: string
}
