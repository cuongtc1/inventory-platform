import { PlusOutlined } from '@ant-design/icons'
import { App, Button, Flex, type TableColumnsType } from 'antd'
import { Link, useNavigate } from 'react-router'
import { deleteWarehouse, getWarehouses } from '../../api/warehouses'
import { DeleteButton } from '../../components/DeleteButton'
import { EditButton } from '../../components/EditButton'
import { PagedTable } from '../../components/PagedTable'
import { PageHeader } from '../../components/PageHeader'
import { SearchFilter } from '../../components/SearchFilter'
import { ROUTES } from '../../constants/routes'
import { type PageLoader, usePagedList } from '../../hooks/usePagedList'
import { useReturnState } from '../../hooks/useReturnTo'
import { texts } from '../../messages/texts'
import type { WarehouseResponse } from '../../types/warehouse'
import { errorMessage } from '../../utils/errors'
import { EMPTY_VALUE } from '../../utils/format'

const DEFAULT_SORT = 'code,asc'

/** URL filters → API filters. Outside the component, so usePagedList gets the same function on every render. */
const loadWarehouses: PageLoader<WarehouseResponse> = (query, filters, signal) =>
  getWarehouses({ ...query, search: filters.get('search') ?? undefined }, signal)

/** Warehouses list: Code, Name, Address, SKUs, Edit / Delete. Same pattern as SupplierListPage. */
export function WarehouseListPage() {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const returnState = useReturnState()
  const list = usePagedList(loadWarehouses, DEFAULT_SORT)

  const onDelete = async (warehouse: WarehouseResponse) => {
    try {
      await deleteWarehouse(warehouse.id)
      message.success(texts.warehouse.deleted(warehouse.code))
      list.reload()
    } catch (error) {
      // IN_USE (stock left): the backend's text says what to do first
      message.error(errorMessage(error))
    }
  }

  const fields = texts.warehouse.fields
  const columns: TableColumnsType<WarehouseResponse> = [
    {
      title: fields.code,
      dataIndex: 'code',
      sorter: true,
      sortOrder: list.sortOrder('code'),
      render: (code: string, warehouse) => (
        <Link to={ROUTES.warehouseEdit(warehouse.id)} state={returnState}>
          {code}
        </Link>
      ),
    },
    { title: fields.name, dataIndex: 'name', sorter: true, sortOrder: list.sortOrder('name') },
    {
      title: fields.address,
      dataIndex: 'address',
      sorter: true,
      sortOrder: list.sortOrder('address'),
      render: (address: string | null) => address ?? EMPTY_VALUE,
    },
    // Counted by the backend for each row, not a column it can sort by
    { title: fields.skuCount, dataIndex: 'skuCount', align: 'right' },
    {
      title: texts.common.fields.actions,
      key: 'actions',
      render: (_, warehouse) => (
        <Flex gap={6}>
          <EditButton to={ROUTES.warehouseEdit(warehouse.id)} />
          <DeleteButton title={texts.warehouse.confirmDelete(warehouse.code)} onConfirm={() => onDelete(warehouse)} />
        </Flex>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        title={texts.warehouse.title}
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate(ROUTES.warehouseNew, { state: returnState })}>
            {texts.warehouse.newButton}
          </Button>
        }
      />
      <PagedTable
        list={list}
        columns={columns}
        filters={<SearchFilter list={list} placeholder={texts.warehouse.searchPlaceholder} />}
      />
    </>
  )
}
