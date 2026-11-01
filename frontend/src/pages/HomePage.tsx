import { Link } from 'react-router-dom'
import { usePageTitle } from '../hooks/usePageTitle'
import { useEffect, useState } from 'react'
import { getProducts } from '../api/products'
import type { Product } from '../types/product'
import { ProductImage } from '../components/ProductImage'

const highlights = [
  ['Curated marketplace', 'Discover active products from trusted sellers.'],
  ['Clear ordering', 'Review your cart and complete checkout with confidence.'],
  ['Order visibility', 'Keep purchase history and status in one place.'],
]

export function HomePage() {
  usePageTitle('Marketplace')
  const [featured, setFeatured] = useState<Product[]>([])
  useEffect(() => { void getProducts({ page: 0, size: 8, sortBy: 'createdAt', direction: 'desc' }).then((page) => setFeatured(page.items)).catch(() => undefined) }, [])
  const categories = ['Electronics', 'Fashion', 'Home & Kitchen', 'Books', 'Sports', 'Beauty']

  return (
    <div className="space-y-12">
      <section className="grid overflow-hidden rounded-3xl bg-ink text-white shadow-xl shadow-slate-300/40 lg:grid-cols-[1.2fr_0.8fr]">
        <div className="px-7 py-14 sm:px-12 sm:py-20">
          <p className="mb-5 text-sm font-semibold uppercase tracking-[0.2em] text-sky-200">Enterprise marketplace</p>
          <h1 className="max-w-xl text-4xl font-semibold tracking-tight sm:text-6xl">Everything your business needs, in one marketplace.</h1>
          <p className="mt-6 max-w-xl text-lg leading-8 text-slate-300">A straightforward customer experience for discovering products, managing purchases, and tracking every order.</p>
          <div className="mt-9 flex flex-wrap gap-3">
            <Link className="rounded-xl bg-white px-5 py-3 font-semibold text-brand shadow-sm hover:bg-slate-100" to="/products">Browse products</Link>
            <Link className="rounded-xl border border-slate-500 px-5 py-3 font-semibold text-white hover:border-slate-300 hover:bg-white/5" to="/register">Create an account</Link>
          </div>
        </div>
        <div className="relative hidden bg-[radial-gradient(circle_at_70%_30%,#3b82f6,transparent_24rem),linear-gradient(135deg,#17375e,#11233f)] lg:block">
          <div className="absolute inset-10 rounded-3xl border border-white/15 bg-white/10 p-7 backdrop-blur-sm">
            <p className="text-sm text-sky-100">Designed for clarity</p>
            <p className="mt-4 text-3xl font-semibold leading-tight">Browse. Buy. Keep track.</p>
            <div className="mt-12 space-y-3">
              <div className="h-3 w-4/5 rounded bg-white/25" />
              <div className="h-3 w-3/5 rounded bg-white/15" />
              <div className="h-24 rounded-2xl bg-white/10" />
            </div>
          </div>
        </div>
      </section>
      <section><h2 className="text-3xl font-semibold">Featured products</h2><div className="mt-6 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">{featured.map((product) => <Link key={product.id} to={`/products/${product.id}`} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"><ProductImage imageUrl={product.imageUrl} category={product.category} name={product.name} className="h-36 w-full rounded-xl object-cover" /><p className="mt-4 font-semibold">{product.name}</p><p className="mt-1 text-sm text-brand">{product.category}</p></Link>)}</div></section>
      <section><h2 className="text-2xl font-semibold">Shop by category</h2><div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{categories.map((category) => <Link key={category} to={`/products?category=${encodeURIComponent(category)}`} className="rounded-2xl bg-gradient-to-br from-sky-100 to-indigo-100 p-6 text-lg font-semibold text-brand">{category}</Link>)}</div></section>
      <section>
        <div className="mb-7 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
          <div><p className="text-sm font-semibold text-brand">Built around the customer</p><h2 className="mt-2 text-3xl font-semibold tracking-tight">A dependable purchasing flow</h2></div>
          <Link className="text-sm font-semibold text-brand hover:underline" to="/products">Explore the catalog →</Link>
        </div>
        <div className="grid gap-5 md:grid-cols-3">
          {highlights.map(([title, detail], index) => (
            <article key={title} className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
              <span className="grid size-9 place-items-center rounded-lg bg-sky-50 font-semibold text-brand">0{index + 1}</span>
              <h3 className="mt-5 text-lg font-semibold">{title}</h3>
              <p className="mt-2 leading-7 text-slate-600">{detail}</p>
            </article>
          ))}
        </div>
      </section>
    </div>
  )
}
