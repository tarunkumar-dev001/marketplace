import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from './AuthContext'

export function RequireVendor({ children }: { children: ReactNode }) {
  const { user, isRestoring } = useAuth()
  const location = useLocation()

  if (isRestoring) {
    return <div className="grid min-h-48 place-items-center text-sm font-medium text-slate-500">Restoring your session...</div>
  }
  if (user?.role !== 'VENDOR') {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  return <>{children}</>
}
