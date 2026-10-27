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
    // An error without our JSON format, e.g. from a proxy in front of the backend
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
 */
export function formFieldErrors(error: unknown): { name: (string | number)[]; errors: string[] }[] {
  const apiError = toApiError(error)
  if (apiError.code !== 'VALIDATION_FAILED') {
    return []
  }
  return apiError.errors.map(fieldError => ({
    name: toNamePath(fieldError.field),
    errors: [fieldError.message],
  }))
}

function toNamePath(field: string): (string | number)[] {
  return field
    .split(/[.[\]]/)
    .filter(part => part !== '')
    .map(part => (/^\d+$/.test(part) ? Number(part) : part))
}

function isProblemDetail(data: unknown): data is ProblemDetail {
  return typeof data === 'object' && data !== null && 'code' in data && 'status' in data && 'detail' in data
}
