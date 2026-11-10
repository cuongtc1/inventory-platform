import type { PageQuery, PageResponse } from '../types/api'
import type {
  CreateSalesOrderRequest,
  OrderAction,
  OrderStatusHistoryResponse,
  SalesOrderFilter,
  SalesOrderResponse,
  SalesOrderSummaryResponse,
} from '../types/salesOrder'
import { apiClient } from './client'

/** Calls to /api/sales-orders (order-service). Pages use these functions, never Axios directly. */

/** GET /api/sales-orders: one page without lines, with optional filters (combined with AND). */
export async function getSalesOrders(
  query: PageQuery & SalesOrderFilter,
  signal?: AbortSignal,
): Promise<PageResponse<SalesOrderSummaryResponse>> {
  const response = await apiClient.get<PageResponse<SalesOrderSummaryResponse>>('/api/sales-orders', {
    params: query,
    signal,
  })
  return response.data
}

/** GET /api/sales-orders/{id}: one order with its lines. */
export async function getSalesOrder(id: number, signal?: AbortSignal): Promise<SalesOrderResponse> {
  const response = await apiClient.get<SalesOrderResponse>(`/api/sales-orders/${id}`, { signal })
  return response.data
}

/**
 * POST /api/sales-orders: the new order, as PENDING. An inactive product or the same product twice rejects with
 * BAD_REQUEST, an unknown customer, warehouse or product with NOT_FOUND.
 */
export async function createSalesOrder(request: CreateSalesOrderRequest): Promise<SalesOrderResponse> {
  const response = await apiClient.post<SalesOrderResponse>('/api/sales-orders', request)
  return response.data
}

/**
 * POST /api/sales-orders/{id}/pay, /ship, /complete or /cancel. A status that doesn't allow it rejects with
 * INVALID_STATUS_TRANSITION; someone changing the order at the same moment with CONCURRENT_UPDATE.
 */
export async function changeSalesOrderStatus(id: number, action: OrderAction): Promise<SalesOrderResponse> {
  const response = await apiClient.post<SalesOrderResponse>(`/api/sales-orders/${id}/${action}`)
  return response.data
}

/** GET /api/sales-orders/{id}/history: the timeline, oldest first. */
export async function getSalesOrderHistory(id: number, signal?: AbortSignal): Promise<OrderStatusHistoryResponse[]> {
  const response = await apiClient.get<OrderStatusHistoryResponse[]>(`/api/sales-orders/${id}/history`, { signal })
  return response.data
}
