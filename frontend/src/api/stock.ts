import type { PageQuery, PageResponse } from '../types/api'
import type {
  AdjustStockRequest,
  StockLevelFilter,
  StockLevelResponse,
  StockMovementFilter,
  StockMovementResponse,
} from '../types/stock'
import { apiClient } from './client'

/** Calls to /api/stock-levels and /api/stock-movements (inventory-service). */

/** GET /api/stock-levels: one page, with optional filters (combined with AND). */
export async function getStockLevels(
  query: PageQuery & StockLevelFilter,
  signal?: AbortSignal,
): Promise<PageResponse<StockLevelResponse>> {
  const response = await apiClient.get<PageResponse<StockLevelResponse>>('/api/stock-levels', { params: query, signal })
  return response.data
}

/**
 * POST /api/stock-levels/adjust. Taking more than is available rejects with code INSUFFICIENT_STOCK, an unknown
 * product or warehouse with NOT_FOUND. Returns the changed stock level.
 */
export async function adjustStock(request: AdjustStockRequest): Promise<StockLevelResponse> {
  const response = await apiClient.post<StockLevelResponse>('/api/stock-levels/adjust', request)
  return response.data
}

/** GET /api/stock-movements: one page of the history, with optional filters (combined with AND). */
export async function getStockMovements(
  query: PageQuery & StockMovementFilter,
  signal?: AbortSignal,
): Promise<PageResponse<StockMovementResponse>> {
  const response = await apiClient.get<PageResponse<StockMovementResponse>>('/api/stock-movements', {
    params: query,
    signal,
  })
  return response.data
}
