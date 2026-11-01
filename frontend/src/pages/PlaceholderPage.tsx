import { Link } from 'react-router-dom'
import { usePageTitle } from '../hooks/usePageTitle'

interface PlaceholderPageProps {
  title: string
  description: string
}

export function PlaceholderPage({ title, description }: PlaceholderPageProps) {
  usePageTitle(title)

  return (
    <section className="mx-auto max-w-2xl rounded-3xl border border-slate-200 bg-white px-7 py-14 text-center shadow-sm sm:px-12">
      <p className="text-sm font-semibold uppercase tracking-[0.18em] text-brand">Coming next</p>
      <h1 className="mt-4 text-4xl font-semibold tracking-tight">{title}</h1>
      <p className="mx-auto mt-4 max-w-lg text-lg leading-8 text-slate-600">{description}</p>
      <Link className="mt-8 inline-flex rounded-xl bg-brand px-5 py-3 font-semibold text-white hover:bg-[#102f59]" to="/">Return home</Link>
    </section>
  )
}
