import { Button, Empty } from 'antd'
import { PageHeader } from '../../components/PageHeader'
import { texts } from '../../messages/texts'

/** Categories list with create/edit. Placeholder until T2.4. */
export function CategoryListPage() {
  return (
    <>
      <PageHeader title={texts.category.title} extra={<Button type="primary">{texts.category.newButton}</Button>} />
      <Empty description={texts.pages.placeholder('T2.4')} />
    </>
  )
}
