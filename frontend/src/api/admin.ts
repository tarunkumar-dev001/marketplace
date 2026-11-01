import { apiClient } from './client'
import type { ApiResponse } from '../types/api'
import type { RegisteredUser } from '../types/auth'
import type { AdminProduct, AdminProductPage, AdminUserPage } from '../types/admin'
import type { AdminOrderStatistics, OrderPage } from '../types/order'

function requireData<T>(response: ApiResponse<T>, fallback: string): T {
  if (!response.success || response.data === null) throw new Error(response.message || fallback)
  return response.data
}

export async function getAdminUsers(page = 0, size = 20): Promise<AdminUserPage> {
  const response = await apiClient.get<ApiResponse<AdminUserPage>>('/api/admin/users', { params: { page, size } })
  return requireData(response.data, 'Unable to load users.')
}

export async function updateAdminUserStatus(userId: number, active: boolean): Promise<RegisteredUser> {
  const response = await apiClient.patch<ApiResponse<RegisteredUser>>(`/api/admin/users/${userId}/status`, { active })
  return requireData(response.data, 'Unable to update user status.')
}

export async function getAdminProducts(page = 0, size = 20): Promise<AdminProductPage> {
  const response = await apiClient.get<ApiResponse<AdminProductPage>>('/api/admin/products', { params: { page, size } })
  return requireData(response.data, 'Unable to load products.')
}

export async function updateAdminProductStatus(productId: number, active: boolean): Promise<AdminProduct> {
  const response = await apiClient.patch<ApiResponse<AdminProduct>>(`/api/admin/products/${productId}/status`, { active })
  return requireData(response.data, 'Unable to update product status.')
}

export async function getAdminOrders(page = 0, size = 20): Promise<OrderPage> {
  const response = await apiClient.get<ApiResponse<OrderPage>>('/api/admin/orders', { params: { page, size } })
  return requireData(response.data, 'Unable to load marketplace orders.')
}

export async function getAdminOrderStatistics(): Promise<AdminOrderStatistics> {
  const response = await apiClient.get<ApiResponse<AdminOrderStatistics>>('/api/admin/orders/statistics')
  return requireData(response.data, 'Unable to load marketplace order statistics.')
}
