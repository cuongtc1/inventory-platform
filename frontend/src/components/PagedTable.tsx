import { Alert, Button, Card, Flex, Table, type TableColumnsType } from 'antd'
import type { ReactNode } from 'react'
import type { PagedList } from '../hooks/usePagedList'
import { useIsMobile } from '../hooks/useIsMobile'
import { texts } from '../messages/texts'

interface PagedTableProps<T> {
  /** The list from usePagedList: data, loading, error, paging and sort. */
  list: PagedList<T>
  columns: TableColumnsType<T>
  /** The filter controls (SearchFilter, Selects); a Reset button is added when a filter is set. */
  filters: ReactNode
}

/**
 * The mockup's list card (docs/conventions.md section 10, "List pages"): filters with Reset, an error with
 * "Try again", and the table with backend paging and sorting. The page supplies the columns and filters.
 */
export function PagedTable<T extends { id: number }>({ list, columns, filters }: PagedTableProps<T>) {
  const isMobile = useIsMobile()

  return (
    <Card>
      <Flex gap={10} wrap style={{ marginBottom: 14 }}>
        {filters}
        {list.hasFilters && <Button onClick={list.resetFilters}>{texts.common.actions.reset}</Button>}
      </Flex>

      {list.error && (
        <Alert
          type="error"
          showIcon
          title={list.error}
          action={<Button size="small" onClick={list.reload}>{texts.common.actions.retry}</Button>}
          style={{ marginBottom: 14 }}
        />
      )}

      <Table
        rowKey="id"
        columns={columns}
        dataSource={list.data?.content}
        loading={list.loading}
        pagination={{
          ...list.pagination,
          showTotal: (total, [from, to]) => texts.common.pageRange(from, to, total),
          size: isMobile ? 'small' : undefined,
        }}
        onChange={list.onTableChange}
        // Only asc / desc: there's always a sort (the page's default), so "unsorted" isn't offered
        sortDirections={['ascend', 'descend']}
        // Wide table on a narrow screen: it scrolls sideways inside the card, not the whole page
        scroll={{ x: 'max-content' }}
      />
    </Card>
  )
}
