import { isAxiosError } from 'axios'
import { useCallback, useEffect, useState } from 'react'
import { getAdminOrderStatistics, getAdminOrders, getAdminProducts, getAdminUsers, updateAdminProductStatus, updateAdminUserStatus } from '../api/admin'
import { getOrderStatuses, orderErrorMessage } from '../api/orders'
import { usePageTitle } from '../hooks/usePageTitle'
import type { AdminProduct, AdminProductPage, AdminUserPage } from '../types/admin'
import type { RegisteredUser } from '../types/auth'
import type { AdminOrderStatistics } from '../types/order'
import type { Order, OrderPage, OrderStatuses } from '../types/order'
import { useToast } from '../context/ToastContext'

const PAGE_SIZE = 20

function errorMessage(error: unknown, fallback: string) {
  if (isAxiosError<{ message?: string }>(error)) return error.response?.data?.message ?? fallback
  return error instanceof Error ? error.message : fallback
}

export function AdminDashboardPage() {
  usePageTitle('Admin dashboard')
  const showToast = useToast()
  const [users, setUsers] = useState<AdminUserPage | null>(null)
  const [products, setProducts] = useState<AdminProductPage | null>(null)
  const [userPage, setUserPage] = useState(0)
  const [productPage, setProductPage] = useState(0)
  const [loadingUsers, setLoadingUsers] = useState(true)
  const [loadingProducts, setLoadingProducts] = useState(true)
  const [userError, setUserError] = useState<string | null>(null)
  const [productError, setProductError] = useState<string | null>(null)
  const [updatingUserId, setUpdatingUserId] = useState<number | null>(null)
  const [updatingProductId, setUpdatingProductId] = useState<number | null>(null)
  const [orders, setOrders] = useState<OrderPage | null>(null)
  const [statistics, setStatistics] = useState<AdminOrderStatistics | null>(null)
  const [statuses, setStatuses] = useState<OrderStatuses | null>(null)
  const [orderPage, setOrderPage] = useState(0)
  const [loadingOrders, setLoadingOrders] = useState(true)
  const [loadingOrderStatistics, setLoadingOrderStatistics] = useState(true)
  const [orderError, setOrderError] = useState<string | null>(null)

  const loadUsers = useCallback(async () => {
    setLoadingUsers(true)
    setUserError(null)
    try {
      setUsers(await getAdminUsers(userPage, PAGE_SIZE))
    } catch (requestError) {
      setUserError(errorMessage(requestError, 'We could not load users. Please try again.'))
    } finally {
      setLoadingUsers(false)
    }
  }, [userPage])

  const loadProducts = useCallback(async () => {
    setLoadingProducts(true)
    setProductError(null)
    try {
      setProducts(await getAdminProducts(productPage, PAGE_SIZE))
    } catch (requestError) {
      setProductError(errorMessage(requestError, 'We could not load products. Please try again.'))
    } finally {
      setLoadingProducts(false)
    }
  }, [productPage])

  useEffect(() => { void loadUsers() }, [loadUsers])
  useEffect(() => { void loadProducts() }, [loadProducts])

  const loadOrders = useCallback(async () => {
    setLoadingOrders(true)
    setOrderError(null)
    try {
      setOrders(await getAdminOrders(orderPage, PAGE_SIZE))
    } catch (requestError) {
      setOrderError(orderErrorMessage(requestError, 'We could not load marketplace orders.'))
    } finally {
      setLoadingOrders(false)
    }
  }, [orderPage])

  const loadOrderStatistics = useCallback(async () => {
    setLoadingOrderStatistics(true)
    try {
      const [statisticsResult, statusesResult] = await Promise.all([getAdminOrderStatistics(), getOrderStatuses()])
      setStatistics(statisticsResult)
      setStatuses(statusesResult)
      setOrderError(null)
    } catch (requestError) {
      setOrderError(orderErrorMessage(requestError, 'We could not load marketplace order statistics.'))
    } finally {
      setLoadingOrderStatistics(false)
    }
  }, [])

  useEffect(() => { void loadOrders() }, [loadOrders])
  useEffect(() => { void loadOrderStatistics() }, [loadOrderStatistics])

  async function updateUser(user: RegisteredUser) {
    const active = !user.active
    if (!window.confirm(`${active ? 'Activate' : 'Deactivate'} ${user.email}?`)) return
    setUpdatingUserId(user.id)
    setUserError(null)
    try {
      await updateAdminUserStatus(user.id, active)
      await loadUsers()
      showToast(active ? 'User activated.' : 'User deactivated.')
    } catch (requestError) {
      setUserError(errorMessage(requestError, 'We could not update this user. Please try again.'))
    } finally {
      setUpdatingUserId(null)
    }
  }

  async function updateProduct(product: AdminProduct) {
    const active = !product.active
    if (!window.confirm(`${active ? 'Activate' : 'Deactivate'} ${product.name}?`)) return
    setUpdatingProductId(product.id)
    setProductError(null)
    try {
      await updateAdminProductStatus(product.id, active)
      await loadProducts()
      showToast(active ? 'Product activated.' : 'Product deactivated.')
    } catch (requestError) {
      setProductError(errorMessage(requestError, 'We could not update this product. Please try again.'))
    } finally {
      setUpdatingProductId(null)
    }
  }

  return <div className="space-y-8"><section className="rounded-3xl bg-ink px-6 py-10 text-white shadow-xl shadow-slate-300/40 sm:px-10 sm:py-12"><p className="text-sm font-semibold uppercase tracking-[0.2em] text-sky-200">Administration</p><h1 className="mt-3 text-3xl font-semibold tracking-tight sm:text-4xl">Marketplace dashboard</h1><p className="mt-3 max-w-2xl leading-7 text-slate-300">Review marketplace users, products, and orders, including inactive records.</p></section>
    <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm"><div className="flex flex-wrap items-center justify-between gap-4"><div><h2 className="text-xl font-semibold">Order overview</h2><p className="mt-1 text-sm text-slate-600">Marketplace-wide sales and order volume.</p></div><div className="flex flex-wrap gap-2">{statuses?.statuses.map((status) => <span key={status.value} className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">{status.label}</span>)}</div></div>{loadingOrderStatistics ? <div className="mt-5 h-24 animate-pulse rounded-xl bg-slate-50" /> : statistics ? <div className="mt-5 grid gap-4 sm:grid-cols-2 xl:grid-cols-5"><AdminStatistic label="Orders" value={statistics.totalOrders} /><AdminStatistic label="Placed" value={statistics.placedOrders} /><AdminStatistic label="Fulfilled" value={statistics.fulfilledOrders} /><AdminStatistic label="Units" value={statistics.totalUnits} /><AdminStatistic label="Sales" value={adminMoneyLabel(statistics.totalSales)} /></div> : <button type="button" onClick={() => void loadOrderStatistics()} className="mt-4 text-sm font-semibold text-brand">Retry summary</button>}</section>
    <DashboardSection title="Orders" page={orders} loading={loadingOrders} error={orderError} empty="No orders found." onRetry={loadOrders} onPrevious={() => setOrderPage((current) => current - 1)} onNext={() => setOrderPage((current) => current + 1)}>{orders?.items.map((order) => <AdminOrder key={order.orderId} order={order} statuses={statuses?.statuses ?? []} />)}</DashboardSection>
    <DashboardSection title="Users" page={users} loading={loadingUsers} error={userError} empty="No users found." onRetry={loadUsers} onPrevious={() => setUserPage((current) => current - 1)} onNext={() => setUserPage((current) => current + 1)}>{users?.items.map((user) => <article key={user.id} className="flex flex-wrap items-center justify-between gap-4 p-5"><div><p className="font-semibold">{user.email}</p><p className="mt-1 text-sm text-slate-500">{user.role}</p></div><div className="flex items-center gap-4"><Status active={user.active} /><button type="button" disabled={updatingUserId === user.id} onClick={() => void updateUser(user)} className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-semibold text-brand hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60">{updatingUserId === user.id ? 'Updating...' : user.active ? 'Deactivate' : 'Activate'}</button></div></article>)}</DashboardSection><DashboardSection title="Products" page={products} loading={loadingProducts} error={productError} empty="No products found." onRetry={loadProducts} onPrevious={() => setProductPage((current) => current - 1)} onNext={() => setProductPage((current) => current + 1)}>{products?.items.map((product) => <article key={product.id} className="flex flex-wrap items-center justify-between gap-4 p-5"><div><p className="font-semibold">{product.name}</p><p className="mt-1 text-sm text-slate-500">{product.category} · Vendor: {product.vendorEmail ?? 'Marketplace'}</p></div><div className="flex items-center gap-4"><Status active={product.active} /><button type="button" disabled={updatingProductId === product.id} onClick={() => void updateProduct(product)} className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-semibold text-brand hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60">{updatingProductId === product.id ? 'Updating...' : product.active ? 'Deactivate' : 'Activate'}</button></div></article>)}</DashboardSection></div>
}

