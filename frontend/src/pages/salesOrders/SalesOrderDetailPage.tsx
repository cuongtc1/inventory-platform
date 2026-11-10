import { Alert, App, Button, Card, Col, Popconfirm, Result, Row, Steps, Table, type TableColumnsType, Timeline, Typography } from 'antd'
import { type ReactNode, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { changeSalesOrderStatus, getSalesOrder, getSalesOrderHistory } from '../../api/salesOrders'
import { PageHeader } from '../../components/PageHeader'
import { PageLoading } from '../../components/PageLoading'
import { StatusBadge } from '../../components/StatusBadge'
import { NEXT_STATUSES, ORDER_ACTIONS, ORDER_STATUS, ORDER_STEPS } from '../../constants/orderStatus'
import { ROUTES } from '../../constants/routes'
import { useIsMobile } from '../../hooks/useIsMobile'
import { useReturnTo } from '../../hooks/useReturnTo'
import { useHasRole } from '../../hooks/useSession'
import { texts } from '../../messages/texts'
import type { OrderAction, OrderStatusHistoryResponse, SalesOrderItem, SalesOrderResponse } from '../../types/salesOrder'
import { errorMessage } from '../../utils/errors'
import { formatDateTime, formatMoney } from '../../utils/format'

/** The order and its timeline, or the text of the error that stopped loading them. */
type Loaded = { id: number; order: SalesOrderResponse; history: OrderStatusHistoryResponse[] } | { id: number; error: string }

/**
 * "/sales-orders/:id", like the mockup: number and status, the buttons the status allows, a stepper of the happy path,
 * the lines, and the status history. The buttons only follow a copy of the state machine; the backend decides.
 */
export function SalesOrderDetailPage() {
  const { id } = useParams()
  const orderId = Number(id)
  const validId = Number.isInteger(orderId) && orderId > 0
  const [loaded, setLoaded] = useState<Loaded>()
  const [reloadCount, setReloadCount] = useState(0)
  const [busy, setBusy] = useState<OrderAction>()
  const { message } = App.useApp()
  const navigate = useNavigate()
  const returnTo = useReturnTo(ROUTES.salesOrders)
  const canCancel = useHasRole('MANAGER')

  useEffect(() => {
    if (!validId) {
      return
    }
    const controller = new AbortController()
    Promise.all([getSalesOrder(orderId, controller.signal), getSalesOrderHistory(orderId, controller.signal)])
      .then(([order, history]) => setLoaded({ id: orderId, order, history }))
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          setLoaded({ id: orderId, error: errorMessage(error) })
        }
      })
    return () => controller.abort()
  }, [validId, orderId, reloadCount])

  const back = <Button onClick={() => navigate(returnTo)}>{texts.common.actions.back}</Button>
  if (!validId) {
    return <Result status="404" title={texts.pages.notFound.title} subTitle={texts.pages.notFound.description} />
  }
  if (loaded?.id !== orderId) {
    return <PageLoading />
  }
  if ('error' in loaded) {
    return (
      <>
        <PageHeader title={texts.salesOrder.title} extra={back} />
        <Result status="warning" title={loaded.error} />
      </>
    )
  }

  const { order, history } = loaded
  const next = NEXT_STATUSES[order.status]

  const run = async (action: OrderAction, done: (orderNumber: string) => string) => {
    setBusy(action)
    try {
      const updated = await changeSalesOrderStatus(order.id, action)
      message.success(done(updated.orderNumber))
    } catch (error) {
      // INVALID_STATUS_TRANSITION or CONCURRENT_UPDATE: someone changed it meanwhile; reloading shows where it is
      message.error(errorMessage(error))
    }
    setBusy(undefined)
    setReloadCount(count => count + 1)
  }

  const forward = ORDER_ACTIONS.find(action => next.includes(action.to))
  const actions: ReactNode[] = [back]
  if (canCancel && next.includes('CANCELLED')) {
    actions.push(
      <Popconfirm
        key="cancel"
        title={texts.salesOrder.actions.confirmCancel(order.orderNumber)}
        description={texts.salesOrder.actions.cancelHint}
        okText={texts.salesOrder.actions.cancel}
        okButtonProps={{ danger: true }}
        cancelText={texts.salesOrder.actions.keep}
        onConfirm={() => run('cancel', texts.salesOrder.actions.cancelled)}
      >
        <Button danger loading={busy === 'cancel'} disabled={busy !== undefined}>
          {texts.salesOrder.actions.cancel}
        </Button>
      </Popconfirm>,
    )
  }
  if (forward) {
    actions.push(
      <Button
        key={forward.action}
        type="primary"
        loading={busy === forward.action}
        disabled={busy !== undefined}
        onClick={() => run(forward.action, forward.done)}
      >
        {forward.label}
      </Button>,
    )
  }

  return (
    <>
      <PageHeader
        title={order.orderNumber}
        titleExtra={<StatusBadge value={order.status} display={ORDER_STATUS} />}
        subtitle={texts.salesOrder.subtitle(order.customerName, order.createdBy, formatDateTime(order.createdAt))}
        extra={<Row gutter={[8, 8]}>{actions.map((action, index) => <Col key={index}>{action}</Col>)}</Row>}
      />
      {order.status === 'PENDING' && (
        <Alert type="info" showIcon title={texts.salesOrder.pendingNote} style={{ marginBottom: 16 }} />
      )}
      <Card style={{ marginBottom: 16 }}>
        <OrderSteps order={order} history={history} />
      </Card>
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={15}>
          <Card title={texts.salesOrder.items}>
            <OrderItems order={order} />
          </Card>
        </Col>
        <Col xs={24} lg={9}>
          <Card title={texts.salesOrder.history}>
            <Timeline
              items={history.map(row => ({
                key: row.id,
                color: ORDER_STATUS[row.toStatus].tone === 'red' ? 'red' : 'green',
                title: formatDateTime(row.createdAt),
                content: (
                  <>
                    <StatusBadge value={row.toStatus} display={ORDER_STATUS} />{' '}
                    {texts.salesOrder.changedBy(row.createdBy)}
                    {row.note && <Typography.Paragraph type="secondary" style={{ margin: 0 }}>{row.note}</Typography.Paragraph>}
                  </>
                ),
              }))}
            />
          </Card>
        </Col>
      </Row>
    </>
  )
}

