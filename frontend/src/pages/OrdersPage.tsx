import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getOrders, orderErrorMessage } from '../api/orders'
import { usePageTitle } from '../hooks/usePageTitle'
import type { OrderPage } from '../types/order'

const PAGE_SIZE = 20

function formatAmount(amount: number | string, currency: string) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency }).format(Number(amount))
}

function formatDate(date: string) {
  return new Date(date).toLocaleString()
}

export function OrdersPage() {
  usePageTitle('Your orders')
  const [orders, setOrders] = useState<OrderPage | null>(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadOrders = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setOrders(await getOrders(page, PAGE_SIZE))
    } catch (requestError) {
      setError(orderErrorMessage(requestError, 'We could not load your orders. Please try again.'))
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => {
    void loadOrders()
  }, [loadOrders])

  if (loading) return <OrdersSkeleton />

  return (
    <div className="mx-auto max-w-5xl space-y-7">
      <section className="rounded-3xl bg-ink px-6 py-9 text-white shadow-xl shadow-slate-300/40 sm:px-10"><p className="text-sm font-semibold uppercase tracking-[0.2em] text-sky-200">Your account</p><h1 className="mt-2 text-3xl font-semibold tracking-tight sm:text-4xl">Order history</h1></section>
      {error && <section role="alert" className="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-rose-950"><h2 className="font-semibold">Orders unavailable</h2><p className="mt-1 text-sm leading-6">{error}</p><button type="button" onClick={() => void loadOrders()} className="mt-4 rounded-lg bg-white px-4 py-2 text-sm font-semibold text-rose-800 shadow-sm ring-1 ring-rose-200 hover:bg-rose-100">Try again</button></section>}
      {!error && orders?.items.length === 0 && <section className="rounded-3xl border border-dashed border-slate-300 bg-white px-6 py-16 text-center"><h2 className="text-2xl font-semibold">No orders yet</h2><p className="mt-3 text-slate-600">When you place an order, it will appear here.</p><Link to="/products" className="mt-7 inline-flex rounded-xl bg-brand px-5 py-3 text-sm font-semibold text-white hover:bg-[#102f59]">Browse products</Link></section>}
      {!error && orders && orders.items.length > 0 && <>
        <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"><div className="divide-y divide-slate-100">{orders.items.map((order) => <Link key={order.orderId} to={`/orders/${order.orderId}`} className="block p-6 hover:bg-slate-50"><div className="flex flex-wrap items-start justify-between gap-4"><div><p className="font-semibold text-brand">Order #{order.orderId}</p><p className="mt-1 text-sm text-slate-500">{formatDate(order.createdAt)}</p><p className="mt-3 text-sm text-slate-600">{order.items.length} {order.items.length === 1 ? 'item' : 'items'}: {order.items.slice(0, 2).map((item) => item.productName).join(', ')}{order.items.length > 2 ? '…' : ''}</p></div><div className="text-right"><p className="text-sm font-semibold text-slate-600">{order.status}</p><p className="mt-2 text-lg font-semibold">{formatAmount(order.totalAmount, order.currency)}</p></div></div></Link>)}</div></section>
        {orders.totalPages > 1 && <nav aria-label="Order pages" className="flex items-center justify-between"><button type="button" disabled={page === 0} onClick={() => setPage((currentPage) => currentPage - 1)} className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-semibold text-brand hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50">Previous</button><p className="text-sm text-slate-600">Page {orders.page + 1} of {orders.totalPages}</p><button type="button" disabled={page + 1 >= orders.totalPages} onClick={() => setPage((currentPage) => currentPage + 1)} className="rounded-xl border border-slate-300 px-4 py-2 text-sm font-semibold text-brand hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50">Next</button></nav>}
      </>}
    </div>
  )
}

function OrdersSkeleton() {
  return <div className="mx-auto max-w-5xl space-y-7"><div className="h-36 animate-pulse rounded-3xl bg-slate-200" /><div className="h-80 animate-pulse rounded-2xl bg-white" /></div>
}
