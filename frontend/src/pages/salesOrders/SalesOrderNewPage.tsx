import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import { Alert, App, Button, Card, Col, Divider, Flex, Form, InputNumber, Row, Select, Tooltip, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { getCustomerOptions } from '../../api/customers'
import { getSellableProducts } from '../../api/products'
import { createSalesOrder } from '../../api/salesOrders'
import { getStockLevels } from '../../api/stock'
import { getWarehouseOptions } from '../../api/warehouses'
import { PageHeader } from '../../components/PageHeader'
import { ROUTES } from '../../constants/routes'
import { ORDER_MAX_ITEMS, ORDER_QUANTITY_MAX } from '../../constants/validation'
import { useIsMobile } from '../../hooks/useIsMobile'
import { useReturnTo } from '../../hooks/useReturnTo'
import { useSelectOptions } from '../../hooks/useSelectOptions'
import { texts } from '../../messages/texts'
import type { ProductResponse } from '../../types/product'
import type { CreateSalesOrderRequest } from '../../types/salesOrder'
import { errorMessage, formFieldErrors } from '../../utils/errors'
import { EMPTY_VALUE, formatMoney } from '../../utils/format'
import { fromCents, lineTotalCents } from '../../utils/money'

/** The form's values: like the request, but a new line has no product yet. */
interface OrderFormValues {
  customerId?: number
  warehouseId?: number
  items: { productId?: number; quantity?: number }[]
}

/**
 * "/sales-orders/new", like the mockup: customer and warehouse, then the lines with Available, Qty, Unit price and
 * Line total, and the total. One button, Place order: there are no drafts (T5.3). Totals here are an estimate in whole
 * cents (utils/money.ts); the saved order's amounts come from the backend.
 */
export function SalesOrderNewPage() {
  const [form] = Form.useForm<OrderFormValues>()
  const [saving, setSaving] = useState(false)
  const [products, setProducts] = useState<ProductResponse[]>()
  const [productsError, setProductsError] = useState<string>()
  const { message } = App.useApp()
  const navigate = useNavigate()
  const isMobile = useIsMobile()
  const returnTo = useReturnTo(ROUTES.salesOrders)
  const customers = useSelectOptions(getCustomerOptions)
  const warehouses = useSelectOptions(getWarehouseOptions)
  const items = Form.useWatch('items', form) ?? []
  const warehouseId = Form.useWatch('warehouseId', form)
  const fields = texts.salesOrder.fields

  useEffect(() => {
    const controller = new AbortController()
    getSellableProducts(controller.signal)
      .then(setProducts)
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          setProductsError(errorMessage(error))
        }
      })
    return () => controller.abort()
  }, [])

  const productById = new Map((products ?? []).map(product => [product.id, product]))
  const productOptions = (products ?? []).map(product => ({ value: product.id, label: `${product.sku} ${product.name}` }))
  const lineCents = (line: OrderFormValues['items'][number] | undefined) => {
    const product = line?.productId === undefined ? undefined : productById.get(line.productId)
    return product && line?.quantity ? lineTotalCents(product.salePrice, line.quantity) : undefined
  }
  const totalCents = items.reduce((sum, line) => sum + (lineCents(line) ?? 0), 0)

  const onFinish = async (values: OrderFormValues) => {
    const request: CreateSalesOrderRequest = {
      // Required by the form rules, so they are set here
      customerId: values.customerId as number,
      warehouseId: values.warehouseId as number,
      items: values.items.map(line => ({ productId: line.productId as number, quantity: line.quantity as number })),
    }
    setSaving(true)
    try {
      const order = await createSalesOrder(request)
      message.success(texts.salesOrder.placed(order.orderNumber))
      navigate(ROUTES.salesOrderDetail(order.id), { replace: true })
    } catch (error) {
      // VALIDATION_FAILED names the line ("items[1].quantity"), which is the field's path in Form.List
      form.setFields(formFieldErrors<OrderFormValues>(error))
      message.error(errorMessage(error))
      setSaving(false)
    }
  }

  const optionsError = customers.error ?? warehouses.error ?? productsError

  return (
    <>
      <PageHeader
        title={texts.salesOrder.newTitle}
        extra={<Button onClick={() => navigate(returnTo)}>{texts.common.actions.back}</Button>}
      />
      {optionsError && (
        <Alert
          type="error"
          showIcon
          title={texts.common.validation.optionsFailed}
          description={optionsError}
          style={{ marginBottom: 16 }}
        />
      )}
      <Form
        form={form}
        layout="vertical"
        initialValues={{ items: [{ quantity: 1 }] }}
        onFinish={onFinish}
        disabled={saving}
        validateTrigger="onBlur"
      >
        <Card style={{ marginBottom: 16 }}>
          <Row gutter={16}>
            <Col xs={24} md={12}>
              <Form.Item name="customerId" label={fields.customer} rules={[{ required: true }]}>
                <Select
                  options={customers.options}
                  loading={customers.loading}
                  placeholder={texts.salesOrder.selectCustomer}
                  showSearch={{ optionFilterProp: 'label' }}
                />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="warehouseId" label={fields.warehouse} rules={[{ required: true }]}>
                <Select
                  options={warehouses.options}
                  loading={warehouses.loading}
                  placeholder={texts.salesOrder.selectWarehouse}
                  showSearch={{ optionFilterProp: 'label' }}
                />
              </Form.Item>
            </Col>
          </Row>
        </Card>

        <Card title={texts.salesOrder.items}>
          {!isMobile && (
            <Row gutter={12} style={{ marginBottom: 8 }}>
              <Col md={9}><Typography.Text strong>{fields.product}</Typography.Text></Col>
              <Col md={3} style={{ textAlign: 'right' }}><Typography.Text strong>{fields.available}</Typography.Text></Col>
              <Col md={4}><Typography.Text strong>{fields.quantity}</Typography.Text></Col>
              <Col md={3} style={{ textAlign: 'right' }}><Typography.Text strong>{fields.unitPrice}</Typography.Text></Col>
              <Col md={4} style={{ textAlign: 'right' }}><Typography.Text strong>{fields.lineTotal}</Typography.Text></Col>
            </Row>
          )}
          <Form.List
            name="items"
            rules={[{ validator: async (_, lines?: unknown[]) => (lines?.length ? undefined : Promise.reject(new Error(texts.salesOrder.noItems))) }]}
          >
            {(lines, { add, remove }, { errors }) => (
              <>
                {lines.map((line, index) => {
                  const value = items[line.name]
                  const product = value?.productId === undefined ? undefined : productById.get(value.productId)
                  const cents = lineCents(value)
                  return (
                    <Row key={line.key} gutter={12} align="top">
                      {/* On phones each line is a stack of fields; a line between them shows where one ends */}
                      {isMobile && index > 0 && (
                        <Col xs={24}>
                          <Divider style={{ margin: '4px 0 16px' }} />
                        </Col>
                      )}
                      <Col xs={24} md={9}>
                        <Form.Item
                          name={[line.name, 'productId']}
                          label={isMobile ? fields.product : undefined}
                          rules={[{ required: true, message: texts.salesOrder.selectProduct }, { validator: notTwice(form, line.name) }]}
                        >
                          <Select
                            options={productOptions}
                            loading={products === undefined && !productsError}
                            placeholder={texts.salesOrder.selectProduct}
                            showSearch={{ optionFilterProp: 'label' }}
                          />
                        </Form.Item>
                      </Col>
                      <Col xs={8} md={3} style={{ textAlign: isMobile ? 'left' : 'right' }}>
                        <Form.Item label={isMobile ? fields.available : undefined}>
                          <Available warehouseId={warehouseId} productId={value?.productId} quantity={value?.quantity} />
                        </Form.Item>
                      </Col>
                      <Col xs={16} md={4}>
                        <Form.Item
                          name={[line.name, 'quantity']}
                          label={isMobile ? fields.quantity : undefined}
                          rules={[{ required: true }]}
                        >
                          <InputNumber min={1} max={ORDER_QUANTITY_MAX} precision={0} style={{ width: '100%' }} />
                        </Form.Item>
                      </Col>
                      <Col xs={8} md={3} style={{ textAlign: isMobile ? 'left' : 'right' }}>
                        <Form.Item label={isMobile ? fields.unitPrice : undefined}>
                          {product ? formatMoney(product.salePrice) : EMPTY_VALUE}
                        </Form.Item>
                      </Col>
                      <Col xs={12} md={4} style={{ textAlign: isMobile ? 'left' : 'right' }}>
                        <Form.Item label={isMobile ? fields.lineTotal : undefined}>
                          {cents === undefined ? EMPTY_VALUE : formatMoney(fromCents(cents))}
                        </Form.Item>
                      </Col>
                      <Col xs={4} md={1}>
                        <Form.Item label={isMobile ? ' ' : undefined}>
                          <Tooltip title={texts.salesOrder.removeItem}>
                            <Button
                              icon={<DeleteOutlined />}
                              aria-label={texts.salesOrder.removeItem}
                              disabled={lines.length === 1}
                              onClick={() => remove(line.name)}
                            />
                          </Tooltip>
                        </Form.Item>
                      </Col>
                    </Row>
                  )
                })}
                <Form.ErrorList errors={errors} />
                <Button
                  icon={<PlusOutlined />}
                  disabled={lines.length >= ORDER_MAX_ITEMS}
                  onClick={() => add({ quantity: 1 })}
                >
                  {texts.salesOrder.addItem}
                </Button>
              </>
            )}
          </Form.List>

          <Flex vertical align="end" gap={4} style={{ marginTop: 16 }}>
            <Typography.Title level={3} style={{ margin: 0 }}>
              {texts.salesOrder.estimatedTotal} {formatMoney(fromCents(totalCents))}
            </Typography.Title>
            <Typography.Text type="secondary">{texts.salesOrder.estimateNote}</Typography.Text>
            <Flex gap={8} wrap style={{ marginTop: 12 }}>
              <Button onClick={() => navigate(returnTo)}>{texts.common.actions.cancel}</Button>
              <Button type="primary" htmlType="submit" loading={saving}>
                {texts.salesOrder.placeOrder}
              </Button>
            </Flex>
          </Flex>
        </Card>
      </Form>
    </>
  )
}

