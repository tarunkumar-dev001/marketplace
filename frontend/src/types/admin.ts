import type { RegisteredUser } from './auth'
import type { Product } from './product'

export interface AdminProduct extends Product {
  vendorId: number | null
  vendorEmail: string | null
}

export interface AdminPage<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type AdminUserPage = AdminPage<RegisteredUser>
export type AdminProductPage = AdminPage<AdminProduct>
