import { Button, Card, Flex, Typography } from 'antd'
import { StatusBadge } from './components/StatusBadge'
import { PRODUCT_STATUS } from './constants/productStatus'
import { texts } from './messages/texts'

/**
 * Placeholder start page: shows that the theme and the shared building blocks work. T2.1 replaces it with the
 * router and the page skeleton.
 */
export function App() {
  return (
    <Flex justify="center" align="center" style={{ minHeight: '100%', padding: 24 }}>
      <Card style={{ maxWidth: 520 }}>
        <Typography.Title level={3}>{texts.app.name}</Typography.Title>
        <Typography.Paragraph type="secondary">{texts.app.foundationsReady}</Typography.Paragraph>
        <Flex gap={8} align="center">
          <StatusBadge value="ACTIVE" display={PRODUCT_STATUS} />
          <StatusBadge value="INACTIVE" display={PRODUCT_STATUS} />
          <Button type="primary">{texts.product.newButton}</Button>
        </Flex>
      </Card>
    </Flex>
  )
}
