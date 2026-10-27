/**
 * Types shared by every API call. They mirror the backend (stockline-common) exactly: same names, same fields.
 */

/** One page of a list: backend `PageResponse<T>` (docs/conventions.md section 4). `page` starts at 0. */
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/** What a list request sends: `page` starts at 0, `sort` is `field,asc` or `field,desc`. */
export interface PageQuery {
  page: number
  size: number
  sort?: string
}

/** The `code` field of an error response: backend `ErrorCode`, plus the HTTP status names Spring MVC uses. */
export type ErrorCode =
  | 'VALIDATION_FAILED'
  | 'BAD_REQUEST'
  | 'UNAUTHORIZED'
  | 'FORBIDDEN'
  | 'NOT_FOUND'
  | 'METHOD_NOT_ALLOWED'
  | 'NOT_ACCEPTABLE'
  | 'UNSUPPORTED_MEDIA_TYPE'
  | 'DUPLICATE'
  | 'IN_USE'
  | 'INVALID_STATUS_TRANSITION'
  | 'CONCURRENT_UPDATE'
  | 'INSUFFICIENT_STOCK'
  | 'INTERNAL_ERROR'
  // Frontend only: no response at all (backend down, network error, timeout)
  | 'NETWORK_ERROR'

/** One entry of `errors` in a 400 VALIDATION_FAILED response. */
export interface FieldValidationError {
  field: string
  message: string
}

/** The JSON body of every backend error (Problem Details, docs/conventions.md section 3). */
export interface ProblemDetail {
  type: string
  title: string
  status: number
  detail: string
  instance: string
  code: ErrorCode
  timestamp: string
  errors?: FieldValidationError[]
}

/** The four audit fields every resource response ends with. */
export interface Audited {
  /** ISO-8601 in UTC, e.g. "2026-10-22T08:15:30Z". Show it with formatDateTime(). */
  createdAt: string
  createdBy: string
  updatedAt: string
  updatedBy: string
}
