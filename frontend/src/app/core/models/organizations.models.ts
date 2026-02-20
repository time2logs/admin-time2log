export interface Organization {
  id: string
  name: string
}

export interface Profession {
  id: string,
  key: string,
  label: string
}

export interface Invite {
  id: string
  organizationId: string
  email: string
  userRole: string
  token: string
  status: 'pending' | 'accepted' | 'expired'
  createdAt: string
  expiresAt: string
}
