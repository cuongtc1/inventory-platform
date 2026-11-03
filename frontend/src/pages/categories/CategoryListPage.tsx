import { PlusOutlined } from '@ant-design/icons'
import { App, Button, Flex, type TableColumnsType } from 'antd'
import { Link, useNavigate } from 'react-router'
import { deleteCategory, getCategories } from '../../api/categories'
import { DeleteButton } from '../../components/DeleteButton'
import { EditButton } from '../../components/EditButton'
import { PagedTable } from '../../components/PagedTable'
import { PageHeader } from '../../components/PageHeader'
import { SearchFilter } from '../../components/SearchFilter'
import { ROUTES } from '../../constants/routes'
import { type PageLoader, usePagedList } from '../../hooks/usePagedList'
import { useReturnState } from '../../hooks/useReturnTo'
import { texts } from '../../messages/texts'
import type { CategoryResponse } from '../../types/category'
import { errorMessage } from '../../utils/errors'
import { EMPTY_VALUE } from '../../utils/format'

const DEFAULT_SORT = 'name,asc'

/** URL filters → API filters. Outside the component, so usePagedList gets the same function on every render. */
const loadCategories: PageLoader<CategoryResponse> = (query, filters, signal) =>
  getCategories({ ...query, search: filters.get('search') ?? undefined }, signal)

/**
 * Categories list, like the mockup: Name, Parent, Products, Edit / Delete. Same pattern as ProductListPage.
 * The product count links to the products list filtered by this category.
 */
export function CategoryListPage() {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const returnState = useReturnState()
  const list = usePagedList(loadCategories, DEFAULT_SORT)

  const onDelete = async (category: CategoryResponse) => {
    try {
      await deleteCategory(category.id)
      message.success(texts.category.deleted(category.name))
      list.reload()
    } catch (error) {
      // IN_USE (subcategories or products left): the backend's text says what to do first
      message.error(errorMessage(error))
    }
  }

  const fields = texts.category.fields
  const columns: TableColumnsType<CategoryResponse> = [
    {
      title: fields.name,
      dataIndex: 'name',
      sorter: true,
      sortOrder: list.sortOrder('name'),
      render: (name: string, category) => (
        <Link to={ROUTES.categoryEdit(category.id)} state={returnState}>
          {name}
        </Link>
      ),
    },
    {
      title: fields.parent,
      key: 'parent.name',
      sorter: true,
      sortOrder: list.sortOrder('parent.name'),
      render: (_, category) => category.parentName ?? EMPTY_VALUE,
    },
    {
      // Counted by the backend per page, so this column can't be sorted
      title: fields.productCount,
      dataIndex: 'productCount',
      align: 'right',
      render: (count: number, category) =>
        count === 0 ? (
          count
        ) : (
          <Link
            to={`${ROUTES.products}?categoryId=${category.id}`}
            aria-label={texts.category.showProducts(category.name)}
          >
            {count}
          </Link>
        ),
    },
    {
      title: texts.common.fields.actions,
      key: 'actions',
      render: (_, category) => (
        <Flex gap={6}>
          <EditButton to={ROUTES.categoryEdit(category.id)} />
          <DeleteButton title={texts.category.confirmDelete(category.name)} onConfirm={() => onDelete(category)} />
        </Flex>
      ),
    },
  ]

  return (
    <>
      <PageHeader
        title={texts.category.title}
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate(ROUTES.categoryNew, { state: returnState })}>
            {texts.category.newButton}
          </Button>
        }
      />
      <PagedTable
        list={list}
        columns={columns}
        filters={<SearchFilter list={list} placeholder={texts.category.searchPlaceholder} />}
      />
    </>
  )
}
