import type { Audited } from './api'

/**
 * Backend enum `ProductStatus`. A union of string literals instead of a TypeScript `enum`: the JSON already
 * carries the name ("ACTIVE"), and this project's tsconfig (`erasableSyntaxOnly`) doesn't allow `enum`.
 */
export type ProductStatus = 'ACTIVE' | 'INACTIVE'

/** Backend `ProductResponse`. `supplierId` / `supplierName` are null when no supplier is set. */
export interface ProductResponse extends Audited {
  id: number
  sku: string
  name: string
  categoryId: number
  categoryName: string
  supplierId: number | null
  supplierName: string | null
  /** USD with 2 decimals. Only display it (formatMoney) or send it back; never calculate with it here. */
  costPrice: number
  salePrice: number
  reorderLevel: number
  status: ProductStatus
}

/** Body of POST / PUT /api/products. */
export interface ProductRequest {
  sku: string
  name: string
  categoryId: number
  supplierId?: number | null
  costPrice: number
  salePrice: number
  reorderLevel: number
  status: ProductStatus
}

/** Filters of GET /api/products, sent as query parameters with the same names. All optional. */
export interface ProductFilter {
  search?: string
  categoryId?: number
  supplierId?: number
  status?: ProductStatus
}
