import { createContext, useCallback, useContext, useState, type ReactNode } from 'react'

type ToastContextValue = (message: string) => void
const ToastContext = createContext<ToastContextValue>(() => undefined)

export function ToastProvider({ children }: { children: ReactNode }) {
  const [message, setMessage] = useState<string | null>(null)
  const showToast = useCallback((nextMessage: string) => {
    setMessage(nextMessage)
    window.setTimeout(() => setMessage(null), 3500)
  }, [])

  return <ToastContext.Provider value={showToast}><>{children}</>{message && <div role="status" className="fixed bottom-5 right-5 z-50 rounded-xl bg-ink px-5 py-3 text-sm font-semibold text-white shadow-xl">{message}</div>}</ToastContext.Provider>
}

export const useToast = () => useContext(ToastContext)
