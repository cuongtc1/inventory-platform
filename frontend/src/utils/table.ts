import type { TablePaginationConfig, TableProps } from 'antd'
import { DEFAULT_PAGE_SIZE, PAGE_SIZE_OPTIONS } from '../constants/app'
import type { PageQuery } from '../types/api'

/**
 * Translating between Ant Design's Table and our list API (docs/conventions.md sections 4 and 10).
 * Ant Design counts pages from 1, the backend from 0; Ant Design says "ascend", the backend "asc".
 */

/** The sorter argument of a Table's onChange, taken from Ant Design's own types instead of a deep import. */
type TableSorter<T> = Parameters<NonNullable<TableProps<T>['onChange']>>[2]

/** A filter from the URL: "12" → 12; anything else (missing, "abc") → undefined, i.e. no filter. */
export function toId(value: string | null | undefined): number | undefined {
  const id = Number(value)
  return value && Number.isInteger(id) && id > 0 ? id : undefined
}

/**
 * Table state → API paging: `current` 3 becomes `page` 2. Without a sorted column, `defaultSort` (or the
 * backend's default) is used. A column's sort field is its `dataIndex` (nested: ["category", "name"] →
 * "category.name"), or its `key`.
 */
export function toPageQuery<T>(
  pagination: TablePaginationConfig,
  sorter?: TableSorter<T>,
  defaultSort?: string,
): PageQuery {
  const query: PageQuery = {
    page: Math.max((pagination.current ?? 1) - 1, 0),
    size: pagination.pageSize ?? DEFAULT_PAGE_SIZE,
  }
  const sort = toSort(sorter) ?? defaultSort
  return sort ? { ...query, sort } : query
}

/**
 * API paging → Table pagination: `page` 2 becomes `current` 3. Built from the requested page, not the answer,
 * so the pager moves at once while the next page loads.
 */
export function toTablePagination(query: PageQuery, total: number): TablePaginationConfig {
  return {
    current: query.page + 1,
    pageSize: query.size,
    total,
    showSizeChanger: true,
    pageSizeOptions: [...PAGE_SIZE_OPTIONS],
  }
}

/**
 * API sort → a column's `sortOrder`, so the column header shows the current sort: sortOrderOf("salePrice,desc",
 * "salePrice") is "descend". The field is the column's `dataIndex` (or `key`) as used by toPageQuery.
 */
export function sortOrderOf(sort: string | undefined, field: string): 'ascend' | 'descend' | null {
  const [sortField, direction] = (sort ?? '').split(',')
  if (sortField !== field) {
    return null
  }
  return direction === 'desc' ? 'descend' : 'ascend'
}

function toSort<T>(sorter?: TableSorter<T>): string | undefined {
  // Several sorted columns come as an array; this app sorts by one column at a time
  const single = Array.isArray(sorter) ? sorter[0] : sorter
  if (!single?.order) {
    return undefined
  }
  const field = Array.isArray(single.field) ? single.field.join('.') : (single.field ?? single.columnKey)
  if (field === undefined || field === null) {
    return undefined
  }
  return `${String(field)},${single.order === 'ascend' ? 'asc' : 'desc'}`
}
