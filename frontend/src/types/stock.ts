import type { Audited } from './api'

/** Backend `StockLevelResponse`: one product in one warehouse. */
export interface StockLevelResponse extends Audited {
  id: number
  warehouseId: number
  warehouseCode: string
  warehouseName: string
  productId: number
  productSku: string
  productName: string
  onHand: number
  reserved: number
  /** `onHand - reserved`, worked out by the backend. */
  available: number
  reorderLevel: number
  /** True when `available` is at or below `reorderLevel`. */
  lowStock: boolean
}

/** Filters of GET /api/stock-levels, sent as query parameters with the same names. All optional. */
export interface StockLevelFilter {
  search?: string
  warehouseId?: number
  productId?: number
  lowStock?: boolean
}

/** Body of POST /api/stock-levels/adjust. `quantity` is the change, not the new total: +50 or -2. */
export interface AdjustStockRequest {
  productId: number
  warehouseId: number
  quantity: number
  reference?: string | null
}

/** Backend enum `MovementType`. */
export type MovementType = 'IN' | 'OUT' | 'ADJUST' | 'RESERVE' | 'RELEASE'

/** Backend `StockMovementResponse`. `quantity` is signed; the product's name is as it was at that moment. */
export interface StockMovementResponse extends Audited {
  id: number
  warehouseId: number
  warehouseCode: string
  productId: number
  productSku: string
  productName: string
  movementType: MovementType
  quantity: number
  reference: string | null
}

/**
 * Filters of GET /api/stock-movements, sent as query parameters with the same names. All optional. `from` and
 * `to` are `YYYY-MM-DD`, both included; the backend reads them as Vietnam dates.
 */
export interface StockMovementFilter {
  search?: string
  warehouseId?: number
  movementType?: MovementType
  from?: string
  to?: string
}
