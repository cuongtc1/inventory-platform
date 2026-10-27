import { Result } from 'antd'
import { PageHeader } from '../components/PageHeader'
import { texts } from '../messages/texts'

interface ComingSoonPageProps {
  title: string
  /** When the page arrives, e.g. "Phase 4". */
  phase: string
}

/**
 * Stand-in for a menu entry whose page belongs to a later phase (warehouses, orders, ...). The route already
 * exists, so links and the menu can point to it; the real page replaces this element in its task.
 */
export function ComingSoonPage({ title, phase }: ComingSoonPageProps) {
  return (
    <>
      <PageHeader title={title} />
      <Result status="info" title={texts.pages.comingSoon.title} subTitle={texts.pages.comingSoon.description(phase)} />
    </>
  )
}
