import type { Audited } from './api'

/** Backend `CustomerResponse`. Contact fields are null when not set; `orderCount` is the mockup's Orders column. */
export interface CustomerResponse extends Audited {
  id: number
  name: string
  email: string | null
  phone: string | null
  orderCount: number
}

/** Body of POST / PUT /api/customers. Only `name` is required. */
export interface CustomerRequest {
  name: string
  email?: string | null
  phone?: string | null
}