/** One line per product, like the backend: it refuses the same product twice. */
function notTwice(form: ReturnType<typeof Form.useForm<OrderFormValues>>[0], index: number) {
  return async (_: unknown, productId?: number) => {
    const lines = form.getFieldValue('items') as OrderFormValues['items']
    const earlier = lines.slice(0, index).some(line => line?.productId !== undefined && line.productId === productId)
    if (earlier) {
      throw new Error(texts.salesOrder.productTwice)
    }
  }
}

interface AvailableProps {
  warehouseId?: number
  productId?: number
  quantity?: number
}

/**
 * Available stock of the product in the chosen warehouse, from inventory-service. Red when the line asks for more:
 * only a hint, since stock is checked and reserved when the order is placed (Phase 6).
 */
function Available({ warehouseId, productId, quantity }: AvailableProps) {
  const [loaded, setLoaded] = useState<{ key: string; available?: number }>()
  const key = `${warehouseId}-${productId}`

  useEffect(() => {
    if (warehouseId === undefined || productId === undefined) {
      return
    }
    const controller = new AbortController()
    getStockLevels({ page: 0, size: 1, warehouseId, productId }, controller.signal)
      // No stock level yet means nothing in stock
      .then(page => setLoaded({ key: `${warehouseId}-${productId}`, available: page.content[0]?.available ?? 0 }))
      .catch(() => {
        if (!controller.signal.aborted) {
          setLoaded({ key: `${warehouseId}-${productId}` })
        }
      })
    return () => controller.abort()
  }, [warehouseId, productId])

  if (warehouseId === undefined || productId === undefined || loaded?.key !== key || loaded.available === undefined) {
    return EMPTY_VALUE
  }
  const short = quantity !== undefined && quantity > loaded.available
  return <Typography.Text type={short ? 'danger' : undefined}>{loaded.available}</Typography.Text>
}
