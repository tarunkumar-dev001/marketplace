import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { AUTH_SESSION_INVALIDATED_EVENT, clearAccessToken, getAccessToken, setAccessToken } from './session'
import type { LoginResult, UserRole } from '../types/auth'

interface AuthenticatedSession {
  id: number
  email: string
  role: 'CUSTOMER' | 'VENDOR' | 'ADMIN'
  expiresAt: number
}

interface JwtClaims {
  sub?: string
  email?: string
  role?: string
  exp?: number
}

interface AuthContextValue {
  user: AuthenticatedSession | null
  isAuthenticated: boolean
  isRestoring: boolean
  signIn: (result: LoginResult) => boolean
  signOut: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

function decodeSession(accessToken: string): AuthenticatedSession | null {
  try {
    const payload = accessToken.split('.')[1]
    if (!payload) return null
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const paddedBase64 = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')
    const bytes = Uint8Array.from(atob(paddedBase64), (character) => character.charCodeAt(0))
    const claims = JSON.parse(new TextDecoder().decode(bytes)) as JwtClaims
    const id = Number(claims.sub)
    const expiresAt = claims.exp ?? 0

    if (!Number.isSafeInteger(id) || id <= 0 || (claims.role !== 'CUSTOMER' && claims.role !== 'VENDOR' && claims.role !== 'ADMIN') || !claims.email || expiresAt <= Date.now() / 1000) {
      return null
    }
    return { id, email: claims.email, role: claims.role, expiresAt }
  } catch {
    return null
  }
}

function restoreSession(): AuthenticatedSession | null {
  const accessToken = getAccessToken()
  if (!accessToken) return null
  const session = decodeSession(accessToken)
  if (!session) clearAccessToken()
  return session
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthenticatedSession | null>(null)
  const [isRestoring, setIsRestoring] = useState(true)

  const signOut = useCallback(() => {
    clearAccessToken()
    setUser(null)
  }, [])

  const signIn = useCallback((result: LoginResult) => {
    if (!result.active || !isSupportedRole(result.role)) {
      clearAccessToken()
      setUser(null)
      return false
    }
    const session = decodeSession(result.accessToken)
    if (!session || session.role !== result.role) {
      clearAccessToken()
      setUser(null)
      return false
    }
    setAccessToken(result.accessToken)
    setUser(session)
    return true
  }, [])

  useEffect(() => {
    setUser(restoreSession())
    setIsRestoring(false)
  }, [])

  useEffect(() => {
    const clearSession = () => setUser(null)
    window.addEventListener(AUTH_SESSION_INVALIDATED_EVENT, clearSession)
    return () => window.removeEventListener(AUTH_SESSION_INVALIDATED_EVENT, clearSession)
  }, [])

  const value = useMemo<AuthContextValue>(() => ({
    user,
    isAuthenticated: user !== null,
    isRestoring,
    signIn,
    signOut,
  }), [user, isRestoring, signIn, signOut])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

function isSupportedRole(role: UserRole): role is 'CUSTOMER' | 'VENDOR' | 'ADMIN' {
  return role === 'CUSTOMER' || role === 'VENDOR' || role === 'ADMIN'
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within AuthProvider')
  return context
}
