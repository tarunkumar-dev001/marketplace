export type OrderStatus = 'PLACED' | 'CANCELLED' | 'FULFILLED'

export interface OrderItem {
  productId: number
  productName: string
  unitPrice: number | string
  quantity: number
  lineTotal: number | string
  fulfillmentStatus?: 'PLACED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED'
}

export interface Order {
  orderId: number
  status: OrderStatus
  currency: string
  totalAmount: number | string
  createdAt: string
  updatedAt: string
  shippingName?: string | null
  shippingPhone?: string | null
  shippingAddress?: string | null
  shippingCity?: string | null
  shippingPincode?: string | null
  paymentMethod?: 'COD' | 'DEMO_CARD'
  paymentStatus?: 'PENDING' | 'PAID'
  items: OrderItem[]
}

export interface OrderPage {
  items: Order[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface VendorOrderStatistics {
  totalOrders: number
  placedOrders: number
  fulfilledOrders: number
  cancelledOrders: number
  totalUnits: number
  totalSales: number | string
}

export interface AdminOrderStatistics {
  totalOrders: number
  placedOrders: number
  fulfilledOrders: number
  cancelledOrders: number
  totalUnits: number
  totalSales: number | string
}

export interface OrderStatusOption {
  value: string
  label: string
}

export interface OrderStatuses {
  statuses: OrderStatusOption[]
}
