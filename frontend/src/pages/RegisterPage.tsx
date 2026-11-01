import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authErrorMessage, registerCustomer } from '../api/auth'
import { usePageTitle } from '../hooks/usePageTitle'

export function RegisterPage() {
  usePageTitle('Create account')
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (submitting) return
    setError(null)

    const normalizedEmail = email.trim()
    if (!normalizedEmail || !password) {
      setError('Enter an email address and password.')
      return
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match.')
      return
    }
    if (password.length > 72) {
      setError('Passwords must be 72 characters or fewer.')
      return
    }

    setSubmitting(true)
    try {
      await registerCustomer({ email: normalizedEmail, password })
      navigate('/login', { replace: true, state: { registered: true, email: normalizedEmail } })
    } catch (requestError) {
      setError(authErrorMessage(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="mx-auto grid max-w-5xl overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-xl shadow-slate-200/50 lg:grid-cols-[0.85fr_1.15fr]">
      <div className="hidden bg-[radial-gradient(circle_at_30%_25%,#38bdf8,transparent_18rem),linear-gradient(145deg,#17375e,#11233f)] p-10 text-white lg:block">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-sky-200">Customer account</p>
        <h1 className="mt-5 text-4xl font-semibold leading-tight">A simpler way to shop across sellers.</h1>
        <p className="mt-5 max-w-sm leading-7 text-slate-300">Create one customer account to build your cart and keep track of every order.</p>
      </div>
      <div className="px-6 py-10 sm:px-10 sm:py-12">
        <p className="text-sm font-semibold text-brand">Join Marketline</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight">Create your account</h1>
        <p className="mt-3 leading-7 text-slate-600">Registration creates a customer account for the marketplace.</p>
        {error && <div role="alert" className="mt-6 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm leading-6 text-rose-900">{error}</div>}
        <form onSubmit={submit} className="mt-7 space-y-5">
          <label className="grid gap-2 text-sm font-semibold text-slate-700">Email address<input type="email" autoComplete="email" maxLength={320} value={email} onChange={(event) => setEmail(event.target.value)} required disabled={submitting} className="h-11 rounded-xl border border-slate-300 px-3 text-base font-normal outline-none transition focus:border-brand focus:ring-3 focus:ring-sky-100 disabled:bg-slate-50" /></label>
          <label className="grid gap-2 text-sm font-semibold text-slate-700">Password<input type="password" autoComplete="new-password" maxLength={72} value={password} onChange={(event) => setPassword(event.target.value)} required disabled={submitting} className="h-11 rounded-xl border border-slate-300 px-3 text-base font-normal outline-none transition focus:border-brand focus:ring-3 focus:ring-sky-100 disabled:bg-slate-50" /></label>
          <label className="grid gap-2 text-sm font-semibold text-slate-700">Confirm password<input type="password" autoComplete="new-password" maxLength={72} value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} required disabled={submitting} className="h-11 rounded-xl border border-slate-300 px-3 text-base font-normal outline-none transition focus:border-brand focus:ring-3 focus:ring-sky-100 disabled:bg-slate-50" /></label>
          <button type="submit" disabled={submitting} className="flex h-11 w-full items-center justify-center rounded-xl bg-brand px-5 text-sm font-semibold text-white shadow-sm hover:bg-[#102f59] disabled:cursor-not-allowed disabled:opacity-60">{submitting ? 'Creating account...' : 'Create customer account'}</button>
        </form>
        <p className="mt-7 text-sm text-slate-600">Already have an account? <Link className="font-semibold text-brand hover:underline" to="/login">Sign in</Link></p>
      </div>
    </section>
  )
}
