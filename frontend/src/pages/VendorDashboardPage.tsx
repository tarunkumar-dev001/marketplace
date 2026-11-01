import { isAxiosError } from 'axios'
import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { createProduct, deleteProduct, getMyProducts, updateProduct } from '../api/products'
import { getOrderStatuses, getVendorOrders, getVendorOrderStatistics, orderErrorMessage } from '../api/orders'
import { usePageTitle } from '../hooks/usePageTitle'
import type { Product, ProductPage, ProductRequest } from '../types/product'
import type { Order, OrderPage, OrderStatuses, OrderStatusOption, VendorOrderStatistics } from '../types/order'

const PAGE_SIZE = 20

function priceLabel(price: Product['price']) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(Number(price))
}

function productErrorMessage(error: unknown, fallback = 'We could not complete that product action. Please try again.') {
  if (isAxiosError<{ message?: string }>(error)) return error.response?.data?.message ?? fallback
  return error instanceof Error ? error.message : fallback
}

export function VendorDashboardPage() {
  usePageTitle('Vendor dashboard')
  const [products, setProducts] = useState<ProductPage | null>(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [editingProduct, setEditingProduct] = useState<Product | null | undefined>(undefined)
  const [saving, setSaving] = useState(false)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [orders, setOrders] = useState<OrderPage | null>(null)
  const [statistics, setStatistics] = useState<VendorOrderStatistics | null>(null)
  const [statuses, setStatuses] = useState<OrderStatuses | null>(null)
  const [orderPage, setOrderPage] = useState(0)
  const [loadingOrders, setLoadingOrders] = useState(true)
  const [loadingStatistics, setLoadingStatistics] = useState(true)
  const [orderError, setOrderError] = useState<string | null>(null)

  const loadProducts = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setProducts(await getMyProducts(page, PAGE_SIZE))
    } catch (requestError) {
      setError(productErrorMessage(requestError, 'We could not load your products. Please try again.'))
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => {
    void loadProducts()
  }, [loadProducts])

  const loadOrders = useCallback(async () => {
    setLoadingOrders(true)
    setOrderError(null)
    try {
      setOrders(await getVendorOrders(orderPage, PAGE_SIZE))
    } catch (requestError) {
      setOrderError(orderErrorMessage(requestError, 'We could not load your orders.'))
    } finally {
      setLoadingOrders(false)
    }
  }, [orderPage])

  const loadStatistics = useCallback(async () => {
    setLoadingStatistics(true)
    try {
      const [statisticsResult, statusesResult] = await Promise.all([getVendorOrderStatistics(), getOrderStatuses()])
      setStatistics(statisticsResult)
      setStatuses(statusesResult)
      setOrderError(null)
    } catch (requestError) {
      setOrderError(orderErrorMessage(requestError, 'We could not load your order summary.'))
    } finally {
      setLoadingStatistics(false)
    }
  }, [])

  useEffect(() => { void loadOrders() }, [loadOrders])
  useEffect(() => { void loadStatistics() }, [loadStatistics])

  async function saveProduct(request: ProductRequest) {
    setSaving(true)
    setActionError(null)
    setNotice(null)
    try {
      if (editingProduct) {
        await updateProduct(editingProduct.id, request)
        setNotice('Product updated successfully.')
      } else {
        await createProduct(request)
        setNotice('Product created successfully.')
      }
      setEditingProduct(undefined)
      if (page === 0) await loadProducts()
      else setPage(0)
    } catch (requestError) {
      setActionError(productErrorMessage(requestError))
    } finally {
      setSaving(false)
    }
  }

  async function removeProduct(product: Product) {
    if (deletingId || !window.confirm(`Delete ${product.name}? This action cannot be undone.`)) return
    setDeletingId(product.id)
    setActionError(null)
    setNotice(null)
    try {
      await deleteProduct(product.id)
      setNotice('Product deleted successfully.')
      await loadProducts()
    } catch (requestError) {
      setActionError(productErrorMessage(requestError, 'We could not delete this product. Please try again.'))
    } finally {
      setDeletingId(null)
    }
  }

  const currentPage = products?.page ?? 0
  const totalPages = products?.totalPages ?? 0

  return (
    <div className="space-y-8">
      <section className="overflow-hidden rounded-3xl bg-ink px-6 py-10 text-white shadow-xl shadow-slate-300/40 sm:px-10 sm:py-12"><p className="text-sm font-semibold uppercase tracking-[0.2em] text-sky-200">Vendor portal</p><div className="mt-3 flex flex-wrap items-end justify-between gap-4"><div><h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">Your products</h1><p className="mt-3 max-w-2xl leading-7 text-slate-300">Review every product you manage, including inactive listings.</p></div><button type="button" onClick={() => { setActionError(null); setNotice(null); setEditingProduct(null) }} className="rounded-xl bg-white px-5 py-3 text-sm font-semibold text-brand hover:bg-sky-50">Add product</button></div></section>
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div><h2 className="text-xl font-semibold">Order overview</h2><p className="mt-1 text-sm text-slate-600">Sales and orders for your marketplace items.</p></div>
          <div className="flex flex-wrap gap-2">{statuses?.statuses.map((status) => <span key={status.value} className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">{status.label}</span>)}</div>
        </div>
        {loadingStatistics ? <div className="mt-5 h-24 animate-pulse rounded-xl bg-slate-50" /> : statistics ? <div className="mt-5 grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
          <Statistic label="Orders" value={statistics.totalOrders} />
          <Statistic label="Placed" value={statistics.placedOrders} />
          <Statistic label="Fulfilled" value={statistics.fulfilledOrders} />
          <Statistic label="Units sold" value={statistics.totalUnits} />
          <Statistic label="Sales" value={moneyLabel(statistics.totalSales)} />
        </div> : <button type="button" onClick={() => void loadStatistics()} className="mt-4 text-sm font-semibold text-brand">Retry summary</button>}
      </section>
      {loadingOrders ? <DashboardSkeleton /> : orderError && !statistics ? <section role="alert" className="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-rose-950"><p>{orderError}</p><button type="button" onClick={() => { void loadOrders(); void loadStatistics() }} className="mt-3 text-sm font-semibold underline">Try again</button></section> : orders && <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-5"><h2 className="text-xl font-semibold">Orders</h2><span className="text-sm text-slate-500">{orders.totalElements} total</span></div>
        {orders.items.length === 0 ? <p className="p-6 text-sm text-slate-600">No orders contain your items yet.</p> : <div className="divide-y divide-slate-100">{orders.items.map((order) => <VendorOrder key={order.orderId} order={order} statuses={statuses?.statuses ?? []} />)}</div>}
        {orders.totalPages > 1 && <nav aria-label="Vendor order pagination" className="flex items-center justify-between border-t border-slate-100 px-6 py-4"><button type="button" disabled={orders.page === 0} onClick={() => setOrderPage((current) => current - 1)} className="text-sm font-semibold text-brand disabled:opacity-50">Previous</button><span className="text-sm text-slate-600">Page {orders.page + 1} of {orders.totalPages}</span><button type="button" disabled={orders.page >= orders.totalPages - 1} onClick={() => setOrderPage((current) => current + 1)} className="text-sm font-semibold text-brand disabled:opacity-50">Next</button></nav>}
      </section>}
      {editingProduct !== undefined && <ProductForm product={editingProduct} saving={saving} onCancel={() => setEditingProduct(undefined)} onSubmit={saveProduct} />}
      {notice && <section role="status" className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-900">{notice}</section>}
      {actionError && <section role="alert" className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-950">{actionError}</section>}
      {loading && <DashboardSkeleton />}
      {!loading && error && <section role="alert" className="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-rose-950"><h2 className="font-semibold">Products unavailable</h2><p className="mt-1 text-sm leading-6">{error}</p><button type="button" onClick={() => void loadProducts()} className="mt-4 rounded-lg bg-white px-4 py-2 text-sm font-semibold text-rose-800 shadow-sm ring-1 ring-rose-200 hover:bg-rose-100">Try again</button></section>}
      {!loading && !error && products?.items.length === 0 && <section className="rounded-3xl border border-dashed border-slate-300 bg-white px-6 py-16 text-center"><h2 className="text-xl font-semibold">No products yet</h2><p className="mt-2 text-slate-600">Create your first product to start your catalog.</p></section>}
      {!loading && !error && products && products.items.length > 0 && <><div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">{products.items.map((product) => <ProductCard key={product.id} product={product} deleting={deletingId === product.id} onEdit={() => { setActionError(null); setNotice(null); setEditingProduct(product) }} onDelete={() => void removeProduct(product)} />)}</div>{totalPages > 1 && <nav aria-label="Vendor product pagination" className="flex items-center justify-between gap-4 border-t border-slate-200 pt-6"><button type="button" disabled={currentPage === 0} onClick={() => setPage(currentPage - 1)} className="rounded-xl border border-slate-300 px-4 py-2.5 text-sm font-semibold text-slate-700 enabled:hover:border-brand enabled:hover:text-brand disabled:cursor-not-allowed disabled:opacity-45">Previous</button><span className="text-sm font-medium text-slate-600">Page {currentPage + 1} of {totalPages}</span><button type="button" disabled={currentPage >= totalPages - 1} onClick={() => setPage(currentPage + 1)} className="rounded-xl border border-slate-300 px-4 py-2.5 text-sm font-semibold text-slate-700 enabled:hover:border-brand enabled:hover:text-brand disabled:cursor-not-allowed disabled:opacity-45">Next</button></nav>}</>}
    </div>
  )
}

function ProductForm({ product, saving, onCancel, onSubmit }: { product: Product | null; saving: boolean; onCancel: () => void; onSubmit: (request: ProductRequest) => Promise<void> }) {
  const [name, setName] = useState(product?.name ?? '')
  const [description, setDescription] = useState(product?.description ?? '')
  const [price, setPrice] = useState(String(product?.price ?? ''))
  const [stockQuantity, setStockQuantity] = useState(String(product?.stockQuantity ?? '0'))
  const [category, setCategory] = useState(product?.category ?? '')
  const [active, setActive] = useState(product?.active ?? true)
  const [validationError, setValidationError] = useState<string | null>(null)

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const parsedPrice = Number(price)
    const parsedStock = Number(stockQuantity)
    if (!name.trim() || !category.trim() || name.trim().length > 150 || category.trim().length > 100 || description.length > 5000 || !Number.isFinite(parsedPrice) || parsedPrice <= 0 || !Number.isInteger(parsedStock) || parsedStock < 0) {
      setValidationError('Enter a name and category, a price above 0, and a whole stock quantity of 0 or more.')
      return
    }
    setValidationError(null)
    await onSubmit({ name: name.trim(), description: description.trim() || undefined, price: parsedPrice, stockQuantity: parsedStock, category: category.trim(), active })
  }

  return <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm"><div className="flex items-center justify-between gap-4"><div><h2 className="text-xl font-semibold">{product ? 'Edit product' : 'Add product'}</h2><p className="mt-1 text-sm text-slate-600">Provide the catalog details shown to customers.</p></div><button type="button" onClick={onCancel} disabled={saving} className="text-sm font-semibold text-brand hover:underline">Cancel</button></div>{validationError && <p role="alert" className="mt-5 rounded-lg bg-rose-50 p-3 text-sm text-rose-900">{validationError}</p>}<form onSubmit={(event) => void submit(event)} className="mt-6 grid gap-5 md:grid-cols-2"><label className="grid gap-2 text-sm font-semibold text-slate-700">Name<input required maxLength={150} value={name} onChange={(event) => setName(event.target.value)} className="h-11 rounded-xl border border-slate-300 px-3 font-normal outline-none focus:border-brand focus:ring-3 focus:ring-sky-100" /></label><label className="grid gap-2 text-sm font-semibold text-slate-700">Category<input required maxLength={100} value={category} onChange={(event) => setCategory(event.target.value)} className="h-11 rounded-xl border border-slate-300 px-3 font-normal outline-none focus:border-brand focus:ring-3 focus:ring-sky-100" /></label><label className="grid gap-2 text-sm font-semibold text-slate-700">Price<input required min="0.01" step="0.01" type="number" value={price} onChange={(event) => setPrice(event.target.value)} className="h-11 rounded-xl border border-slate-300 px-3 font-normal outline-none focus:border-brand focus:ring-3 focus:ring-sky-100" /></label><label className="grid gap-2 text-sm font-semibold text-slate-700">Stock quantity<input required min="0" step="1" type="number" value={stockQuantity} onChange={(event) => setStockQuantity(event.target.value)} className="h-11 rounded-xl border border-slate-300 px-3 font-normal outline-none focus:border-brand focus:ring-3 focus:ring-sky-100" /></label><label className="grid gap-2 text-sm font-semibold text-slate-700 md:col-span-2">Description<textarea maxLength={5000} rows={4} value={description} onChange={(event) => setDescription(event.target.value)} className="rounded-xl border border-slate-300 px-3 py-2 font-normal outline-none focus:border-brand focus:ring-3 focus:ring-sky-100" /></label><label className="flex items-center gap-3 text-sm font-semibold text-slate-700 md:col-span-2"><input type="checkbox" checked={active} onChange={(event) => setActive(event.target.checked)} className="size-4" />Active product</label><div className="md:col-span-2"><button type="submit" disabled={saving} className="rounded-xl bg-brand px-5 py-3 text-sm font-semibold text-white hover:bg-[#102f59] disabled:cursor-not-allowed disabled:opacity-60">{saving ? 'Saving product...' : product ? 'Save changes' : 'Create product'}</button></div></form></section>
}

function Statistic({ label, value }: { label: string; value: number | string }) {
  return <div className="rounded-xl bg-slate-50 px-4 py-5"><p className="text-sm text-slate-500">{label}</p><p className="mt-1 text-2xl font-semibold text-ink">{value}</p></div>
}

function VendorOrder({ order, statuses }: { order: Order; statuses: OrderStatusOption[] }) {
  const statusLabel = statuses.find((status) => status.value === order.status)?.label ?? order.status
  return <article className="p-5"><div className="flex flex-wrap items-start justify-between gap-3"><div><p className="font-semibold">Order #{order.orderId}</p><p className="mt-1 text-sm text-slate-500">{new Date(order.createdAt).toLocaleString()}</p></div><span className="rounded-full bg-sky-50 px-3 py-1 text-xs font-semibold text-sky-700">{statusLabel}</span></div><div className="mt-4 divide-y divide-slate-100 rounded-xl border border-slate-200">{order.items.map((item, index) => <div key={`${item.productId}-${index}`} className="flex justify-between gap-4 px-4 py-3 text-sm"><span>{item.productName} × {item.quantity}</span><span className="font-semibold text-slate-700">{moneyLabel(item.lineTotal)}</span></div>)}</div></article>
}

function moneyLabel(value: number | string) {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(Number(value))
}

function ProductCard({ product, deleting, onEdit, onDelete }: { product: Product; deleting: boolean; onEdit: () => void; onDelete: () => void }) {
  return <article className="flex min-h-56 flex-col rounded-2xl border border-slate-200 bg-white p-6 shadow-sm"><div className="flex items-start justify-between gap-4"><span className="rounded-full bg-sky-50 px-3 py-1 text-xs font-semibold text-brand">{product.category}</span><span className={`rounded-full px-3 py-1 text-xs font-semibold ${product.active ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'}`}>{product.active ? 'Active' : 'Inactive'}</span></div><h2 className="mt-5 text-xl font-semibold tracking-tight">{product.name}</h2><p className="mt-2 flex-1 text-sm leading-6 text-slate-600">{product.description || 'No description provided.'}</p><div className="mt-5 flex items-center justify-between border-t border-slate-100 pt-5"><span className="text-xl font-semibold">{priceLabel(product.price)}</span><span className="text-sm font-medium text-slate-600">{product.stockQuantity} in stock</span></div><div className="mt-5 flex gap-4 text-sm font-semibold"><button type="button" onClick={onEdit} className="text-brand hover:underline">Edit</button><button type="button" onClick={onDelete} disabled={deleting} className="text-rose-700 hover:underline disabled:cursor-not-allowed disabled:opacity-60">{deleting ? 'Deleting...' : 'Delete'}</button></div></article>
}

function DashboardSkeleton() {
  return <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">{Array.from({ length: 6 }, (_, index) => <div key={index} className="h-56 animate-pulse rounded-2xl border border-slate-200 bg-white p-6"><div className="h-6 w-24 rounded bg-slate-100" /><div className="mt-8 h-7 w-3/4 rounded bg-slate-100" /><div className="mt-4 h-4 w-full rounded bg-slate-100" /><div className="mt-12 h-8 w-28 rounded bg-slate-100" /></div>)}</div>
}
