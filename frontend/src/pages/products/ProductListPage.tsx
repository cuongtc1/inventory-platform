import { Button, Empty } from 'antd'
import { Link } from 'react-router'
import { PageHeader } from '../../components/PageHeader'
import { ROUTES } from '../../constants/routes'
import { texts } from '../../messages/texts'

/** Products list. Placeholder until T2.3 (table with search, filters, paging, Edit/Delete). */
export function ProductListPage() {
  return (
    <>
      <PageHeader
        title={texts.product.title}
        extra={
          <Link to={ROUTES.productNew}>
            <Button type="primary">{texts.product.newButton}</Button>
          </Link>
        }
      />
      <Empty description={texts.pages.placeholder('T2.3')} />
    </>
  )
}
