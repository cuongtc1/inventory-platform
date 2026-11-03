import type { FormInstance } from 'antd'
import axios from 'axios'
import { texts } from '../messages/texts'
import type { ErrorCode, FieldValidationError, ProblemDetail } from '../types/api'

/**
 * Every failed API call ends up as one ApiError (docs/conventions.md section 10), whatever went wrong: an error
 * response from the backend, or no response at all. Pages decide by `code`, never by the `detail` text.
 *
 * Fields are declared one by one (no constructor "parameter properties"): this project's tsconfig uses
 * `erasableSyntaxOnly`, which only allows TypeScript syntax that can simply be deleted to get JavaScript.
 */
export class ApiError extends Error {
  readonly status: number
  readonly code: ErrorCode
  readonly detail: string
  readonly errors: FieldValidationError[]

  constructor(status: number, code: ErrorCode, detail: string, errors: FieldValidationError[] = []) {
    super(detail)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.detail = detail
    this.errors = errors
  }

  /** True for errors the user can fix or understand (4xx); false for server and network problems. */
  get isClientError(): boolean {
    return this.status >= 400 && this.status < 500
  }
}

/** Bad Gateway, Service Unavailable, Gateway Timeout: a proxy or gateway couldn't reach the service behind it. */
const GATEWAY_STATUSES = [502, 503, 504]

/** Turns anything thrown by an API call into an ApiError. Used by the Axios client's response interceptor. */
export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error
  }
  if (axios.isAxiosError(error)) {
    const response = error.response
    if (!response) {
      return new ApiError(0, 'NETWORK_ERROR', texts.common.errors.network)
    }
    if (isProblemDetail(response.data)) {
      const problem = response.data
      return new ApiError(problem.status, problem.code, problem.detail, problem.errors ?? [])
    }
    // Our services always answer with Problem Details. 502/503/504 without it come from something in between
    // (the Vite dev proxy now, the api-gateway from Phase 3) that couldn't reach the service: same as no response.
    if (GATEWAY_STATUSES.includes(response.status)) {
      return new ApiError(response.status, 'NETWORK_ERROR', texts.common.errors.network)
    }
    return new ApiError(response.status, response.status >= 500 ? 'INTERNAL_ERROR' : 'BAD_REQUEST',
      texts.common.errors.unexpected)
  }
  return new ApiError(0, 'INTERNAL_ERROR', texts.common.errors.unexpected)
}

/**
 * The text to show for an error: the backend's own `detail` for 4xx (already written for people), a generic
 * text for 5xx and network errors (the real cause is only in the server log).
 */
export function errorMessage(error: unknown): string {
  const apiError = toApiError(error)
  if (apiError.code === 'NETWORK_ERROR') {
    return texts.common.errors.network
  }
  if (apiError.code === 'VALIDATION_FAILED') {
    return texts.common.errors.validation
  }
  return apiError.isClientError ? apiError.detail : texts.common.errors.unexpected
}

/**
 * For a 400 VALIDATION_FAILED: one entry per invalid field, in the shape Ant Design's `form.setFields()` takes,
 * so each message appears under its field. Empty for any other error.
 *
 * `field` can be a nested path from the backend ("items[0].quantity"); it is split into Ant Design's name path.
 *
 * `codeFields` puts other errors under a field too: `{ DUPLICATE: 'sku' }` shows "Product with SKU 'A01' already
 * exists" under the SKU field. The backend's `detail` is the text.
 *
 * `Values` is the form's type (`formFieldErrors<ProductRequest>(error)` for a `Form.useForm<ProductRequest>()`).
 * The field names come from the backend at runtime, so TypeScript can't check them against `Values`; the cast
 * says "trust the backend": its field names are the request's field names (same DTO).
 */
export function formFieldErrors<Values = unknown>(
  error: unknown,
  codeFields: Partial<Record<ErrorCode, keyof Values & string>> = {},
): FormFieldErrors<Values> {
  const apiError = toApiError(error)
  const field = codeFields[apiError.code]
  if (field) {
    return [{ name: [field], errors: [apiError.detail] }] as FormFieldErrors<Values>
  }
  if (apiError.code !== 'VALIDATION_FAILED') {
    return []
  }
  return apiError.errors.map(fieldError => ({
    name: toNamePath(fieldError.field),
    errors: [fieldError.message],
  })) as FormFieldErrors<Values>
}

/** What `form.setFields()` takes, from Ant Design's own types instead of a deep import. */
type FormFieldErrors<Values> = Parameters<FormInstance<Values>['setFields']>[0]

function toNamePath(field: string): (string | number)[] {
  return field
    .split(/[.[\]]/)
    .filter(part => part !== '')
    .map(part => (/^\d+$/.test(part) ? Number(part) : part))
}

function isProblemDetail(data: unknown): data is ProblemDetail {
  return typeof data === 'object' && data !== null && 'code' in data && 'status' in data && 'detail' in data
}
