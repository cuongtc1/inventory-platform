import { MAX_PAGE_SIZE } from '../constants/app'
import type { PageQuery, PageResponse } from '../types/api'
import type { SupplierResponse } from '../types/supplier'
import type { SelectOption } from '../types/ui'
import { apiClient } from './client'

/**
 * Calls to /api/suppliers (product-service). Pages use these functions, never Axios directly.
 * T2.4 adds get, create, update and delete.
 */

/** GET /api/suppliers: one page, optionally filtered by part of the name. */
export async function getSuppliers(
  query: PageQuery & { search?: string },
  signal?: AbortSignal,
): Promise<PageResponse<SupplierResponse>> {
  const response = await apiClient.get<PageResponse<SupplierResponse>>('/api/suppliers', { params: query, signal })
  return response.data
}

/** Every supplier as Select options, sorted by name (one page of MAX_PAGE_SIZE, like getCategoryOptions). */
export async function getSupplierOptions(signal?: AbortSignal): Promise<SelectOption[]> {
  const page = await getSuppliers({ page: 0, size: MAX_PAGE_SIZE, sort: 'name,asc' }, signal)
  return page.content.map(supplier => ({ value: supplier.id, label: supplier.name }))
}
