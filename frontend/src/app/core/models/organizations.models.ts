export interface Organization {
  id: string
  name: string
  createdBy: string
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

export interface Reminder {
  id: number
  channel: 'EMAIL' | 'SMS'
  sendTime: string
  idleDays: number
  sendDay: string
}
