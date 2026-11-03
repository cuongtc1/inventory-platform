import { MAX_PAGE_SIZE } from '../constants/app'
import type { PageQuery, PageResponse } from '../types/api'
import type { CategoryResponse } from '../types/category'
import type { SelectOption } from '../types/ui'
import { apiClient } from './client'

/**
 * Calls to /api/categories (product-service). Pages use these functions, never Axios directly.
 * T2.4 adds get, create, update and delete.
 */

/** GET /api/categories: one page, optionally filtered by part of the name. */
export async function getCategories(
  query: PageQuery & { search?: string },
  signal?: AbortSignal,
): Promise<PageResponse<CategoryResponse>> {
  const response = await apiClient.get<PageResponse<CategoryResponse>>('/api/categories', { params: query, signal })
  return response.data
}

/**
 * Every category as Select options, sorted by name. One request of the largest page the API allows
 * (MAX_PAGE_SIZE); enough for this project's catalog. A catalog with more would need a searching Select.
 */
export async function getCategoryOptions(signal?: AbortSignal): Promise<SelectOption[]> {
  const page = await getCategories({ page: 0, size: MAX_PAGE_SIZE, sort: 'name,asc' }, signal)
  return page.content.map(category => ({ value: category.id, label: category.name }))
}
