import { Route, Routes } from 'react-router-dom'
import { AppLayout } from './components/AppLayout'
import { RequireCustomer } from './auth/RequireCustomer'
import { RequireVendor } from './auth/RequireVendor'
import { RequireAdmin } from './auth/RequireAdmin'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { ProductDetailsPage } from './pages/ProductDetailsPage'
import { ProductsPage } from './pages/ProductsPage'
import { RegisterPage } from './pages/RegisterPage'
import { CartPage } from './pages/CartPage'
import { CheckoutPage } from './pages/CheckoutPage'
import { OrderDetailsPage } from './pages/OrderDetailsPage'
import { OrdersPage } from './pages/OrdersPage'
import { VendorDashboardPage } from './pages/VendorDashboardPage'
import { AdminDashboardPage } from './pages/AdminDashboardPage'
import { NotFoundPage } from './pages/NotFoundPage'

function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<HomePage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="products" element={<ProductsPage />} />
        <Route path="products/:id" element={<ProductDetailsPage />} />
        <Route path="cart" element={<RequireCustomer><CartPage /></RequireCustomer>} />
        <Route path="checkout" element={<RequireCustomer><CheckoutPage /></RequireCustomer>} />
        <Route path="orders" element={<RequireCustomer><OrdersPage /></RequireCustomer>} />
        <Route path="orders/:id" element={<RequireCustomer><OrderDetailsPage /></RequireCustomer>} />
        <Route path="vendor/dashboard" element={<RequireVendor><VendorDashboardPage /></RequireVendor>} />
        <Route path="admin/dashboard" element={<RequireAdmin><AdminDashboardPage /></RequireAdmin>} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}

export default App
