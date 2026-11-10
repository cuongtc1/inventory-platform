import type { Audited } from './api'

/** Backend enum `OrderStatus`. */
export type OrderStatus = 'PENDING' | 'RESERVED' | 'PAID' | 'SHIPPED' | 'COMPLETED' | 'REJECTED' | 'CANCELLED'

/** One line of an order. SKU, name and price are as they were when the order was created. */
export interface SalesOrderItem {
  id: number
  productId: number
  productSku: string
  productName: string
  quantity: number
  /** USD. Only display it (formatMoney); the backend calculated `lineTotal`. */
  unitPrice: number
  lineTotal: number
}

/** Backend `SalesOrderResponse`: one order with its lines. */
export interface SalesOrderResponse extends Audited {
  id: number
  orderNumber: string
  customerId: number
  customerName: string
  warehouseId: number
  warehouseCode: string
  status: OrderStatus
  totalAmount: number
  items: SalesOrderItem[]
}

/** Backend `SalesOrderSummaryResponse`: one row of the order list, without lines. */
export interface SalesOrderSummaryResponse {
  id: number
  orderNumber: string
  customerId: number
  customerName: string
  warehouseCode: string
  status: OrderStatus
  totalAmount: number
  createdAt: string
  createdBy: string
}

/** Filters of GET /api/sales-orders, sent as query parameters with the same names. All optional. */
export interface SalesOrderFilter {
  search?: string
  customerId?: number
  status?: OrderStatus
}

/** Body of POST /api/sales-orders: only ids and quantities; prices come from product-service. */
export interface CreateSalesOrderRequest {
  customerId: number
  warehouseId: number
  items: { productId: number; quantity: number }[]
}

/** Backend `OrderStatusHistoryResponse`: one row of the timeline. `fromStatus` is null on the first. */
export interface OrderStatusHistoryResponse {
  id: number
  fromStatus: OrderStatus | null
  toStatus: OrderStatus
  note: string | null
  createdAt: string
  createdBy: string
}

/** The status actions: POST /api/sales-orders/{id}/{action}. */
export type OrderAction = 'pay' | 'ship' | 'complete' | 'cancel'
