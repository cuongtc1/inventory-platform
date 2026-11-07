import { Alert, App, Button, Card, Col, Form, Input, InputNumber, Row, Select } from 'antd'
import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import { getProductOptions } from '../../api/products'
import { adjustStock } from '../../api/stock'
import { getWarehouseOptions } from '../../api/warehouses'
import { FormActions } from '../../components/FormActions'
import { PageHeader } from '../../components/PageHeader'
import { FORM_MAX_WIDTH } from '../../constants/app'
import { ROUTES } from '../../constants/routes'
import { STOCK_QUANTITY_MAX, STOCK_REFERENCE_MAX_LENGTH } from '../../constants/validation'
import { useReturnTo } from '../../hooks/useReturnTo'
import { useSelectOptions } from '../../hooks/useSelectOptions'
import { texts } from '../../messages/texts'
import type { AdjustStockRequest } from '../../types/stock'
import { errorMessage, formFieldErrors } from '../../utils/errors'
import { emptyToNull } from '../../utils/format'
import { toId } from '../../utils/table'

/**
 * "/stock-levels/adjust": a manual stock correction. `?productId=&warehouseId=` chooses them in advance (the Adjust
 * button of a row). Not a RecordFormPage: there is no record to load, each save adds a new movement.
 */
export function StockAdjustPage() {
  const [form] = Form.useForm<AdjustStockRequest>()
  const [saving, setSaving] = useState(false)
  const [params] = useSearchParams()
  const { message } = App.useApp()
  const navigate = useNavigate()
  const returnTo = useReturnTo(ROUTES.stockLevels)
  const products = useSelectOptions(getProductOptions)
  const warehouses = useSelectOptions(getWarehouseOptions)
  const fields = texts.stockLevel.fields

  const onFinish = async (values: AdjustStockRequest) => {
    setSaving(true)
    try {
      const saved = await adjustStock({ ...values, reference: emptyToNull(values.reference) })
      message.success(texts.stockLevel.adjusted(saved.productName, saved.warehouseCode, saved.onHand))
      navigate(returnTo)
    } catch (error) {
      // INSUFFICIENT_STOCK: the backend's text says how much is available
      form.setFields(formFieldErrors<AdjustStockRequest>(error, { INSUFFICIENT_STOCK: 'quantity' }))
      message.error(errorMessage(error))
      setSaving(false)
    }
  }

  const optionsError = products.error ?? warehouses.error

  return (
    <>
      <PageHeader
        title={texts.stockLevel.adjustButton}
        extra={<Button onClick={() => navigate(returnTo)}>{texts.common.actions.back}</Button>}
      />
      <Card style={{ maxWidth: FORM_MAX_WIDTH }}>
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
          initialValues={{ productId: toId(params.get('productId')), warehouseId: toId(params.get('warehouseId')) }}
          onFinish={onFinish}
          disabled={saving}
          validateTrigger="onBlur"
        >
          <Row gutter={16}>
            <Col xs={24} md={12}>
              <Form.Item name="productId" label={fields.product} rules={[{ required: true }]}>
                <Select
                  options={products.options}
                  loading={products.loading}
                  placeholder={texts.stockLevel.selectProduct}
                  showSearch={{ optionFilterProp: 'label' }}
                />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="warehouseId" label={fields.warehouse} rules={[{ required: true }]}>
                <Select
                  options={warehouses.options}
                  loading={warehouses.loading}
                  placeholder={texts.stockLevel.selectWarehouse}
                  showSearch={{ optionFilterProp: 'label' }}
                />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item
                name="quantity"
                label={fields.quantity}
                extra={texts.stockLevel.quantityHelp}
                rules={[{ required: true }, { validator: notZero }]}
              >
                <InputNumber min={-STOCK_QUANTITY_MAX} max={STOCK_QUANTITY_MAX} precision={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item name="reference" label={fields.reference}>
                <Input maxLength={STOCK_REFERENCE_MAX_LENGTH} placeholder={texts.stockLevel.referencePlaceholder} />
              </Form.Item>
            </Col>
          </Row>
          <FormActions saving={saving} onCancel={() => navigate(returnTo)} />
        </Form>
      </Card>
    </>
  )
}

/** An adjustment of 0 changes nothing, and the backend refuses it too. An empty field is the `required` rule's job. */
function notZero(_rule: unknown, value: number | null | undefined): Promise<void> {
  return value === 0 ? Promise.reject(new Error(texts.stockLevel.quantityNotZero)) : Promise.resolve()
}
