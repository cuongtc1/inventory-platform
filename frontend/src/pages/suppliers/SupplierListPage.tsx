import { Button, Empty } from 'antd'
import { PageHeader } from '../../components/PageHeader'
import { texts } from '../../messages/texts'

/** Suppliers list with create/edit. Placeholder until T2.4. */
export function SupplierListPage() {
  return (
    <>
      <PageHeader title={texts.supplier.title} extra={<Button type="primary">{texts.supplier.newButton}</Button>} />
      <Empty description={texts.pages.placeholder('T2.4')} />
    </>
  )
}
