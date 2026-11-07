import { EditOutlined, PlusOutlined } from '@ant-design/icons'
import { Button, type TableColumnsType, Tooltip } from 'antd'
import { useNavigate } from 'react-router'
import { getStockLevels } from '../../api/stock'
import { PagedTable } from '../../components/PagedTable'
import { PageHeader } from '../../components/PageHeader'
import { ProductCell } from '../../components/ProductCell'
import { SearchFilter } from '../../components/SearchFilter'
import { StockLevelBar } from '../../components/StockLevelBar'
import { WarehouseFilter } from '../../components/WarehouseFilter'
import { ROUTES } from '../../constants/routes'
import { useIsMobile } from '../../hooks/useIsMobile'
import { type PageLoader, usePagedList } from '../../hooks/usePagedList'
import { useReturnState } from '../../hooks/useReturnTo'
import { useHasRole } from '../../hooks/useSession'
import { texts } from '../../messages/texts'
import type { StockLevelResponse } from '../../types/stock'
import { toId } from '../../utils/table'

const DEFAULT_SORT = 'productName,asc'

/** URL filters → API filters. Outside the component, so usePagedList gets the same function on every render. */
const loadStockLevels: PageLoader<StockLevelResponse> = (query, filters, signal) =>
  getStockLevels(
    { ...query, search: filters.get('search') ?? undefined, warehouseId: toId(filters.get('warehouseId')) },
    signal,
  )

/**
 * Stock Levels, like the mockup: one row per product and warehouse. Managers get "Adjust Stock" and an Adjust
 * button per row, which opens the form with that product and warehouse already chosen.
 */
export function StockLevelListPage() {
  const navigate = useNavigate()
  const isMobile = useIsMobile()
  const returnState = useReturnState()
  const canAdjust = useHasRole('MANAGER')
  const list = usePagedList(loadStockLevels, DEFAULT_SORT)

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
      // The backend sorts by the warehouse's code; `key` is the sort field, render shows the code
      key: 'warehouse.code',
      sorter: true,
      sortOrder: list.sortOrder('warehouse.code'),
      render: (_, level) => level.warehouseCode,
    },
    {
      title: fields.onHand,
      dataIndex: 'onHand',
      align: 'right',
      sorter: true,
      sortOrder: list.sortOrder('onHand'),
    },
    {
      title: fields.reserved,
      dataIndex: 'reserved',
      align: 'right',
      sorter: true,
      sortOrder: list.sortOrder('reserved'),
    },
    // Worked out by the backend from on hand and reserved, not a column it can sort by
    { title: fields.available, dataIndex: 'available', align: 'right' },
    { title: fields.level, key: 'level', render: (_, level) => <StockLevelBar level={level} /> },
  ]
  // Staff only read (hiding only: the backend refuses their changes anyway)
  if (canAdjust) {
    columns.push({
      title: texts.common.fields.actions,
      key: 'actions',
      render: (_, level) => (
        <Tooltip title={isMobile ? texts.stockLevel.adjustRow : undefined}>
          <Button
            size="small"
            icon={<EditOutlined />}
            aria-label={texts.stockLevel.adjustRow}
            onClick={() => navigate(ROUTES.stockAdjustFor(level.productId, level.warehouseId), { state: returnState })}
          >
            {!isMobile && texts.stockLevel.adjustRow}
          </Button>
        </Tooltip>
      ),
    })
  }

  return (
    <>
      <PageHeader
        title={texts.stockLevel.title}
        extra={
          canAdjust && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate(ROUTES.stockAdjust, { state: returnState })}>
              {texts.stockLevel.adjustButton}
            </Button>
          )
        }
      />
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
