import { Flex, Typography } from 'antd'
import type { ReactNode } from 'react'
import { texts } from '../messages/texts'

interface PageHeaderProps {
  title: string
  /** Buttons on the right, e.g. "New Product" (the mockup's `.hd` row). */
  extra?: ReactNode
  /** Shown right after the title, e.g. a status badge. */
  titleExtra?: ReactNode
  /** A muted line under the title, e.g. "ACME Ltd · created by staff1 · 20/10/2026 10:15". */
  subtitle?: ReactNode
}

/**
 * The title row every page starts with: the page title on the left, actions on the right. It also sets the
 * browser tab title ("Products · Stockline").
 *
 * React 19: a <title> rendered anywhere in a component is moved into the document's <head> by React itself,
 * so no extra library or useEffect is needed for page titles.
 */
export function PageHeader({ title, extra, titleExtra, subtitle }: PageHeaderProps) {
  return (
    <Flex justify="space-between" align="center" gap={16} wrap style={{ marginBottom: 16 }}>
      <title>{texts.app.documentTitle(title)}</title>
      <div>
        <Flex align="center" gap={10} wrap>
          <Typography.Title level={3} style={{ margin: 0 }}>
            {title}
          </Typography.Title>
          {titleExtra}
        </Flex>
        {subtitle && <Typography.Text type="secondary">{subtitle}</Typography.Text>}
      </div>
      {extra}
    </Flex>
  )
}