/**
 * The mockup's stepper: Created, Stock reserved, Paid, Shipped, Done. A rejected or cancelled order shows the step
 * where it stopped in red; which step that is comes from the history (the status it was cancelled from).
 */
function OrderSteps({ order, history }: { order: SalesOrderResponse; history: OrderStatusHistoryResponse[] }) {
  const isMobile = useIsMobile()
  const index = (status: string | null | undefined) => ORDER_STEPS.findIndex(step => step.status === status)

  let current: number
  let status: 'finish' | 'error'
  let stoppedReason: string | undefined
  if (order.status === 'REJECTED' || order.status === 'CANCELLED') {
    const last = history.at(-1)
    current = Math.max(index(last?.fromStatus ?? 'PENDING'), 0) + 1
    status = 'error'
    stoppedReason = ORDER_STATUS[order.status].label
  } else {
    current = index(order.status)
    status = 'finish'
  }

  return (
    <Steps
      current={current}
      status={status}
      orientation={isMobile ? 'vertical' : 'horizontal'}
      items={ORDER_STEPS.map((step, i) => ({
        title: step.title,
        content: i === current ? stoppedReason : undefined,
      }))}
    />
  )
}

/** The lines, like the mockup: Item, Qty, Price, Total, and the order total from the backend. */
function OrderItems({ order }: { order: SalesOrderResponse }) {
  const fields = texts.salesOrder.fields
  const columns: TableColumnsType<SalesOrderItem> = [
    {
      title: fields.item,
      key: 'item',
      render: (_, item) => (
        <>
          {item.productName} <Typography.Text type="secondary">{item.productSku}</Typography.Text>
        </>
      ),
    },
    { title: fields.quantity, dataIndex: 'quantity', align: 'right' },
    { title: fields.price, dataIndex: 'unitPrice', align: 'right', render: (price: number) => formatMoney(price) },
    { title: fields.total, dataIndex: 'lineTotal', align: 'right', render: (total: number) => formatMoney(total) },
  ]

  return (
    <Table
      rowKey="id"
      columns={columns}
      dataSource={order.items}
      pagination={false}
      scroll={{ x: 'max-content' }}
      summary={() => (
        <Table.Summary.Row>
          <Table.Summary.Cell index={0} colSpan={3}>
            <Typography.Text strong>{fields.total}</Typography.Text>
          </Table.Summary.Cell>
          <Table.Summary.Cell index={1} align="right">
            <Typography.Text strong>{formatMoney(order.totalAmount)}</Typography.Text>
          </Table.Summary.Cell>
        </Table.Summary.Row>
      )}
    />
  )
}
