import { isAxiosError } from 'axios'
import { apiClient } from './client'
import type { ApiResponse } from '../types/api'
import type { Order, OrderPage, OrderStatuses, VendorOrderStatistics } from '../types/order'

function requireData<T>(response: ApiResponse<T>, fallback: string): T {
  if (!response.success || response.data === null) throw new Error(response.message || fallback)
  return response.data
}

export async function getOrders(page = 0, size = 20): Promise<OrderPage> {
  const response = await apiClient.get<ApiResponse<OrderPage>>('/api/orders', { params: { page, size } })
  return requireData(response.data, 'Unable to load your orders.')
}

export async function getVendorOrders(page = 0, size = 20): Promise<OrderPage> {
  const response = await apiClient.get<ApiResponse<OrderPage>>('/api/vendor/orders', { params: { page, size } })
  return requireData(response.data, 'Unable to load vendor orders.')
}

export async function getVendorOrderStatistics(): Promise<VendorOrderStatistics> {
  const response = await apiClient.get<ApiResponse<VendorOrderStatistics>>('/api/vendor/orders/statistics')
  return requireData(response.data, 'Unable to load vendor order statistics.')
}

export async function getOrderStatuses(): Promise<OrderStatuses> {
  const response = await apiClient.get<ApiResponse<OrderStatuses>>('/api/orders/statuses')
  return requireData(response.data, 'Unable to load order statuses.')
}

export async function getOrder(orderId: string): Promise<Order> {
  const response = await apiClient.get<ApiResponse<Order>>(`/api/orders/${orderId}`)
  return requireData(response.data, 'Unable to load this order.')
}

export async function cancelOrder(orderId: number): Promise<Order> {
  const response = await apiClient.post<ApiResponse<Order>>(`/api/orders/${orderId}/cancel`)
  return requireData(response.data, 'Unable to cancel this order.')
}

export function orderErrorMessage(error: unknown, fallback: string): string {
  if (isAxiosError<ApiResponse<null>>(error)) return error.response?.data?.message ?? fallback
  return error instanceof Error ? error.message : fallback
}
