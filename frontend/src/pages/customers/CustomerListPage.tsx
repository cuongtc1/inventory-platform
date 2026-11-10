import { PlusOutlined } from '@ant-design/icons'
import { App, Button, Flex, type TableColumnsType } from 'antd'
import { Link, useNavigate } from 'react-router'
import { deleteCustomer, getCustomers } from '../../api/customers'
import { DeleteButton } from '../../components/DeleteButton'
import { EditButton } from '../../components/EditButton'
import { PagedTable } from '../../components/PagedTable'
import { PageHeader } from '../../components/PageHeader'
import { SearchFilter } from '../../components/SearchFilter'
import { ROUTES } from '../../constants/routes'
import { type PageLoader, usePagedList } from '../../hooks/usePagedList'
import { useReturnState } from '../../hooks/useReturnTo'
import { useHasRole } from '../../hooks/useSession'
import { texts } from '../../messages/texts'
import type { CustomerResponse } from '../../types/customer'
import { errorMessage } from '../../utils/errors'
import { EMPTY_VALUE } from '../../utils/format'

const DEFAULT_SORT = 'name,asc'

/** URL filters → API filters. Outside the component, so usePagedList gets the same function on every render. */
const loadCustomers: PageLoader<CustomerResponse> = (query, filters, signal) =>
  getCustomers({ ...query, search: filters.get('search') ?? undefined }, signal)

/**
 * Customers, like the mockup: Name, Email, Phone, Orders, Edit / Delete. Everyone adds and edits customers (staff
 * take the orders); Delete is shown to managers only.
 */
export function CustomerListPage() {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const returnState = useReturnState()
  const canDelete = useHasRole('MANAGER')
  const list = usePagedList(loadCustomers, DEFAULT_SORT)

  const onDelete = async (customer: CustomerResponse) => {
    try {
      await deleteCustomer(customer.id)
      message.success(texts.customer.deleted(customer.name))
      list.reload()
    } catch (error) {
      // IN_USE (has orders): the backend's text says why
      message.error(errorMessage(error))
    }
  }

  const fields = texts.customer.fields
  const columns: TableColumnsType<CustomerResponse> = [
    {
      title: fields.name,
      dataIndex: 'name',
      sorter: true,
      sortOrder: list.sortOrder('name'),
      render: (name: string, customer) => (
        <Link to={ROUTES.customerEdit(customer.id)} state={returnState}>
          {name}
        </Link>
      ),
    },
    {
      title: fields.email,
      dataIndex: 'email',
      sorter: true,
      sortOrder: list.sortOrder('email'),
      render: (email: string | null) => (email ? <a href={`mailto:${email}`}>{email}</a> : EMPTY_VALUE),
    },
    {
      title: fields.phone,
      dataIndex: 'phone',
      sorter: true,
      sortOrder: list.sortOrder('phone'),
      render: (phone: string | null) => phone ?? EMPTY_VALUE,
    },
    // Counted by the backend for each row, not a column it can sort by
    { title: fields.orderCount, dataIndex: 'orderCount', align: 'right' },
    {
      title: texts.common.fields.actions,
      key: 'actions',
      render: (_, customer) => (
        <Flex gap={6}>
          <EditButton to={ROUTES.customerEdit(customer.id)} />
          {canDelete && (
            <DeleteButton title={texts.customer.confirmDelete(customer.name)} onConfirm={() => onDelete(customer)} />
          )}
        </Flex>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        title={texts.customer.title}
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate(ROUTES.customerNew, { state: returnState })}>
            {texts.customer.newButton}
          </Button>
        }
      />
      <PagedTable
        list={list}
        columns={columns}
        filters={<SearchFilter list={list} placeholder={texts.customer.searchPlaceholder} />}
      />
    </>
  )
}