function AdminStatistic({ label, value }: { label: string; value: number | string }) {
  return <div className="rounded-xl bg-slate-50 px-4 py-5"><p className="text-sm text-slate-500">{label}</p><p className="mt-1 text-2xl font-semibold text-ink">{value}</p></div>
}

function AdminOrder({ order, statuses }: { order: Order; statuses: { value: string; label: string }[] }) {
  const statusLabel = statuses.find((status) => status.value === order.status)?.label ?? order.status
  return <article className="p-5"><div className="flex flex-wrap items-start justify-between gap-3"><div><p className="font-semibold">Order #{order.orderId}</p>{order.customerEmail && <p className="mt-1 text-sm font-medium text-slate-600">{order.customerEmail}</p>}<p className="mt-1 text-sm text-slate-500">{new Date(order.createdAt).toLocaleString()}</p></div><span className="rounded-full bg-sky-50 px-3 py-1 text-xs font-semibold text-sky-700">{statusLabel}</span></div><div className="mt-4 flex flex-wrap justify-between gap-4 text-sm"><span>{order.items.length} item{order.items.length === 1 ? '' : 's'}</span><span className="font-semibold text-slate-700">{adminMoneyLabel(order.totalAmount)}</span></div></article>
}

function adminMoneyLabel(value: number | string) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(Number(value))
}

