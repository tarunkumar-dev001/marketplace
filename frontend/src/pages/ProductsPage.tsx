import { isAxiosError } from 'axios'
import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { getProducts } from '../api/products'
import { usePageTitle } from '../hooks/usePageTitle'
import type { Product, ProductPage, ProductQuery } from '../types/product'

const PAGE_SIZE = 12

function priceLabel(price: Product['price']) {
  const value = typeof price === 'string' ? Number(price) : price
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(value)
}

function productErrorMessage(error: unknown) {
  if (isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message ?? 'We could not load the catalog. Please try again.'
  }
  return error instanceof Error ? error.message : 'We could not load the catalog. Please try again.'
}

export function ProductsPage() {
  usePageTitle('Products')

  const [products, setProducts] = useState<ProductPage | null>(null)
  const [searchInput, setSearchInput] = useState('')
  const [categoryInput, setCategoryInput] = useState('')
  const [query, setQuery] = useState<ProductQuery>({ page: 0, size: PAGE_SIZE, sortBy: 'createdAt', direction: 'desc' })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let current = true
    setLoading(true)
    setError(null)

    getProducts(query)
      .then((page) => {
        if (current) setProducts(page)
      })
      .catch((requestError: unknown) => {
        if (current) setError(productErrorMessage(requestError))
      })
      .finally(() => {
        if (current) setLoading(false)
      })

    return () => {
      current = false
    }
  }, [query])

  const resultLabel = useMemo(() => {
    if (!products) return ''
    return `${products.totalElements} ${products.totalElements === 1 ? 'product' : 'products'}`
  }, [products])

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setQuery((current) => ({
      ...current,
      page: 0,
      search: searchInput.trim() || undefined,
      category: categoryInput.trim() || undefined,
    }))
  }

  function clearFilters() {
    setSearchInput('')
    setCategoryInput('')
    setQuery({ page: 0, size: PAGE_SIZE, sortBy: 'createdAt', direction: 'desc' })
  }

  function changePage(nextPage: number) {
    setQuery((current) => ({ ...current, page: nextPage }))
  }

  const page = products?.page ?? 0
  const totalPages = products?.totalPages ?? 0
  const hasFilters = Boolean(query.search || query.category)

  return (
    <div className="space-y-8">
      <section className="overflow-hidden rounded-3xl bg-ink px-6 py-10 text-white shadow-xl shadow-slate-300/40 sm:px-10 sm:py-12">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-sky-200">The catalog</p>
        <div className="mt-3 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">Find products worth choosing.</h1>
            <p className="mt-3 max-w-2xl leading-7 text-slate-300">Explore active products from marketplace sellers, with straightforward search and filtering.</p>
          </div>
          {!loading && products && <span className="shrink-0 rounded-full bg-white/10 px-4 py-2 text-sm font-medium text-sky-100">{resultLabel}</span>}
        </div>
      </section>

      <form onSubmit={applyFilters} className="grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm md:grid-cols-[1fr_0.55fr_auto] md:items-end">
        <label className="grid gap-2 text-sm font-semibold text-slate-700">
          Search products
          <input value={searchInput} onChange={(event) => setSearchInput(event.target.value)} placeholder="Search by name" className="h-11 rounded-xl border border-slate-300 px-3 text-base font-normal outline-none transition focus:border-brand focus:ring-3 focus:ring-sky-100" />
        </label>
        <label className="grid gap-2 text-sm font-semibold text-slate-700">
          Category
          <input value={categoryInput} onChange={(event) => setCategoryInput(event.target.value)} placeholder="All categories" className="h-11 rounded-xl border border-slate-300 px-3 text-base font-normal outline-none transition focus:border-brand focus:ring-3 focus:ring-sky-100" />
        </label>
        <div className="flex gap-2">
          <button type="submit" className="h-11 rounded-xl bg-brand px-5 text-sm font-semibold text-white shadow-sm hover:bg-[#102f59]">Apply</button>
          {hasFilters && <button type="button" onClick={clearFilters} className="h-11 rounded-xl px-4 text-sm font-semibold text-brand hover:bg-slate-50">Clear</button>}
        </div>
      </form>

      {loading && <ProductGridSkeleton />}

      {!loading && error && (
        <section role="alert" className="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-rose-950">
          <h2 className="font-semibold">Catalog unavailable</h2>
          <p className="mt-1 text-sm leading-6">{error}</p>
          <button type="button" onClick={() => setQuery((current) => ({ ...current }))} className="mt-4 rounded-lg bg-white px-4 py-2 text-sm font-semibold text-rose-800 shadow-sm ring-1 ring-rose-200 hover:bg-rose-100">Try again</button>
        </section>
      )}

      {!loading && !error && products?.items.length === 0 && (
        <section className="rounded-3xl border border-dashed border-slate-300 bg-white px-6 py-16 text-center">
          <h2 className="text-xl font-semibold">No products found</h2>
          <p className="mx-auto mt-2 max-w-md leading-7 text-slate-600">Try a different search or category. The public catalog only shows active products.</p>
          {hasFilters && <button type="button" onClick={clearFilters} className="mt-5 rounded-xl bg-brand px-4 py-2.5 text-sm font-semibold text-white hover:bg-[#102f59]">Clear filters</button>}
        </section>
      )}

      {!loading && !error && products && products.items.length > 0 && (
        <>
          <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
            {products.items.map((product) => <ProductCard key={product.id} product={product} />)}
          </div>
          {totalPages > 1 && (
            <nav aria-label="Product pagination" className="flex items-center justify-between gap-4 border-t border-slate-200 pt-6">
              <button type="button" disabled={page === 0} onClick={() => changePage(page - 1)} className="rounded-xl border border-slate-300 px-4 py-2.5 text-sm font-semibold text-slate-700 enabled:hover:border-brand enabled:hover:text-brand disabled:cursor-not-allowed disabled:opacity-45">Previous</button>
              <span className="text-sm font-medium text-slate-600">Page {page + 1} of {totalPages}</span>
              <button type="button" disabled={page >= totalPages - 1} onClick={() => changePage(page + 1)} className="rounded-xl border border-slate-300 px-4 py-2.5 text-sm font-semibold text-slate-700 enabled:hover:border-brand enabled:hover:text-brand disabled:cursor-not-allowed disabled:opacity-45">Next</button>
            </nav>
          )}
        </>
      )}
    </div>
  )
}

