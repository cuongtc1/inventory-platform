import { MAX_PAGE_SIZE } from '../constants/app'
import type { PageQuery, PageResponse } from '../types/api'
import type { SelectOption } from '../types/ui'
import type { WarehouseRequest, WarehouseResponse } from '../types/warehouse'
import { apiClient } from './client'

/** Calls to /api/warehouses (inventory-service). Pages use these functions, never Axios directly. */

/** GET /api/warehouses: one page, optionally filtered by part of the code or name. */
export async function getWarehouses(
  query: PageQuery & { search?: string },
  signal?: AbortSignal,
): Promise<PageResponse<WarehouseResponse>> {
  const response = await apiClient.get<PageResponse<WarehouseResponse>>('/api/warehouses', { params: query, signal })
  return response.data
}

/** GET /api/warehouses/{id}. A missing warehouse rejects with an ApiError with code NOT_FOUND. */
export async function getWarehouse(id: number, signal?: AbortSignal): Promise<WarehouseResponse> {
  const response = await apiClient.get<WarehouseResponse>(`/api/warehouses/${id}`, { signal })
  return response.data
}

/** POST /api/warehouses. A taken code rejects with code DUPLICATE. */
export async function createWarehouse(request: WarehouseRequest): Promise<WarehouseResponse> {
  const response = await apiClient.post<WarehouseResponse>('/api/warehouses', request)
  return response.data
}

/** PUT /api/warehouses/{id}: replaces every field. */
export async function updateWarehouse(id: number, request: WarehouseRequest): Promise<WarehouseResponse> {
  const response = await apiClient.put<WarehouseResponse>(`/api/warehouses/${id}`, request)
  return response.data
}

/** DELETE /api/warehouses/{id}. A warehouse that still has stock rejects with code IN_USE. */
export async function deleteWarehouse(id: number): Promise<void> {
  await apiClient.delete(`/api/warehouses/${id}`)
}

/** Every warehouse as Select options, sorted by code (one page of MAX_PAGE_SIZE, like getCategoryOptions). */
export async function getWarehouseOptions(signal?: AbortSignal): Promise<SelectOption[]> {
  const page = await getWarehouses({ page: 0, size: MAX_PAGE_SIZE, sort: 'code,asc' }, signal)
  return page.content.map(warehouse => ({ value: warehouse.id, label: `${warehouse.code} ${warehouse.name}` }))
}
