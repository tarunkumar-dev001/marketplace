import { useEffect, useState } from 'react'
import { apiClient } from '../api/client'

export function ServerWakeBanner() {
  const [visible, setVisible] = useState(false)
  const [dismissed, setDismissed] = useState(false)

  useEffect(() => {
    let settled = false
    const timeout = window.setTimeout(() => { if (!settled) setVisible(true) }, 3000)
    void apiClient.get('/api/health').then(() => { settled = true; setVisible(false) }).catch(() => { settled = true })
    return () => window.clearTimeout(timeout)
  }, [])

  if (!visible || dismissed) return null
  return <div className="bg-amber-100 px-5 py-3 text-center text-sm text-amber-950">Server is waking up (free hosting) - first load can take up to a minute<button type="button" aria-label="Dismiss server wake message" onClick={() => setDismissed(true)} className="ml-4 font-bold">x</button></div>
}
