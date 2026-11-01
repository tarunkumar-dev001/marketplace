import { isAxiosError } from 'axios'
import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { addCartItem, cartErrorMessage } from '../api/cart'
import { useAuth } from '../auth/AuthContext'
import { getProduct } from '../api/products'
import { usePageTitle } from '../hooks/usePageTitle'
import type { Product } from '../types/product'
import { ProductImage } from '../components/ProductImage'

function formatPrice(price: Product['price']) {
  const value = typeof price === 'string' ? Number(price) : price
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(value)
}

function errorMessage(error: unknown) {
  if (isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message ?? 'We could not load this product. Please try again.'
  }
  return error instanceof Error ? error.message : 'We could not load this product. Please try again.'
}

export function ProductDetailsPage() {
  const { id } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()
  const productId = Number(id)
  const validProductId = Number.isSafeInteger(productId) && productId > 0
  const [product, setProduct] = useState<Product | null>(null)
  const [loading, setLoading] = useState(validProductId)
  const [notFound, setNotFound] = useState(!validProductId)
  const [error, setError] = useState<string | null>(null)
  const [retry, setRetry] = useState(0)
  const [addingToCart, setAddingToCart] = useState(false)
  const [cartFeedback, setCartFeedback] = useState<string | null>(null)
  const [cartError, setCartError] = useState<string | null>(null)

  usePageTitle(product?.name ?? 'Product details')

  useEffect(() => {
    if (!validProductId) {
      setProduct(null)
      setLoading(false)
      setNotFound(true)
      setError(null)
      return
    }

    let current = true
    setLoading(true)
    setNotFound(false)
    setError(null)

    getProduct(productId)
      .then((loadedProduct) => {
        if (current) setProduct(loadedProduct)
      })
      .catch((requestError: unknown) => {
        if (!current) return
        if (isAxiosError(requestError) && requestError.response?.status === 404) {
          setNotFound(true)
          return
        }
        setError(errorMessage(requestError))
      })
      .finally(() => {
        if (current) setLoading(false)
      })

    return () => {
      current = false
    }
  }, [productId, retry, validProductId])

  if (loading) return <ProductDetailsSkeleton />

  if (notFound) {
    return (
      <PageMessage title="Product not found" description="This product may no longer be active or may not exist." />
    )
  }

  if (error) {
    return (
      <section role="alert" className="mx-auto max-w-3xl rounded-3xl border border-rose-200 bg-rose-50 px-7 py-12 text-center text-rose-950 sm:px-12">
        <h1 className="text-2xl font-semibold">Product unavailable</h1>
        <p className="mx-auto mt-3 max-w-lg leading-7">{error}</p>
        <div className="mt-7 flex flex-wrap justify-center gap-3">
          <button type="button" onClick={() => setRetry((value) => value + 1)} className="rounded-xl bg-rose-800 px-5 py-3 text-sm font-semibold text-white hover:bg-rose-900">Try again</button>
          <Link className="rounded-xl border border-rose-200 bg-white px-5 py-3 text-sm font-semibold text-rose-900 hover:bg-rose-100" to="/products">Back to products</Link>
        </div>
      </section>
    )
  }

  if (!product) return null

  const currentProduct = product
  const inStock = product.stockQuantity > 0

  async function addToCart() {
    if (!inStock || addingToCart) return
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location } })
      return
    }
    setAddingToCart(true)
    setCartFeedback(null)
    setCartError(null)
    try {
      await addCartItem(currentProduct.id, 1)
      setCartFeedback('Added to your cart.')
    } catch (requestError) {
      setCartError(cartErrorMessage(requestError))
    } finally {
      setAddingToCart(false)
    }
  }

  return (
    <div className="space-y-7">
      <Link to="/products" className="inline-flex items-center gap-2 text-sm font-semibold text-brand hover:underline">&larr; Back to products</Link>
      <article className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-lg shadow-slate-200/50">
        <div className="grid lg:grid-cols-[0.9fr_1.1fr]">
          <ProductImage imageUrl={product.imageUrl} category={product.category} name={product.name} className="min-h-72 h-full w-full object-cover" />
          <div className="p-7 sm:p-10">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <p className="text-sm font-semibold text-brand">{product.category}</p>
                <h1 className="mt-2 text-3xl font-semibold tracking-tight text-ink sm:text-4xl">{product.name}</h1>
              </div>
              <span className={`rounded-full px-3 py-1 text-sm font-semibold ${inStock ? 'bg-emerald-50 text-emerald-800' : 'bg-slate-100 text-slate-600'}`}>{inStock ? 'Available' : 'Out of stock'}</span>
            </div>
            <p className="mt-7 whitespace-pre-line text-lg leading-8 text-slate-600">{product.description || 'Product details are available from the seller.'}</p>
            <div className="mt-9 flex flex-col gap-5 border-y border-slate-100 py-6 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <p className="text-sm font-medium text-slate-500">Price</p>
                <p className="mt-1 text-3xl font-semibold tracking-tight text-ink">{formatPrice(product.price)}</p>
              </div>
              <div className="sm:text-right">
                <p className="text-sm font-medium text-slate-500">Availability</p>
                <p className={`mt-1 font-semibold ${inStock ? 'text-emerald-700' : 'text-slate-600'}`}>{inStock ? `${product.stockQuantity} in stock` : 'Currently unavailable'}</p>
              </div>
            </div>
            <div className="mt-6">
              {cartFeedback && <div role="status" className="mb-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-900">{cartFeedback} <Link to="/cart" className="font-semibold underline">View cart</Link></div>}
              {cartError && <div role="alert" className="mb-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-900">{cartError}</div>}
              <button type="button" disabled={!inStock || addingToCart} onClick={() => void addToCart()} className="flex h-11 w-full items-center justify-center rounded-xl bg-brand px-5 text-sm font-semibold text-white shadow-sm hover:bg-[#102f59] disabled:cursor-not-allowed disabled:bg-slate-200 disabled:text-slate-500">{addingToCart ? 'Adding to cart...' : inStock ? (isAuthenticated ? 'Add to cart' : 'Sign in to add to cart') : 'Out of stock'}</button>
            </div>
          </div>
        </div>
      </article>
    </div>
  )
}

function PageMessage({ title, description }: { title: string; description: string }) {
  return (
    <section className="mx-auto max-w-3xl rounded-3xl border border-dashed border-slate-300 bg-white px-7 py-16 text-center sm:px-12">
      <p className="text-sm font-semibold uppercase tracking-[0.18em] text-brand">Catalog</p>
      <h1 className="mt-4 text-3xl font-semibold tracking-tight">{title}</h1>
      <p className="mx-auto mt-3 max-w-lg leading-7 text-slate-600">{description}</p>
      <Link className="mt-7 inline-flex rounded-xl bg-brand px-5 py-3 text-sm font-semibold text-white hover:bg-[#102f59]" to="/products">Browse products</Link>
    </section>
  )
}

function ProductDetailsSkeleton() {
  return (
    <div className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm lg:grid lg:grid-cols-[0.9fr_1.1fr]">
      <div className="min-h-72 animate-pulse bg-slate-200" />
      <div className="space-y-5 p-7 sm:p-10"><div className="h-5 w-24 animate-pulse rounded bg-slate-100" /><div className="h-10 w-3/4 animate-pulse rounded bg-slate-100" /><div className="h-5 w-full animate-pulse rounded bg-slate-100" /><div className="h-5 w-5/6 animate-pulse rounded bg-slate-100" /><div className="mt-10 h-20 animate-pulse rounded bg-slate-100" /></div>
    </div>
  )
}
