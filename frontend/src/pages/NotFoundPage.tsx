import { Link } from 'react-router-dom'
import { usePageTitle } from '../hooks/usePageTitle'

export function NotFoundPage() {
  usePageTitle('Page not found')
  return <section className="mx-auto max-w-xl rounded-3xl border border-slate-200 bg-white px-8 py-16 text-center shadow-sm"><p className="text-sm font-semibold text-brand">404</p><h1 className="mt-3 text-3xl font-semibold">Page not found</h1><p className="mt-3 text-slate-600">The page you requested does not exist.</p><Link to="/" className="mt-7 inline-flex rounded-xl bg-brand px-5 py-3 font-semibold text-white">Go home</Link></section>
}
