import { PlusOutlined } from '@ant-design/icons'
import { Button, Select, type TableColumnsType } from 'antd'
import { Link, useNavigate } from 'react-router'
import { getCustomerOptions } from '../../api/customers'
import { getSalesOrders } from '../../api/salesOrders'
import { PagedTable } from '../../components/PagedTable'
import { PageHeader } from '../../components/PageHeader'
import { SearchFilter } from '../../components/SearchFilter'
import { StatusBadge } from '../../components/StatusBadge'
import { ORDER_STATUS, ORDER_STATUS_OPTIONS } from '../../constants/orderStatus'
import { ROUTES } from '../../constants/routes'
import { useFilterWidth } from '../../hooks/useIsMobile'
import { type PageLoader, usePagedList } from '../../hooks/usePagedList'
import { useReturnState } from '../../hooks/useReturnTo'
import { useSelectOptions } from '../../hooks/useSelectOptions'
import { texts } from '../../messages/texts'
import type { OrderStatus, SalesOrderSummaryResponse } from '../../types/salesOrder'
import { formatDate, formatMoney } from '../../utils/format'
import { toId } from '../../utils/table'

const DEFAULT_SORT = 'createdAt,desc'

/** URL filters → API filters. Outside the component, so usePagedList gets the same function on every render. */
const loadSalesOrders: PageLoader<SalesOrderSummaryResponse> = (query, filters, signal) =>
  getSalesOrders(
    {
      ...query,
      search: filters.get('search') ?? undefined,
      customerId: toId(filters.get('customerId')),
      status: toStatus(filters.get('status')),
    },
    signal,
  )

/** Sales Orders, like the mockup: Order, Customer, Date, Total, Status, newest first. Everyone may place orders. */
export function SalesOrderListPage() {
  const navigate = useNavigate()
  const filterWidth = useFilterWidth()
  const returnState = useReturnState()
  const list = usePagedList(loadSalesOrders, DEFAULT_SORT)
  const customers = useSelectOptions(getCustomerOptions)

  const fields = texts.salesOrder.fields
  const columns: TableColumnsType<SalesOrderSummaryResponse> = [
    {
      title: fields.orderNumber,
      dataIndex: 'orderNumber',
      sorter: true,
      sortOrder: list.sortOrder('orderNumber'),
      render: (orderNumber: string, order) => (
        <Link to={ROUTES.salesOrderDetail(order.id)} state={returnState}>
          {orderNumber}
        </Link>
      ),
    },
    {
      title: fields.customer,
      // The backend sorts by the customer's name; `key` is the sort field, render shows the name
      key: 'customer.name',
      sorter: true,
      sortOrder: list.sortOrder('customer.name'),
      render: (_, order) => order.customerName,
    },
    {
      title: fields.date,
      dataIndex: 'createdAt',
      sorter: true,
      sortOrder: list.sortOrder('createdAt'),
      render: (createdAt: string) => formatDate(createdAt),
    },
    {
      title: fields.total,
      dataIndex: 'totalAmount',
      align: 'right',
      sorter: true,
      sortOrder: list.sortOrder('totalAmount'),
      render: (total: number) => formatMoney(total),
    },
    {
      title: fields.status,
      dataIndex: 'status',
      sorter: true,
      sortOrder: list.sortOrder('status'),
      render: (status: OrderStatus) => <StatusBadge value={status} display={ORDER_STATUS} />,
    },
  ]

  return (
    <>
      <PageHeader
        title={texts.salesOrder.title}
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate(ROUTES.salesOrderNew, { state: returnState })}>
            {texts.salesOrder.newButton}
          </Button>
        }
      />
      <PagedTable
        list={list}
        columns={columns}
        filters={
          <>
            <SearchFilter list={list} placeholder={texts.salesOrder.searchPlaceholder} />
            <Select
              value={toId(list.filter('customerId'))}
              options={customers.options}
              loading={customers.loading}
              placeholder={texts.salesOrder.allCustomers}
              allowClear
              showSearch={{ optionFilterProp: 'label' }}
              onChange={(customerId?: number) => list.setFilters({ customerId })}
              style={filterWidth(220)}
            />
            <Select
              value={toStatus(list.filter('status'))}
              options={ORDER_STATUS_OPTIONS}
              placeholder={texts.common.filters.allStatuses}
              allowClear
              onChange={(status?: OrderStatus) => list.setFilters({ status })}
              style={filterWidth(160)}
            />
          </>
        }
      />
    </>
  )
}

/** Only a status the frontend knows; anything else in the URL is ignored. */
function toStatus(value: string | null | undefined): OrderStatus | undefined {
  return value !== null && value !== undefined && value in ORDER_STATUS ? (value as OrderStatus) : undefined
}
