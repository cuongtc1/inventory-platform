import type { Audited } from './api'

/** Backend `SupplierResponse`. Contact fields are null when not set. */
export interface SupplierResponse extends Audited {
  id: number
  name: string
  contactName: string | null
  email: string | null
  phone: string | null
}

/** Body of POST / PUT /api/suppliers. Only `name` is required. */
export interface SupplierRequest {
  name: string
  contactName?: string | null
  email?: string | null
  phone?: string | null
}