function DashboardSection<T extends { items: unknown[]; page: number; totalElements: number; totalPages: number }>({ title, page, loading, error, empty, onRetry, onPrevious, onNext, children }: { title: string; page: T | null; loading: boolean; error: string | null; empty: string; onRetry: () => Promise<void>; onPrevious: () => void; onNext: () => void; children: React.ReactNode }) {
  return <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"><div className="flex items-center justify-between border-b border-slate-100 px-6 py-5"><h2 className="text-xl font-semibold">{title}</h2>{!loading && page && <span className="text-sm text-slate-500">{page.totalElements} total</span>}</div>{loading && <div className="h-36 animate-pulse bg-slate-50" />}{!loading && error && <div role="alert" className="p-6 text-rose-950"><p className="text-sm">{error}</p><button type="button" onClick={() => void onRetry()} className="mt-4 text-sm font-semibold text-brand hover:underline">Try again</button></div>}{!loading && !error && page?.items.length === 0 && <p className="p-6 text-sm text-slate-600">{empty}</p>}{!loading && !error && page && page.items.length > 0 && <><div className="divide-y divide-slate-100">{children}</div>{page.totalPages > 1 && <nav aria-label={`${title} pagination`} className="flex items-center justify-between border-t border-slate-100 px-6 py-4"><button type="button" disabled={page.page === 0} onClick={onPrevious} className="text-sm font-semibold text-brand disabled:opacity-50">Previous</button><span className="text-sm text-slate-600">Page {page.page + 1} of {page.totalPages}</span><button type="button" disabled={page.page >= page.totalPages - 1} onClick={onNext} className="text-sm font-semibold text-brand disabled:opacity-50">Next</button></nav>}</>}</section>
}

function Status({ active }: { active: boolean }) {
  return <span className={`rounded-full px-3 py-1 text-xs font-semibold ${active ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'}`}>{active ? 'Active' : 'Inactive'}</span>
}
