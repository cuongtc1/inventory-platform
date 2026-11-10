import { MAX_PAGE_SIZE } from '../constants/app'
import type { PageQuery, PageResponse } from '../types/api'
import type { CustomerRequest, CustomerResponse } from '../types/customer'
import type { SelectOption } from '../types/ui'
import { apiClient } from './client'

/** Calls to /api/customers (order-service). Pages use these functions, never Axios directly. */

/** GET /api/customers: one page, optionally filtered by part of the name or email. */
export async function getCustomers(
  query: PageQuery & { search?: string },
  signal?: AbortSignal,
): Promise<PageResponse<CustomerResponse>> {
  const response = await apiClient.get<PageResponse<CustomerResponse>>('/api/customers', { params: query, signal })
  return response.data
}

/** GET /api/customers/{id}. A missing customer rejects with an ApiError with code NOT_FOUND. */
export async function getCustomer(id: number, signal?: AbortSignal): Promise<CustomerResponse> {
  const response = await apiClient.get<CustomerResponse>(`/api/customers/${id}`, { signal })
  return response.data
}

/** POST /api/customers. A taken name rejects with code DUPLICATE. */
export async function createCustomer(request: CustomerRequest): Promise<CustomerResponse> {
  const response = await apiClient.post<CustomerResponse>('/api/customers', request)
  return response.data
}

/** PUT /api/customers/{id}: replaces every field. */
export async function updateCustomer(id: number, request: CustomerRequest): Promise<CustomerResponse> {
  const response = await apiClient.put<CustomerResponse>(`/api/customers/${id}`, request)
  return response.data
}

/** DELETE /api/customers/{id} (MANAGER and up). A customer with orders rejects with code IN_USE. */
export async function deleteCustomer(id: number): Promise<void> {
  await apiClient.delete(`/api/customers/${id}`)
}

/** Customers as Select options, sorted by name (one page of MAX_PAGE_SIZE, like getSupplierOptions). */
export async function getCustomerOptions(signal?: AbortSignal): Promise<SelectOption[]> {
  const page = await getCustomers({ page: 0, size: MAX_PAGE_SIZE, sort: 'name,asc' }, signal)
  return page.content.map(customer => ({ value: customer.id, label: customer.name }))
}
