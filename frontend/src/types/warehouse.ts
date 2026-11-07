import type { Audited } from './api'

/** Backend `WarehouseResponse`. `address` is null when not set; `skuCount` is the number of products stocked there. */
export interface WarehouseResponse extends Audited {
  id: number
  code: string
  name: string
  address: string | null
  skuCount: number
}

/** Body of POST / PUT /api/warehouses. Only `address` is optional. */
export interface WarehouseRequest {
  code: string
  name: string
  address?: string | null
}
