export type OrderStatus = 'PLACED' | 'CANCELLED' | 'FULFILLED'

export interface OrderItem {
  productId: number
  productName: string
  unitPrice: number | string
  quantity: number
  lineTotal: number | string
}

export interface Order {
  orderId: number
  status: OrderStatus
  currency: string
  totalAmount: number | string
  createdAt: string
  updatedAt: string
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
