import { MAX_PAGE_SIZE } from '../constants/app'
import type { PageQuery, PageResponse } from '../types/api'
import type { SupplierRequest, SupplierResponse } from '../types/supplier'
import type { SelectOption } from '../types/ui'
import { apiClient } from './client'

/** Calls to /api/suppliers (product-service). Pages use these functions, never Axios directly. */

/** GET /api/suppliers: one page, optionally filtered by part of the name. */
export async function getSuppliers(
  query: PageQuery & { search?: string },
  signal?: AbortSignal,
): Promise<PageResponse<SupplierResponse>> {
  const response = await apiClient.get<PageResponse<SupplierResponse>>('/api/suppliers', { params: query, signal })
  return response.data
}

/** GET /api/suppliers/{id}. A missing supplier rejects with an ApiError with code NOT_FOUND. */
export async function getSupplier(id: number, signal?: AbortSignal): Promise<SupplierResponse> {
  const response = await apiClient.get<SupplierResponse>(`/api/suppliers/${id}`, { signal })
  return response.data
}

/** POST /api/suppliers. A taken name rejects with code DUPLICATE. */
export async function createSupplier(request: SupplierRequest): Promise<SupplierResponse> {
  const response = await apiClient.post<SupplierResponse>('/api/suppliers', request)
  return response.data
}

/** PUT /api/suppliers/{id}: replaces every field. */
export async function updateSupplier(id: number, request: SupplierRequest): Promise<SupplierResponse> {
  const response = await apiClient.put<SupplierResponse>(`/api/suppliers/${id}`, request)
  return response.data
}

/** DELETE /api/suppliers/{id}. A supplier that still has products rejects with code IN_USE. */
export async function deleteSupplier(id: number): Promise<void> {
  await apiClient.delete(`/api/suppliers/${id}`)
}

/** Every supplier as Select options, sorted by name (one page of MAX_PAGE_SIZE, like getCategoryOptions). */
export async function getSupplierOptions(signal?: AbortSignal): Promise<SelectOption[]> {
  const page = await getSuppliers({ page: 0, size: MAX_PAGE_SIZE, sort: 'name,asc' }, signal)
  return page.content.map(supplier => ({ value: supplier.id, label: supplier.name }))
}
