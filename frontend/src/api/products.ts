import { MAX_PAGE_SIZE } from '../constants/app'
import type { PageQuery, PageResponse } from '../types/api'
import type { ProductFilter, ProductRequest, ProductResponse } from '../types/product'
import type { SelectOption } from '../types/ui'
import { apiClient } from './client'

/** Calls to /api/products (product-service). Pages use these functions, never Axios directly. */

/** GET /api/products: one page, with optional filters (combined with AND). */
export async function getProducts(
  query: PageQuery & ProductFilter,
  signal?: AbortSignal,
): Promise<PageResponse<ProductResponse>> {
  const response = await apiClient.get<PageResponse<ProductResponse>>('/api/products', { params: query, signal })
  return response.data
}

/** GET /api/products/{id}. A missing product rejects with an ApiError with code NOT_FOUND. */
export async function getProduct(id: number, signal?: AbortSignal): Promise<ProductResponse> {
  const response = await apiClient.get<ProductResponse>(`/api/products/${id}`, { signal })
  return response.data
}

/** POST /api/products. A taken SKU rejects with code DUPLICATE. */
export async function createProduct(request: ProductRequest): Promise<ProductResponse> {
  const response = await apiClient.post<ProductResponse>('/api/products', request)
  return response.data
}

/** PUT /api/products/{id}: replaces every field. */
export async function updateProduct(id: number, request: ProductRequest): Promise<ProductResponse> {
  const response = await apiClient.put<ProductResponse>(`/api/products/${id}`, request)
  return response.data
}

/** DELETE /api/products/{id} (hard delete). */
export async function deleteProduct(id: number): Promise<void> {
  await apiClient.delete(`/api/products/${id}`)
}

/**
 * Products as Select options, sorted by name. One page of MAX_PAGE_SIZE like getCategoryOptions, but a catalog can
 * outgrow it: past 100 products this needs a search that asks the backend.
 */
export async function getProductOptions(signal?: AbortSignal): Promise<SelectOption[]> {
  const page = await getProducts({ page: 0, size: MAX_PAGE_SIZE, sort: 'name,asc' }, signal)
  return page.content.map(product => ({ value: product.id, label: `${product.sku} ${product.name}` }))
}

/**
 * Products that can be sold (ACTIVE), sorted by name, with their price: the product choices of a new sales order.
 * The same first-100 limit as getProductOptions.
 */
export async function getSellableProducts(signal?: AbortSignal): Promise<ProductResponse[]> {
  const page = await getProducts({ page: 0, size: MAX_PAGE_SIZE, sort: 'name,asc', status: 'ACTIVE' }, signal)
  return page.content
}
