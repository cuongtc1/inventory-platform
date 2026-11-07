import { Select, type TableColumnsType } from 'antd'
import { getStockMovements } from '../../api/stock'
import { DateRangeFilter } from '../../components/DateRangeFilter'
import { PagedTable } from '../../components/PagedTable'
import { PageHeader } from '../../components/PageHeader'
import { ProductCell } from '../../components/ProductCell'
import { SearchFilter } from '../../components/SearchFilter'
import { StatusBadge } from '../../components/StatusBadge'
import { WarehouseFilter } from '../../components/WarehouseFilter'
import { MOVEMENT_TYPE, MOVEMENT_TYPE_OPTIONS } from '../../constants/movementType'
import { useFilterWidth } from '../../hooks/useIsMobile'
import { type PageLoader, usePagedList } from '../../hooks/usePagedList'
import { texts } from '../../messages/texts'
import type { MovementType, StockMovementResponse } from '../../types/stock'
import { EMPTY_VALUE, formatDateTime, formatQuantityChange, parseApiDate, toApiDate } from '../../utils/format'
import { toId } from '../../utils/table'

const DEFAULT_SORT = 'createdAt,desc'

/** URL filters → API filters. Outside the component, so usePagedList gets the same function on every render. */
const loadMovements: PageLoader<StockMovementResponse> = (query, filters, signal) =>
  getStockMovements(
    {
      ...query,
      search: filters.get('search') ?? undefined,
      warehouseId: toId(filters.get('warehouseId')),
      movementType: toMovementType(filters.get('movementType')),
      from: toDateFilter(filters.get('from')),
      to: toDateFilter(filters.get('to')),
    },
    signal,
  )

/** Stock Movements, like the mockup: the history of stock, newest first. Read-only for everyone. */
export function StockMovementListPage() {
  const filterWidth = useFilterWidth()
  const list = usePagedList(loadMovements, DEFAULT_SORT)

  const fields = texts.stockMovement.fields
  const columns: TableColumnsType<StockMovementResponse> = [
    {
      title: fields.time,
      dataIndex: 'createdAt',
      sorter: true,
      sortOrder: list.sortOrder('createdAt'),
      render: (createdAt: string) => formatDateTime(createdAt),
    },
    {
      title: fields.product,
      dataIndex: 'productName',
      sorter: true,
      sortOrder: list.sortOrder('productName'),
      render: (name: string, movement) => <ProductCell name={name} sku={movement.productSku} />,
    },
    {
      title: fields.warehouse,
      // The backend sorts by the warehouse's code; `key` is the sort field, render shows the code
      key: 'warehouse.code',
      sorter: true,
      sortOrder: list.sortOrder('warehouse.code'),
      render: (_, movement) => movement.warehouseCode,
    },
    {
      title: fields.type,
      dataIndex: 'movementType',
      sorter: true,
      sortOrder: list.sortOrder('movementType'),
      render: (type: MovementType) => <StatusBadge value={type} display={MOVEMENT_TYPE} />,
    },
    {
      title: fields.quantity,
      dataIndex: 'quantity',
      align: 'right',
      sorter: true,
      sortOrder: list.sortOrder('quantity'),
      render: (quantity: number) => formatQuantityChange(quantity),
    },
    {
      title: fields.reference,
      dataIndex: 'reference',
      sorter: true,
      sortOrder: list.sortOrder('reference'),
      render: (reference: string | null) => reference ?? EMPTY_VALUE,
    },
    { title: fields.by, dataIndex: 'createdBy', sorter: true, sortOrder: list.sortOrder('createdBy') },
  ]

  return (
    <>
      <PageHeader title={texts.stockMovement.title} />
      <PagedTable
        list={list}
        columns={columns}
        filters={
          <>
            <SearchFilter list={list} placeholder={texts.stockMovement.searchPlaceholder} />
            <WarehouseFilter list={list} />
            <Select
              value={toMovementType(list.filter('movementType'))}
              options={MOVEMENT_TYPE_OPTIONS}
              placeholder={texts.stockMovement.allTypes}
              allowClear
              onChange={(movementType?: MovementType) => list.setFilters({ movementType })}
              style={filterWidth(160)}
            />
            <DateRangeFilter list={list} />
          </>
        }
      />
    </>
  )
}

/** Only a type the frontend knows; anything else in the URL is ignored. */
function toMovementType(value: string | null | undefined): MovementType | undefined {
  return value !== null && value !== undefined && value in MOVEMENT_TYPE ? (value as MovementType) : undefined
}

/** Only a real date; anything else in the URL (from=abc) is ignored. */
function toDateFilter(value: string | null): string | undefined {
  const date = parseApiDate(value)
  return date && toApiDate(date)
}
