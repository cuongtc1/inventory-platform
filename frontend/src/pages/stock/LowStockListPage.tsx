import type { TableColumnsType } from 'antd'
import { getStockLevels } from '../../api/stock'
import { PagedTable } from '../../components/PagedTable'
import { PageHeader } from '../../components/PageHeader'
import { ProductCell } from '../../components/ProductCell'
import { SearchFilter } from '../../components/SearchFilter'
import { StatusBadge } from '../../components/StatusBadge'
import { WarehouseFilter } from '../../components/WarehouseFilter'
import { usePagedList, type PageLoader } from '../../hooks/usePagedList'
import { texts } from '../../messages/texts'
import type { StockLevelResponse } from '../../types/stock'
import type { EnumDisplay } from '../../types/ui'
import { toId } from '../../utils/table'

const DEFAULT_SORT = 'productName,asc'

/** Every row here is low, so this page has one status. Later statuses (ordered, ...) would join it. */
const LOW_STOCK_STATUS: Record<'LOW', EnumDisplay> = {
  LOW: { label: texts.lowStock.status, tone: 'red' },
}

/** The same list as Stock Levels, with the low-stock filter always on. */
const loadLowStock: PageLoader<StockLevelResponse> = (query, filters, signal) =>
  getStockLevels(
    {
      ...query,
      lowStock: true,
      search: filters.get('search') ?? undefined,
      warehouseId: toId(filters.get('warehouseId')),
    },
    signal,
  )

/**
 * Low-Stock Alerts: products whose available stock is at or below the reorder level.
 *
 * Differs from the mockup: no Supplier column, because inventory-service doesn't know the supplier (it lives in
 * product-service). A Warehouse column instead: stock is low per warehouse, so a product can appear twice.
 */
export function LowStockListPage() {
  const list = usePagedList(loadLowStock, DEFAULT_SORT)

  const fields = texts.stockLevel.fields
  const columns: TableColumnsType<StockLevelResponse> = [
    {
      title: fields.product,
      dataIndex: 'productName',
      sorter: true,
      sortOrder: list.sortOrder('productName'),
      render: (name: string, level) => <ProductCell name={name} sku={level.productSku} />,
    },
    {
      title: fields.warehouse,
      key: 'warehouse.code',
      sorter: true,
      sortOrder: list.sortOrder('warehouse.code'),
      render: (_, level) => level.warehouseCode,
    },
    { title: fields.available, dataIndex: 'available', align: 'right' },
    {
      title: fields.reorderLevel,
      dataIndex: 'reorderLevel',
      align: 'right',
      sorter: true,
      sortOrder: list.sortOrder('reorderLevel'),
    },
    {
      title: fields.status,
      key: 'status',
      render: () => <StatusBadge value="LOW" display={LOW_STOCK_STATUS} />,
    },
  ]

  return (
    <>
      <PageHeader title={texts.lowStock.title} />
      <PagedTable
        list={list}
        columns={columns}
        filters={
          <>
            <SearchFilter list={list} placeholder={texts.stockLevel.searchPlaceholder} />
            <WarehouseFilter list={list} />
          </>
        }
      />
    </>
  )
}
