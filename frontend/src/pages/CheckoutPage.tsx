import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { cartErrorMessage, checkoutCart, getCart } from '../api/cart'
import { usePageTitle } from '../hooks/usePageTitle'
import type { Cart, CheckoutOrder } from '../types/cart'

function formatAmount(amount: number | string, currency = 'INR') {
  const value = typeof amount === 'string' ? Number(amount) : amount
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency }).format(value)
}

export function CheckoutPage() {
  usePageTitle('Checkout')
  const [cart, setCart] = useState<Cart | null>(null)
  const [order, setOrder] = useState<CheckoutOrder | null>(null)
  const [loading, setLoading] = useState(true)
  const [placingOrder, setPlacingOrder] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [details, setDetails] = useState({ name: '', phone: '', address: '', city: '', pincode: '', paymentMethod: 'COD' as 'COD' | 'DEMO_CARD' })

  const loadCart = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setCart(await getCart())
    } catch (requestError) {
      setError(cartErrorMessage(requestError, 'We could not load checkout. Please try again.'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadCart()
  }, [loadCart])

  async function placeOrder() {
    if (!cart || cart.items.length === 0 || placingOrder) return
    setPlacingOrder(true)
    setError(null)
    try {
      const completedOrder = await checkoutCart(details)
      setOrder(completedOrder)
      setCart((currentCart) => currentCart ? { ...currentCart, items: [], subtotal: 0 } : currentCart)
    } catch (requestError) {
      setError(cartErrorMessage(requestError, 'We could not complete checkout. Please try again.'))
    } finally {
      setPlacingOrder(false)
    }
  }

  if (loading) return <CheckoutSkeleton />

  if (order) {
    return (
      <div className="mx-auto max-w-3xl space-y-7">
        <section className="rounded-3xl bg-ink px-7 py-12 text-center text-white shadow-xl shadow-slate-300/40 sm:px-12">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-sky-200">Order confirmed</p>
          <h1 className="mt-4 text-3xl font-semibold tracking-tight sm:text-4xl">Thank you for your order.</h1>
          <p className="mt-4 text-slate-300">Order #{order.orderId} has been placed successfully.</p>
        </section>
        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
          <div className="flex flex-wrap items-start justify-between gap-4"><div><p className="text-sm font-semibold text-brand">Order #{order.orderId}</p><h2 className="mt-1 text-2xl font-semibold">{order.status}</h2></div><p className="text-xl font-semibold">{formatAmount(order.totalAmount, order.currency)}</p></div>
          <div className="mt-7 divide-y divide-slate-100 border-y border-slate-100">{order.items.map((item) => <div key={item.productId} className="flex items-center justify-between gap-4 py-4"><div><p className="font-semibold">{item.productName}</p><p className="mt-1 text-sm text-slate-500">{item.quantity} × {formatAmount(item.unitPrice, order.currency)}</p></div><p className="font-semibold">{formatAmount(item.lineTotal, order.currency)}</p></div>)}</div>
          <p className="mt-5 text-sm text-slate-500">Your cart has been cleared. {order.createdAt ? `Placed ${new Date(order.createdAt).toLocaleString()}.` : ''}</p>
          <div className="mt-7 flex flex-wrap gap-3"><Link to={`/orders/${order.orderId}`} className="rounded-xl bg-brand px-5 py-3 text-sm font-semibold text-white hover:bg-[#102f59]">View order details</Link><Link to="/products" className="rounded-xl border border-slate-300 px-5 py-3 text-sm font-semibold text-brand hover:bg-slate-50">Continue shopping</Link></div>
        </section>
      </div>
    )
  }

  const items = cart?.items ?? []
  if (!error && items.length === 0) {
    return <EmptyCheckout />
  }

  return (
    <div className="mx-auto max-w-5xl space-y-7">
      <section className="rounded-3xl bg-ink px-6 py-9 text-white shadow-xl shadow-slate-300/40 sm:px-10"><p className="text-sm font-semibold uppercase tracking-[0.2em] text-sky-200">Checkout</p><h1 className="mt-2 text-3xl font-semibold tracking-tight sm:text-4xl">Review your order</h1><p className="mt-3 max-w-2xl leading-7 text-slate-300">Confirm your items before placing your order. Final availability and catalog prices are checked securely when you place it.</p></section>
      {error && <section role="alert" className="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-rose-950"><h2 className="font-semibold">Checkout unavailable</h2><p className="mt-1 text-sm leading-6">{error}</p><button type="button" onClick={() => void loadCart()} className="mt-4 rounded-lg bg-white px-4 py-2 text-sm font-semibold text-rose-800 shadow-sm ring-1 ring-rose-200 hover:bg-rose-100">Return to cart review</button></section>}
      {!error && cart && <div className="grid gap-6 lg:grid-cols-[1fr_21rem] lg:items-start"><section className="space-y-6"><div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"><div className="border-b border-slate-100 px-6 py-5"><h2 className="text-lg font-semibold">Items</h2></div><div className="divide-y divide-slate-100">{items.map((item) => <article key={item.id} className="flex items-center justify-between gap-4 p-6"><div><p className="font-semibold">{item.productName}</p><p className="mt-1 text-sm text-slate-500">{item.quantity} × {formatAmount(item.price)}</p></div><p className="font-semibold">{formatAmount(item.lineTotal)}</p></article>)}</div></div><div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm"><h2 className="text-lg font-semibold">Delivery details</h2><div className="mt-5 grid gap-3 sm:grid-cols-2">{(['name','phone','address','city','pincode'] as const).map((field) => <label key={field} className="grid gap-1 text-sm font-semibold text-slate-700 sm:last:col-span-2"><span className="capitalize">{field}</span><input required value={details[field]} maxLength={field === 'pincode' ? 6 : 120} onChange={(event) => setDetails((current) => ({ ...current, [field]: event.target.value }))} className="h-10 rounded-lg border border-slate-300 px-3 font-normal" /></label>)}</div><fieldset className="mt-5 grid gap-2"><legend className="font-semibold">Payment</legend>{(['COD','DEMO_CARD'] as const).map((method) => <label key={method} className="flex items-center gap-2 text-sm"><input type="radio" checked={details.paymentMethod === method} onChange={() => setDetails((current) => ({ ...current, paymentMethod: method }))} />{method === 'COD' ? 'Cash on delivery' : 'Demo card'}</label>)}</fieldset></div></section><aside className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm lg:sticky lg:top-6"><h2 className="text-lg font-semibold">Order total</h2><div className="mt-5 flex items-center justify-between border-y border-slate-100 py-5"><span className="text-slate-600">Total</span><span className="text-xl font-semibold">{formatAmount(cart.subtotal)}</span></div><button type="button" onClick={() => void placeOrder()} disabled={placingOrder || details.pincode.length !== 6 || !details.name || !details.phone || !details.address || !details.city} className="mt-6 flex h-11 w-full items-center justify-center rounded-xl bg-brand px-5 text-sm font-semibold text-white shadow-sm hover:bg-[#102f59] disabled:cursor-not-allowed disabled:opacity-60">{placingOrder ? 'Placing order...' : 'Place order'}</button><Link to="/cart" className="mt-4 block text-center text-sm font-semibold text-brand hover:underline">Back to cart</Link></aside></div>}
    </div>
  )
}

function EmptyCheckout() {
  return <section className="mx-auto max-w-3xl rounded-3xl border border-dashed border-slate-300 bg-white px-6 py-16 text-center"><p className="text-sm font-semibold uppercase tracking-[0.18em] text-brand">Checkout</p><h1 className="mt-4 text-3xl font-semibold tracking-tight">Your cart is empty</h1><p className="mx-auto mt-3 max-w-lg leading-7 text-slate-600">Add products to your cart before placing an order.</p><Link to="/products" className="mt-7 inline-flex rounded-xl bg-brand px-5 py-3 text-sm font-semibold text-white hover:bg-[#102f59]">Browse products</Link></section>
}

function CheckoutSkeleton() {
  return <div className="mx-auto max-w-5xl space-y-7"><div className="h-40 animate-pulse rounded-3xl bg-slate-200" /><div className="grid gap-6 lg:grid-cols-[1fr_21rem]"><div className="h-80 animate-pulse rounded-2xl bg-white" /><div className="h-64 animate-pulse rounded-2xl bg-white" /></div></div>
}
