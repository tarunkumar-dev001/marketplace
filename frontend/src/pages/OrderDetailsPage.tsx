import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { cancelOrder, getOrder, orderErrorMessage } from '../api/orders'
import { usePageTitle } from '../hooks/usePageTitle'
import type { Order } from '../types/order'

function formatAmount(amount: number | string, currency: string) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency }).format(Number(amount))
}

export function OrderDetailsPage() {
  const { id } = useParams()
  usePageTitle(id ? `Order #${id}` : 'Order details')
  const [order, setOrder] = useState<Order | null>(null)
  const [loading, setLoading] = useState(true)
  const [cancelling, setCancelling] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const loadOrder = useCallback(async () => {
    if (!id) return
    setLoading(true)
    setError(null)
    try {
      setOrder(await getOrder(id))
    } catch (requestError) {
      setError(orderErrorMessage(requestError, 'We could not load this order. Please try again.'))
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => {
    void loadOrder()
  }, [loadOrder])

  async function handleCancel() {
    if (!order || cancelling || !window.confirm('Cancel this order? This action cannot be undone.')) return
    setCancelling(true)
    setError(null)
    try {
      await cancelOrder(order.orderId)
      await loadOrder()
    } catch (requestError) {
      setError(orderErrorMessage(requestError, 'We could not cancel this order. Please try again.'))
    } finally {
      setCancelling(false)
    }
  }

  if (loading) return <div className="mx-auto h-96 max-w-5xl animate-pulse rounded-2xl bg-white" />

  if (!order) return <section className="mx-auto max-w-3xl rounded-2xl border border-rose-200 bg-rose-50 p-6 text-rose-950"><h1 className="text-xl font-semibold">Order unavailable</h1><p className="mt-2 text-sm">{error ?? 'This order could not be found.'}</p><Link to="/orders" className="mt-5 inline-block text-sm font-semibold text-brand hover:underline">Back to order history</Link></section>

  return (
    <div className="mx-auto max-w-5xl space-y-7">
      <Link to="/orders" className="inline-block text-sm font-semibold text-brand hover:underline">← Back to order history</Link>
      <section className="rounded-3xl bg-ink px-6 py-9 text-white shadow-xl shadow-slate-300/40 sm:px-10"><div className="flex flex-wrap items-start justify-between gap-4"><div><p className="text-sm font-semibold uppercase tracking-[0.2em] text-sky-200">Order #{order.orderId}</p><h1 className="mt-2 text-3xl font-semibold tracking-tight sm:text-4xl">{order.status}</h1><p className="mt-3 text-slate-300">Placed {new Date(order.createdAt).toLocaleString()}</p></div><p className="text-2xl font-semibold">{formatAmount(order.totalAmount, order.currency)}</p></div></section>
      {error && <section role="alert" className="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-rose-950"><h2 className="font-semibold">Action unavailable</h2><p className="mt-1 text-sm leading-6">{error}</p></section>}
      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"><div className="border-b border-slate-100 px-6 py-5"><h2 className="text-lg font-semibold">Items</h2></div><div className="divide-y divide-slate-100">{order.items.map((item) => <article key={`${item.productId}-${item.productName}`} className="flex items-center justify-between gap-4 p-6"><div><p className="font-semibold">{item.productName}</p><p className="mt-1 text-sm text-slate-500">{item.quantity} × {formatAmount(item.unitPrice, order.currency)}</p></div><p className="font-semibold">{formatAmount(item.lineTotal, order.currency)}</p></article>)}</div><div className="flex justify-between border-t border-slate-100 px-6 py-5 text-lg font-semibold"><span>Total</span><span>{formatAmount(order.totalAmount, order.currency)}</span></div></section>
      {order.status === 'PLACED' && <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm"><h2 className="text-lg font-semibold">Need to cancel?</h2><p className="mt-1 text-sm text-slate-600">You can cancel this order while it is still placed.</p><button type="button" onClick={() => void handleCancel()} disabled={cancelling} className="mt-4 rounded-xl border border-rose-300 px-5 py-3 text-sm font-semibold text-rose-700 hover:bg-rose-50 disabled:cursor-not-allowed disabled:opacity-60">{cancelling ? 'Cancelling order...' : 'Cancel order'}</button></section>}
    </div>
  )
}
