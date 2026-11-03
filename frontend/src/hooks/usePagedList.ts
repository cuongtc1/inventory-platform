import type { TablePaginationConfig, TableProps } from 'antd'
import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import { DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE } from '../constants/app'
import type { PageQuery, PageResponse } from '../types/api'
import { errorMessage } from '../utils/errors'
import { sortOrderOf, toPageQuery, toTablePagination } from '../utils/table'

/** Loads one page. A function declared outside the component, so it is the same function on every render. */
export type PageLoader<T> = (query: PageQuery, filters: URLSearchParams, signal: AbortSignal) => Promise<PageResponse<T>>

/** Query parameters the hook itself uses; every other parameter is a filter. */
const PAGE = 'page'
const SIZE = 'size'
const SORT = 'sort'

type FilterValue = string | number | null | undefined

/** What usePagedList returns, for components that take the list as a prop (PagedTable, SearchFilter). */
export type PagedList<T> = ReturnType<typeof usePagedList<T>>

interface LoadResult<T> {
  key: string
  data?: PageResponse<T>
  error?: string
}

/**
 * Everything a list page needs: the current page from the API, loading and error state, and handlers for the
 * table and the filters (docs/conventions.md section 10).
 *
 * The list state lives in the URL: `/products?page=2&sort=salePrice,desc&status=ACTIVE`. So a reload, the browser's
 * Back button, a bookmark or a shared link show the same list, and a form can send the user back to it (see
 * useReturnTo). `page` in the URL counts from 1, like the pagination people see; the API counts from 0.
 *
 * @param load        fetches one page; must be declared outside the component (see PageLoader)
 * @param defaultSort used when the URL has no `sort`, e.g. "name,asc"
 */
export function usePagedList<T>(load: PageLoader<T>, defaultSort: string) {
  const [params, setParams] = useSearchParams()
  const [reloadCount, setReloadCount] = useState(0)
  const [result, setResult] = useState<LoadResult<T>>()

  const search = params.toString()
  const query = readQuery(params, defaultSort)
  // One key per request: when it differs from the last result's key, a request is still running
  const key = `${search}#${reloadCount}`

  useEffect(() => {
    const controller = new AbortController()
    const current = new URLSearchParams(search)
    const requestKey = `${search}#${reloadCount}`
    load(readQuery(current, defaultSort), current, controller.signal)
      .then(data => {
        // The page is past the end, e.g. after deleting the last row of the last page: show the last page instead
        if (data.content.length === 0 && data.page > 0 && data.totalPages > 0) {
          current.set(PAGE, String(data.totalPages))
          setParams(current, { replace: true })
          return
        }
        setResult({ key: requestKey, data })
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          setResult({ key: requestKey, error: errorMessage(error) })
        }
      })
    // Leaving the page or changing the URL before the answer arrives cancels the request
    return () => controller.abort()
  }, [search, reloadCount, load, defaultSort, setParams])

  const loading = result?.key !== key

  const pagination: TablePaginationConfig = toTablePagination(query, result?.data?.totalElements ?? 0)

  /** Table onChange: a new page, page size or sorted column. A new sort or page size starts again at page 1. */
  const onTableChange: TableProps<T>['onChange'] = (tablePagination, _filters, sorter, extra) => {
    const next = toPageQuery(tablePagination, sorter)
    const restart = extra.action === 'sort' || next.size !== query.size
    const updated = new URLSearchParams(params)
    setOrDelete(updated, PAGE, restart || next.page === 0 ? undefined : next.page + 1)
    setOrDelete(updated, SIZE, next.size === DEFAULT_PAGE_SIZE ? undefined : next.size)
    setOrDelete(updated, SORT, next.sort === defaultSort ? undefined : next.sort)
    setParams(updated)
  }

  /** Sets filters (empty values remove them) and goes back to page 1. */
  const setFilters = (values: Record<string, FilterValue>) => {
    const updated = new URLSearchParams(params)
    Object.entries(values).forEach(([name, value]) => setOrDelete(updated, name, value))
    updated.delete(PAGE)
    setParams(updated)
  }

  /** Removes every filter; keeps the page size and sort. */
  const resetFilters = () => {
    const updated = new URLSearchParams()
    ;[SIZE, SORT].forEach(name => setOrDelete(updated, name, params.get(name)))
    setParams(updated)
  }

  const filterNames = [...params.keys()].filter(name => ![PAGE, SIZE, SORT].includes(name))

  return {
    /** The last page received; kept while the next one loads, so the table doesn't flash empty. */
    data: result?.data,
    loading,
    /** Text to show when the last request failed. */
    error: loading ? undefined : result?.error,
    pagination,
    onTableChange,
    /** For a column's `sortOrder`: sortOrder('salePrice') is 'ascend', 'descend' or null. */
    sortOrder: (field: string) => sortOrderOf(query.sort, field),
    /** A filter's value from the URL, e.g. filter('status') → "ACTIVE". */
    filter: (name: string) => params.get(name) ?? undefined,
    hasFilters: filterNames.length > 0,
    setFilters,
    resetFilters,
    /** Loads the current page again, e.g. after a delete. */
    reload: () => setReloadCount(count => count + 1),
  }
}

/** URL parameters → API paging. Bad values (page=abc, size=5000) fall back to safe ones. */
function readQuery(params: URLSearchParams, defaultSort: string): PageQuery {
  const page = toPositiveInt(params.get(PAGE)) ?? 1
  const size = Math.min(toPositiveInt(params.get(SIZE)) ?? DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE)
  return { page: page - 1, size, sort: params.get(SORT) ?? defaultSort }
}

function toPositiveInt(value: string | null): number | undefined {
  const number = Number(value)
  return value !== null && Number.isInteger(number) && number > 0 ? number : undefined
}

function setOrDelete(params: URLSearchParams, name: string, value: FilterValue) {
  if (value === undefined || value === null || String(value).trim() === '') {
    params.delete(name)
  } else {
    params.set(name, String(value).trim())
  }
}
