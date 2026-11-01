import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { authErrorMessage, login } from '../api/auth'
import { useAuth } from '../auth/AuthContext'
import { usePageTitle } from '../hooks/usePageTitle'

interface LoginLocationState {
  registered?: boolean
  email?: string
  from?: { pathname?: string }
}

export function LoginPage() {
  usePageTitle('Sign in')
  const navigate = useNavigate()
  const location = useLocation()
  const { signIn } = useAuth()
  const state = location.state as LoginLocationState | null
  const [email, setEmail] = useState(state?.email ?? '')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (submitting) return
    setError(null)

    const normalizedEmail = email.trim()
    if (!normalizedEmail || !password) {
      setError('Enter your email address and password.')
      return
    }

    setSubmitting(true)
    try {
      const result = await login({ email: normalizedEmail, password })
      if (result.role !== 'CUSTOMER' && result.role !== 'VENDOR' && result.role !== 'ADMIN') {
        setError('This account type is not supported by the storefront.')
        return
      }
      if (!result.active) {
        setError('This account is inactive. Please contact support.')
        return
      }
      if (!signIn(result)) {
        setError('We could not restore your session. Please sign in again.')
        return
      }
      const destination = result.role === 'ADMIN' ? '/admin/dashboard' : result.role === 'VENDOR' ? '/vendor/dashboard' : state?.from?.pathname
      navigate(destination?.startsWith('/') ? destination : '/products', { replace: true })
    } catch (requestError) {
      setError(authErrorMessage(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="mx-auto grid max-w-5xl overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-xl shadow-slate-200/50 lg:grid-cols-[0.85fr_1.15fr]">
      <div className="hidden bg-[radial-gradient(circle_at_25%_20%,#38bdf8,transparent_18rem),linear-gradient(145deg,#17375e,#11233f)] p-10 text-white lg:block">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-sky-200">Marketline</p>
        <h1 className="mt-5 text-4xl font-semibold leading-tight">Pick up where your shopping left off.</h1>
        <p className="mt-5 max-w-sm leading-7 text-slate-300">Access your customer cart, checkout, and order history from one secure storefront.</p>
      </div>
      <div className="px-6 py-10 sm:px-10 sm:py-12">
        <p className="text-sm font-semibold text-brand">Customer sign-in</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight">Welcome back</h1>
        <p className="mt-3 leading-7 text-slate-600">Sign in to manage your purchases and orders.</p>
        {state?.registered && <div role="status" className="mt-6 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm leading-6 text-emerald-900">Your customer account is ready. Sign in to continue.</div>}
        {error && <div role="alert" className="mt-6 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm leading-6 text-rose-900">{error}</div>}
        <form onSubmit={submit} className="mt-7 space-y-5">
          <label className="grid gap-2 text-sm font-semibold text-slate-700">Email address<input type="email" autoComplete="email" maxLength={320} value={email} onChange={(event) => setEmail(event.target.value)} required disabled={submitting} className="h-11 rounded-xl border border-slate-300 px-3 text-base font-normal outline-none transition focus:border-brand focus:ring-3 focus:ring-sky-100 disabled:bg-slate-50" /></label>
          <label className="grid gap-2 text-sm font-semibold text-slate-700">Password<input type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} required disabled={submitting} className="h-11 rounded-xl border border-slate-300 px-3 text-base font-normal outline-none transition focus:border-brand focus:ring-3 focus:ring-sky-100 disabled:bg-slate-50" /></label>
          <button type="submit" disabled={submitting} className="flex h-11 w-full items-center justify-center rounded-xl bg-brand px-5 text-sm font-semibold text-white shadow-sm hover:bg-[#102f59] disabled:cursor-not-allowed disabled:opacity-60">{submitting ? 'Signing in...' : 'Sign in'}</button>
        </form>
        <p className="mt-7 text-sm text-slate-600">New to Marketline? <Link className="font-semibold text-brand hover:underline" to="/register">Create a customer account</Link></p>
      </div>
    </section>
  )
}
