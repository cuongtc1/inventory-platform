import { PlusOutlined } from '@ant-design/icons'
import { App, Button, Flex, type TableColumnsType } from 'antd'
import { Link, useNavigate } from 'react-router'
import { deleteSupplier, getSuppliers } from '../../api/suppliers'
import { DeleteButton } from '../../components/DeleteButton'
import { EditButton } from '../../components/EditButton'
import { PagedTable } from '../../components/PagedTable'
import { PageHeader } from '../../components/PageHeader'
import { SearchFilter } from '../../components/SearchFilter'
import { ROUTES } from '../../constants/routes'
import { type PageLoader, usePagedList } from '../../hooks/usePagedList'
import { useReturnState } from '../../hooks/useReturnTo'
import { texts } from '../../messages/texts'
import type { SupplierResponse } from '../../types/supplier'
import { errorMessage } from '../../utils/errors'
import { EMPTY_VALUE } from '../../utils/format'

const DEFAULT_SORT = 'name,asc'

/** URL filters → API filters. Outside the component, so usePagedList gets the same function on every render. */
const loadSuppliers: PageLoader<SupplierResponse> = (query, filters, signal) =>
  getSuppliers({ ...query, search: filters.get('search') ?? undefined }, signal)

/** Suppliers list: Name, Contact, Email, Phone, Edit / Delete. Same pattern as ProductListPage. */
export function SupplierListPage() {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const returnState = useReturnState()
  const list = usePagedList(loadSuppliers, DEFAULT_SORT)

  const onDelete = async (supplier: SupplierResponse) => {
    try {
      await deleteSupplier(supplier.id)
      message.success(texts.supplier.deleted(supplier.name))
      list.reload()
    } catch (error) {
      // IN_USE (products left): the backend's text says what to do first
      message.error(errorMessage(error))
    }
  }

  const fields = texts.supplier.fields
  const columns: TableColumnsType<SupplierResponse> = [
    {
      title: fields.name,
      dataIndex: 'name',
      sorter: true,
      sortOrder: list.sortOrder('name'),
      render: (name: string, supplier) => (
        <Link to={ROUTES.supplierEdit(supplier.id)} state={returnState}>
          {name}
        </Link>
      ),
    },
    {
      title: fields.contactName,
      dataIndex: 'contactName',
      sorter: true,
      sortOrder: list.sortOrder('contactName'),
      render: (contactName: string | null) => contactName ?? EMPTY_VALUE,
    },
    {
      title: fields.email,
      dataIndex: 'email',
      sorter: true,
      sortOrder: list.sortOrder('email'),
      // mailto: opens the viewer's email program
      render: (email: string | null) => (email ? <a href={`mailto:${email}`}>{email}</a> : EMPTY_VALUE),
    },
    {
      title: fields.phone,
      dataIndex: 'phone',
      sorter: true,
      sortOrder: list.sortOrder('phone'),
      render: (phone: string | null) => phone ?? EMPTY_VALUE,
    },
    {
      title: texts.common.fields.actions,
      key: 'actions',
      render: (_, supplier) => (
        <Flex gap={6}>
          <EditButton to={ROUTES.supplierEdit(supplier.id)} />
          <DeleteButton title={texts.supplier.confirmDelete(supplier.name)} onConfirm={() => onDelete(supplier)} />
        </Flex>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        title={texts.supplier.title}
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate(ROUTES.supplierNew, { state: returnState })}>
            {texts.supplier.newButton}
          </Button>
        }
      />
      <PagedTable
        list={list}
        columns={columns}
        filters={<SearchFilter list={list} placeholder={texts.supplier.searchPlaceholder} />}
      />
    </>
  )
}