function ProductCard({ product }: { product: Product }) {
  const availability = product.stockQuantity > 0 ? `${product.stockQuantity} in stock` : 'Out of stock'

  return (
    <article className="group flex min-h-72 flex-col rounded-2xl border border-slate-200 bg-white p-6 shadow-sm transition-shadow hover:shadow-md">
      <div className="flex items-start justify-between gap-4">
        <span className="rounded-full bg-sky-50 px-3 py-1 text-xs font-semibold text-brand">{product.category}</span>
        <span className={`text-xs font-semibold ${product.stockQuantity > 0 ? 'text-emerald-700' : 'text-slate-500'}`}>{availability}</span>
      </div>
      <h2 className="mt-6 text-xl font-semibold tracking-tight text-ink"><Link to={`/products/${product.id}`} className="hover:text-brand">{product.name}</Link></h2>
      <p className="mt-3 line-clamp-3 flex-1 leading-7 text-slate-600">{product.description || 'Product details are available from the seller.'}</p>
      <div className="mt-6 flex items-center justify-between border-t border-slate-100 pt-5">
        <span className="text-xl font-semibold text-ink">{priceLabel(product.price)}</span>
        <Link to={`/products/${product.id}`} className="text-sm font-semibold text-brand hover:underline">View details</Link>
      </div>
    </article>
  )
}

function ProductGridSkeleton() {
  return (
    <div aria-label="Loading products" className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
      {Array.from({ length: 6 }, (_, index) => <div key={index} className="h-72 animate-pulse rounded-2xl border border-slate-200 bg-white p-6"><div className="h-6 w-24 rounded bg-slate-100" /><div className="mt-8 h-7 w-3/4 rounded bg-slate-100" /><div className="mt-4 h-4 w-full rounded bg-slate-100" /><div className="mt-2 h-4 w-5/6 rounded bg-slate-100" /><div className="mt-12 h-8 w-28 rounded bg-slate-100" /></div>)}
    </div>
  )
}
