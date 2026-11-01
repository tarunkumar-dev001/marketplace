import { isAxiosError } from 'axios'
import { apiClient } from './client'
import type { ApiResponse } from '../types/api'
import type { Cart, CartItem, CheckoutOrder } from '../types/cart'

function requireData<T>(response: ApiResponse<T>, fallback: string): T {
  if (!response.success || response.data === null) throw new Error(response.message || fallback)
  return response.data
}

export async function getCart(): Promise<Cart> {
  const response = await apiClient.get<ApiResponse<Cart>>('/api/cart')
  return requireData(response.data, 'Unable to load your cart.')
}

export async function addCartItem(productId: number, quantity: number): Promise<Cart> {
  const response = await apiClient.post<ApiResponse<Cart>>('/api/cart/items', { productId, quantity })
  return requireData(response.data, 'Unable to add this product to your cart.')
}

export async function updateCartItem(itemId: number, quantity: number): Promise<CartItem> {
  const response = await apiClient.put<ApiResponse<CartItem>>(`/api/cart/items/${itemId}`, { quantity })
  return requireData(response.data, 'Unable to update this cart item.')
}

export async function removeCartItem(itemId: number): Promise<void> {
  await apiClient.delete(`/api/cart/items/${itemId}`)
}

export async function checkoutCart(): Promise<CheckoutOrder> {
  const response = await apiClient.post<ApiResponse<CheckoutOrder>>('/api/cart/checkout')
  return requireData(response.data, 'We could not complete checkout. Please try again.')
}

export function cartErrorMessage(error: unknown, fallback = 'We could not update your cart. Please try again.'): string {
  if (isAxiosError<ApiResponse<null>>(error)) {
    return error.response?.data?.message ?? fallback
  }
  return error instanceof Error ? error.message : fallback
}
