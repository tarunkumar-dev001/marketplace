import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ServerWakeBanner } from './ServerWakeBanner'
import { Home, Package, ShoppingCart, ClipboardList, LayoutDashboard, LogOut, LogIn, UserPlus } from 'lucide-react'

const customerNavigation = [
  { label: 'Home', to: '/', icon: Home },
  { label: 'Products', to: '/products', icon: Package },
  { label: 'Cart', to: '/cart', icon: ShoppingCart },
  { label: 'Orders', to: '/orders', icon: ClipboardList },
]

const vendorNavigation = [
  { label: 'Home', to: '/', icon: Home },
  { label: 'Products', to: '/products', icon: Package },
  { label: 'Dashboard', to: '/vendor/dashboard', icon: LayoutDashboard },
]

const adminNavigation = [
  { label: 'Home', to: '/', icon: Home },
  { label: 'Dashboard', to: '/admin/dashboard', icon: LayoutDashboard },
]

export function AppLayout() {
  const { user, isAuthenticated, signOut } = useAuth()
  const navigate = useNavigate()

  function logout() {
    signOut()
    navigate('/', { replace: true })
  }

  return (
    <div className="flex min-h-screen flex-col bg-mist text-ink">
      <ServerWakeBanner />
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-4 px-5 py-4 sm:px-8">
          <NavLink className="flex items-center gap-3 font-semibold tracking-tight" to="/">
            <span className="grid size-9 place-items-center rounded-xl bg-brand text-sm font-bold text-white">M</span>
            <span>Marketline</span>
          </NavLink>
          <nav aria-label="Primary navigation" className="order-3 flex w-full gap-1 overflow-x-auto sm:order-none sm:w-auto">
            {(user?.role === 'ADMIN' ? adminNavigation : user?.role === 'VENDOR' ? vendorNavigation : customerNavigation).map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) => `rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                  isActive ? 'bg-slate-100 text-brand' : 'text-slate-600 hover:bg-slate-50 hover:text-brand'
                }`}
              >
                <span className="flex items-center gap-2"><item.icon size={15} />{item.label}</span>
              </NavLink>
            ))}
          </nav>
          <div className="flex items-center gap-2">
            {isAuthenticated ? (
              <>
                <span className="hidden max-w-44 truncate px-2 text-sm font-medium text-slate-600 sm:block" title={user?.email}>{user?.email}</span>
                <button type="button" onClick={logout} className="flex items-center gap-2 rounded-lg border border-slate-300 px-3 py-2 text-sm font-semibold text-brand hover:bg-slate-50"><LogOut size={16} />Log out</button>
              </>
            ) : (
              <>
                <NavLink className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-semibold text-brand hover:bg-slate-50" to="/login"><LogIn size={16} />Sign in</NavLink>
                <NavLink className="flex items-center gap-2 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-sm hover:bg-[#102f59]" to="/register"><UserPlus size={16} />Get started</NavLink>
              </>
            )}
          </div>
        </div>
      </header>
      <main className="mx-auto w-full max-w-7xl flex-1 px-5 py-10 sm:px-8 sm:py-14">
        <Outlet />
      </main>
      <footer className="border-t border-slate-200 bg-white">
        <div className="mx-auto flex max-w-7xl flex-col gap-2 px-5 py-6 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between sm:px-8">
          <span>© 2026 Marketline. Built for secure commerce.</span>
          <span>Customer marketplace experience</span>
        </div>
      </footer>
    </div>
  )
}
