export interface CartItem {
  id: number
  productId: number
  productName: string
  price: number | string
  quantity: number
  lineTotal: number | string
}

export interface Cart {
  id: number | null
  items: CartItem[]
  subtotal: number | string
  updatedAt: string | null
}

export interface CheckoutItem {
  productId: number
  productName: string
  unitPrice: number | string
  quantity: number
  lineTotal: number | string
}

export interface CheckoutOrder {
  orderId: number
  status: 'PLACED' | 'CANCELLED' | 'FULFILLED'
  currency: string
  totalAmount: number | string
  createdAt: string | null
  items: CheckoutItem[]
}
