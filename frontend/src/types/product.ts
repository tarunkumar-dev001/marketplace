export interface Product {
  id: number
  name: string
  description: string | null
  price: number | string
  stockQuantity: number
  category: string
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface ProductPage {
  items: Product[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ProductRequest {
  name: string
  description?: string
  price: number
  stockQuantity: number
  category: string
  active: boolean
}

export interface ProductQuery {
  page?: number
  size?: number
  category?: string
  search?: string
  sortBy?: 'createdAt' | 'name' | 'price'
  direction?: 'asc' | 'desc'
}
