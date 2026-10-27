import { Button, Empty } from 'antd'
import { Link, useParams } from 'react-router'
import { PageHeader } from '../../components/PageHeader'
import { ROUTES } from '../../constants/routes'
import { texts } from '../../messages/texts'

/**
 * Create and edit a product: "/products/new" and "/products/:id/edit" use the same page. `useParams()` reads the
 * `:id` part of the URL; it is missing on "/products/new". Placeholder until T2.3.
 */
export function ProductFormPage() {
  const { id } = useParams()
  const title = id ? texts.product.editTitle(id) : texts.product.newTitle

  return (
    <>
      <PageHeader
        title={title}
        extra={
          <Link to={ROUTES.products}>
            <Button>{texts.common.actions.back}</Button>
          </Link>
        }
      />
      <Empty description={texts.pages.placeholder('T2.3')} />
    </>
  )
}
