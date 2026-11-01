import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { cartErrorMessage, getCart, removeCartItem, updateCartItem } from '../api/cart'
import { usePageTitle } from '../hooks/usePageTitle'
import type { Cart, CartItem } from '../types/cart'
import { useToast } from '../context/ToastContext'

function formatAmount(amount: number | string) {
  const value = typeof amount === 'string' ? Number(amount) : amount
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(value)
}

export function CartPage() {
  const showToast = useToast()
  usePageTitle('Your cart')
  const [cart, setCart] = useState<Cart | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busyItemId, setBusyItemId] = useState<number | null>(null)

  const loadCart = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setCart(await getCart())
    } catch (requestError) {
      setError(cartErrorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadCart()
  }, [loadCart])

  async function changeQuantity(item: CartItem, quantity: number) {
    if (busyItemId !== null || quantity < 1) return
    setBusyItemId(item.id)
    setError(null)
    setNotice(null)
    try {
      await updateCartItem(item.id, quantity)
      await loadCart()
      setNotice('Cart quantity updated.')
      showToast('Cart quantity updated.')
    } catch (requestError) {
      setError(cartErrorMessage(requestError))
    } finally {
      setBusyItemId(null)
    }
  }

  async function removeItem(item: CartItem) {
    if (busyItemId !== null) return
    setBusyItemId(item.id)
    setError(null)
    setNotice(null)
    try {
      await removeCartItem(item.id)
      await loadCart()
      setNotice(`${item.productName} removed from your cart.`)
    } catch (requestError) {
      setError(cartErrorMessage(requestError))
    } finally {
      setBusyItemId(null)
    }
  }

  if (loading && !cart) return <CartSkeleton />

  const items = cart?.items ?? []
  return (
    <div className="space-y-7">
      <section className="flex flex-col gap-3 rounded-3xl bg-ink px-6 py-9 text-white shadow-xl shadow-slate-300/40 sm:flex-row sm:items-end sm:justify-between sm:px-10">
        <div><p className="text-sm font-semibold uppercase tracking-[0.2em] text-sky-200">Customer cart</p><h1 className="mt-2 text-3xl font-semibold tracking-tight sm:text-4xl">Your selected products</h1></div>
        {!loading && <span className="rounded-full bg-white/10 px-4 py-2 text-sm font-medium text-sky-100">{items.length} {items.length === 1 ? 'item' : 'items'}</span>}
      </section>

      {notice && <div role="status" className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-900">{notice}</div>}
      {error && <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm leading-6 text-rose-900">{error}<button type="button" onClick={() => void loadCart()} className="ml-3 font-semibold underline">Try again</button></div>}

      {!loading && !error && items.length === 0 && (
        <section className="rounded-3xl border border-dashed border-slate-300 bg-white px-6 py-16 text-center">
          <h2 className="text-2xl font-semibold">Your cart is empty</h2>
          <p className="mx-auto mt-3 max-w-lg leading-7 text-slate-600">Explore the catalog and add products when you are ready.</p>
          <Link to="/products" className="mt-7 inline-flex rounded-xl bg-brand px-5 py-3 text-sm font-semibold text-white hover:bg-[#102f59]">Browse products</Link>
        </section>
      )}

      {!loading && items.length > 0 && cart && (
        <div className="grid gap-6 lg:grid-cols-[1fr_21rem] lg:items-start">
          <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="divide-y divide-slate-100">
              {items.map((item) => <CartItemRow key={item.id} item={item} busy={busyItemId !== null} onQuantityChange={changeQuantity} onRemove={removeItem} />)}
            </div>
          </section>
          <aside className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm lg:sticky lg:top-6">
            <h2 className="text-lg font-semibold">Order summary</h2>
            <div className="mt-5 flex items-center justify-between border-y border-slate-100 py-5"><span className="text-slate-600">Cart subtotal</span><span className="text-xl font-semibold">{formatAmount(cart.subtotal)}</span></div>
            <p className="mt-4 text-sm leading-6 text-slate-500">Final availability and pricing are confirmed during checkout.</p>
            <Link to="/checkout" className="mt-6 flex h-11 w-full items-center justify-center rounded-xl bg-brand px-5 text-sm font-semibold text-white shadow-sm hover:bg-[#102f59]">Checkout</Link>
            <Link to="/products" className="mt-4 block text-center text-sm font-semibold text-brand hover:underline">Continue shopping</Link>
          </aside>
        </div>
      )}
    </div>
  )
}

function CartItemRow({ item, busy, onQuantityChange, onRemove }: { item: CartItem; busy: boolean; onQuantityChange: (item: CartItem, quantity: number) => Promise<void>; onRemove: (item: CartItem) => Promise<void> }) {
  const isBusy = busy
  return (
    <article className="flex flex-col gap-5 p-5 sm:flex-row sm:items-center sm:justify-between sm:p-6">
      <div className="min-w-0 flex-1"><Link to={`/products/${item.productId}`} className="text-lg font-semibold text-ink hover:text-brand">{item.productName}</Link><p className="mt-1 text-sm text-slate-500">{formatAmount(item.price)} each</p></div>
      <div className="flex flex-wrap items-center gap-5 sm:justify-end">
        <div className="flex items-center rounded-xl border border-slate-300"><button type="button" aria-label={`Decrease ${item.productName} quantity`} disabled={isBusy || item.quantity <= 1} onClick={() => void onQuantityChange(item, item.quantity - 1)} className="grid size-10 place-items-center text-lg font-semibold text-slate-600 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40">-</button><span className="grid h-10 min-w-10 place-items-center border-x border-slate-300 text-sm font-semibold">{item.quantity}</span><button type="button" aria-label={`Increase ${item.productName} quantity`} disabled={isBusy} onClick={() => void onQuantityChange(item, item.quantity + 1)} className="grid size-10 place-items-center text-lg font-semibold text-slate-600 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40">+</button></div>
        <div className="min-w-24 text-right"><p className="font-semibold">{formatAmount(item.lineTotal)}</p><button type="button" disabled={isBusy} onClick={() => void onRemove(item)} className="mt-1 text-sm font-semibold text-rose-700 hover:underline disabled:cursor-not-allowed disabled:opacity-40">Remove</button></div>
      </div>
    </article>
  )
}

function CartSkeleton() {
  return <div className="space-y-7"><div className="h-36 animate-pulse rounded-3xl bg-slate-200" /><div className="grid gap-6 lg:grid-cols-[1fr_21rem]"><div className="space-y-px overflow-hidden rounded-2xl border border-slate-200 bg-white">{Array.from({ length: 3 }, (_, index) => <div key={index} className="h-28 animate-pulse border-b border-slate-100 bg-slate-50" />)}</div><div className="h-64 animate-pulse rounded-2xl bg-white" /></div></div>
}
