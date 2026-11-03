import { PlusOutlined } from '@ant-design/icons'
import { App, Button, Flex, Select, type TableColumnsType } from 'antd'
import { Link, useNavigate } from 'react-router'
import { getCategoryOptions } from '../../api/categories'
import { deleteProduct, getProducts } from '../../api/products'
import { getSupplierOptions } from '../../api/suppliers'
import { DeleteButton } from '../../components/DeleteButton'
import { EditButton } from '../../components/EditButton'
import { PagedTable } from '../../components/PagedTable'
import { PageHeader } from '../../components/PageHeader'
import { SearchFilter } from '../../components/SearchFilter'
import { StatusBadge } from '../../components/StatusBadge'
import { PRODUCT_STATUS, PRODUCT_STATUS_OPTIONS } from '../../constants/productStatus'
import { ROUTES } from '../../constants/routes'
import { useIsMobile } from '../../hooks/useIsMobile'
import { type PageLoader, usePagedList } from '../../hooks/usePagedList'
import { useReturnState } from '../../hooks/useReturnTo'
import { useSelectOptions } from '../../hooks/useSelectOptions'
import { texts } from '../../messages/texts'
import type { ProductResponse, ProductStatus } from '../../types/product'
import { errorMessage } from '../../utils/errors'
import { EMPTY_VALUE, formatMoney } from '../../utils/format'

const DEFAULT_SORT = 'name,asc'

/** URL filters → API filters. Outside the component, so usePagedList gets the same function on every render. */
const loadProducts: PageLoader<ProductResponse> = (query, filters, signal) =>
  getProducts(
    {
      ...query,
      search: filters.get('search') ?? undefined,
      categoryId: toId(filters.get('categoryId')),
      supplierId: toId(filters.get('supplierId')),
      status: toStatus(filters.get('status')),
    },
    signal,
  )

/**
 * Products list, like the mockup: filters above a table with Edit / Delete buttons per row. Page, page size,
 * sort and filters are in the URL (usePagedList), so Back and reload keep them.
 *
 * Not here yet: the mockup's Stock column (stock levels come from inventory-service, Phase 4).
 */
export function ProductListPage() {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const isMobile = useIsMobile()
  const returnState = useReturnState()
  const list = usePagedList(loadProducts, DEFAULT_SORT)
  const categories = useSelectOptions(getCategoryOptions)
  const suppliers = useSelectOptions(getSupplierOptions)

  const onDelete = async (product: ProductResponse) => {
    try {
      await deleteProduct(product.id)
      message.success(texts.product.deleted(product.sku))
      list.reload()
    } catch (error) {
      message.error(errorMessage(error))
    }
  }

  const fields = texts.product.fields
  // `sorter: true` = sorted by the backend: the table only reports the click (onTableChange)
  const columns: TableColumnsType<ProductResponse> = [
    {
      title: fields.sku,
      dataIndex: 'sku',
      sorter: true,
      sortOrder: list.sortOrder('sku'),
      render: (sku: string, product) => (
        <Link to={ROUTES.productEdit(product.id)} state={returnState}>
          {sku}
        </Link>
      ),
    },
    { title: fields.name, dataIndex: 'name', sorter: true, sortOrder: list.sortOrder('name') },
    {
      title: fields.category,
      // The backend sorts by the category's name; `key` is the sort field, render shows the name
      key: 'category.name',
      sorter: true,
      sortOrder: list.sortOrder('category.name'),
      render: (_, product) => product.categoryName,
    },
    {
      title: fields.supplier,
      key: 'supplier.name',
      sorter: true,
      sortOrder: list.sortOrder('supplier.name'),
      render: (_, product) => product.supplierName ?? EMPTY_VALUE,
    },
    {
      title: fields.price,
      dataIndex: 'salePrice',
      align: 'right',
      sorter: true,
      sortOrder: list.sortOrder('salePrice'),
      render: (price: number) => formatMoney(price),
    },
    {
      title: fields.status,
      dataIndex: 'status',
      sorter: true,
      sortOrder: list.sortOrder('status'),
      render: (status: ProductStatus) => <StatusBadge value={status} display={PRODUCT_STATUS} />,
    },
    {
      title: texts.common.fields.actions,
      key: 'actions',
      render: (_, product) => (
        <Flex gap={6}>
          <EditButton to={ROUTES.productEdit(product.id)} />
          <DeleteButton title={texts.product.confirmDelete(product.name)} onConfirm={() => onDelete(product)} />
        </Flex>
      ),
    },
  ]

  // Filters share the row on wide screens and take a full row each on phones
  const filterWidth = (desktop: number) => ({ width: isMobile ? '100%' : desktop })

  return (
    <>
      <PageHeader
        title={texts.product.title}
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate(ROUTES.productNew, { state: returnState })}>
            {texts.product.newButton}
          </Button>
        }
      />
      <PagedTable
        list={list}
        columns={columns}
        filters={
          <>
            <SearchFilter list={list} placeholder={texts.product.searchPlaceholder} />
            <Select
              value={toId(list.filter('categoryId'))}
              options={categories.options}
              loading={categories.loading}
              placeholder={texts.product.allCategories}
              allowClear
              showSearch={{ optionFilterProp: 'label' }}
              onChange={(categoryId?: number) => list.setFilters({ categoryId })}
              style={filterWidth(200)}
            />
            <Select
              value={toId(list.filter('supplierId'))}
              options={suppliers.options}
              loading={suppliers.loading}
              placeholder={texts.product.allSuppliers}
              allowClear
              showSearch={{ optionFilterProp: 'label' }}
              onChange={(supplierId?: number) => list.setFilters({ supplierId })}
              style={filterWidth(200)}
            />
            <Select
              value={toStatus(list.filter('status'))}
              options={PRODUCT_STATUS_OPTIONS}
              placeholder={texts.common.filters.allStatuses}
              allowClear
              onChange={(status?: ProductStatus) => list.setFilters({ status })}
              style={filterWidth(160)}
            />
          </>
        }
      />
    </>
  )
}

/** "12" → 12; anything else (missing, "abc") → undefined, i.e. no filter. */
function toId(value: string | null | undefined): number | undefined {
  const id = Number(value)
  return value && Number.isInteger(id) && id > 0 ? id : undefined
}

/** Only a status the frontend knows; anything else in the URL is ignored. */
function toStatus(value: string | null | undefined): ProductStatus | undefined {
  return value !== null && value !== undefined && value in PRODUCT_STATUS ? (value as ProductStatus) : undefined
}
