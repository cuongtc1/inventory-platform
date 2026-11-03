import { MAX_PAGE_SIZE } from '../constants/app'
import type { PageQuery, PageResponse } from '../types/api'
import type { CategoryRequest, CategoryResponse } from '../types/category'
import type { SelectOption } from '../types/ui'
import { apiClient } from './client'

/** Calls to /api/categories (product-service). Pages use these functions, never Axios directly. */

/** GET /api/categories: one page, optionally filtered by part of the name. */
export async function getCategories(
  query: PageQuery & { search?: string },
  signal?: AbortSignal,
): Promise<PageResponse<CategoryResponse>> {
  const response = await apiClient.get<PageResponse<CategoryResponse>>('/api/categories', { params: query, signal })
  return response.data
}

/** GET /api/categories/{id}. A missing category rejects with an ApiError with code NOT_FOUND. */
export async function getCategory(id: number, signal?: AbortSignal): Promise<CategoryResponse> {
  const response = await apiClient.get<CategoryResponse>(`/api/categories/${id}`, { signal })
  return response.data
}

/** POST /api/categories. A taken name rejects with code DUPLICATE. */
export async function createCategory(request: CategoryRequest): Promise<CategoryResponse> {
  const response = await apiClient.post<CategoryResponse>('/api/categories', request)
  return response.data
}

/**
 * PUT /api/categories/{id}. Moving a category under itself or one of its subcategories rejects with code
 * BAD_REQUEST.
 */
export async function updateCategory(id: number, request: CategoryRequest): Promise<CategoryResponse> {
  const response = await apiClient.put<CategoryResponse>(`/api/categories/${id}`, request)
  return response.data
}

/** DELETE /api/categories/{id}. A category with subcategories or products rejects with code IN_USE. */
export async function deleteCategory(id: number): Promise<void> {
  await apiClient.delete(`/api/categories/${id}`)
}

/**
 * Every category as Select options, sorted by name. One request of the largest page the API allows
 * (MAX_PAGE_SIZE); enough for this project's catalog. A catalog with more would need a searching Select.
 */
export async function getCategoryOptions(signal?: AbortSignal): Promise<SelectOption[]> {
  const page = await getCategories({ page: 0, size: MAX_PAGE_SIZE, sort: 'name,asc' }, signal)
  return page.content.map(category => ({ value: category.id, label: category.name }))
}
