export type UserRole = 'CUSTOMER' | 'VENDOR' | 'ADMIN'

export interface RegisteredUser {
  id: number
  email: string
  role: UserRole
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface LoginResult {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  id: number
  email: string
  role: UserRole
  active: boolean
}

export interface Credentials {
  email: string
  password: string
}
