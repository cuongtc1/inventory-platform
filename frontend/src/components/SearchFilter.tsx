import { Input } from 'antd'
import type { PagedList } from '../hooks/usePagedList'
import { useIsMobile } from '../hooks/useIsMobile'

interface SearchFilterProps<T> {
  list: PagedList<T>
  placeholder: string
}

/**
 * The search box of a list: searches on Enter or the button (not on every key press) and keeps the text in the
 * URL as `?search=...`. Full width on phones.
 */
export function SearchFilter<T>({ list, placeholder }: SearchFilterProps<T>) {
  const isMobile = useIsMobile()
  const search = list.filter('search')

  return (
    <Input.Search
      // A new key when the URL changes (Reset, Back) makes React create the input again with the URL's value
      key={search ?? ''}
      defaultValue={search}
      placeholder={placeholder}
      allowClear
      onSearch={value => list.setFilters({ search: value })}
      style={{ width: isMobile ? '100%' : 260 }}
    />
  )
}
