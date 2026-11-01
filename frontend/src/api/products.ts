import { apiClient } from './client'
import type { ApiResponse } from '../types/api'
import type { Product, ProductPage, ProductQuery, ProductRequest } from '../types/product'

function requireData<T>(response: ApiResponse<T>, fallback: string): T {
  if (!response.success || response.data === null) throw new Error(response.message || fallback)
  return response.data
}

export async function getProducts(query: ProductQuery = {}): Promise<ProductPage> {
  const response = await apiClient.get<ApiResponse<ProductPage>>('/api/products', { params: query })

  return requireData(response.data, 'Unable to load products.')
}

export async function getProduct(productId: number): Promise<Product> {
  const response = await apiClient.get<ApiResponse<Product>>(`/api/products/${productId}`)

  return requireData(response.data, 'Unable to load product.')
}

export async function getMyProducts(page = 0, size = 20): Promise<ProductPage> {
  const response = await apiClient.get<ApiResponse<ProductPage>>('/api/products/mine', { params: { page, size } })

  return requireData(response.data, 'Unable to load your products.')
}

export async function createProduct(product: ProductRequest): Promise<Product> {
  const response = await apiClient.post<ApiResponse<Product>>('/api/products', product)
  return requireData(response.data, 'Unable to create product.')
}

export async function updateProduct(productId: number, product: ProductRequest): Promise<Product> {
  const response = await apiClient.put<ApiResponse<Product>>(`/api/products/${productId}`, product)
  return requireData(response.data, 'Unable to update product.')
}

export async function deleteProduct(productId: number): Promise<void> {
  await apiClient.delete(`/api/products/${productId}`)
}
