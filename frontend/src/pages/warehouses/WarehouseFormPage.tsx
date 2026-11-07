import { App, Card, Col, Form, Input, Row } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { createWarehouse, getWarehouse, updateWarehouse } from '../../api/warehouses'
import { AuditInfo } from '../../components/AuditInfo'
import { FormActions } from '../../components/FormActions'
import { RecordFormPage } from '../../components/RecordFormPage'
import { FORM_MAX_WIDTH } from '../../constants/app'
import { ROUTES } from '../../constants/routes'
import {
  CODE_MAX_LENGTH,
  CODE_PATTERN,
  NAME_MAX_LENGTH,
  WAREHOUSE_ADDRESS_MAX_LENGTH,
} from '../../constants/validation'
import { texts } from '../../messages/texts'
import type { WarehouseRequest, WarehouseResponse } from '../../types/warehouse'
import { errorMessage, formFieldErrors } from '../../utils/errors'
import { emptyToNull } from '../../utils/format'

/** Create and edit a warehouse: "/warehouses/new" and "/warehouses/:id/edit". */
export function WarehouseFormPage() {
  return (
    <RecordFormPage
      newTitle={texts.warehouse.newTitle}
      editTitle={texts.warehouse.editTitle}
      load={getWarehouse}
      listPath={ROUTES.warehouses}
    >
      {(warehouse, returnTo) => <WarehouseForm key={warehouse?.id} warehouse={warehouse} returnTo={returnTo} />}
    </RecordFormPage>
  )
}

interface WarehouseFormProps {
  /** The warehouse to edit; missing for a new warehouse. */
  warehouse?: WarehouseResponse
  returnTo: string
}

/** Code and name are required; an empty address is sent as null ("not set"). */
function WarehouseForm({ warehouse, returnTo }: WarehouseFormProps) {
  const [form] = Form.useForm<WarehouseRequest>()
  const [saving, setSaving] = useState(false)
  const { message } = App.useApp()
  const navigate = useNavigate()
  const fields = texts.warehouse.fields

  const onFinish = async (values: WarehouseRequest) => {
    const request: WarehouseRequest = { code: values.code, name: values.name, address: emptyToNull(values.address) }
    setSaving(true)
    try {
      const saved = warehouse ? await updateWarehouse(warehouse.id, request) : await createWarehouse(request)
      message.success(warehouse ? texts.warehouse.updated(saved.code) : texts.warehouse.created(saved.code))
      navigate(returnTo)
    } catch (error) {
      form.setFields(formFieldErrors<WarehouseRequest>(error, { DUPLICATE: 'code' }))
      message.error(errorMessage(error))
      setSaving(false)
    }
  }

  return (
    <Card style={{ maxWidth: FORM_MAX_WIDTH }}>
      <Form
        form={form}
        layout="vertical"
        initialValues={warehouse}
        onFinish={onFinish}
        disabled={saving}
        validateTrigger="onBlur"
      >
        <Row gutter={16}>
          <Col xs={24} md={12}>
            <Form.Item
              name="code"
              label={fields.code}
              rules={[{ required: true, whitespace: true }, { pattern: CODE_PATTERN, message: texts.common.validation.code }]}
            >
              <Input maxLength={CODE_MAX_LENGTH} autoFocus={!warehouse} />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item name="name" label={fields.name} rules={[{ required: true, whitespace: true }]}>
              <Input maxLength={NAME_MAX_LENGTH} />
            </Form.Item>
          </Col>
          <Col xs={24}>
            <Form.Item name="address" label={fields.address}>
              <Input maxLength={WAREHOUSE_ADDRESS_MAX_LENGTH} />
            </Form.Item>
          </Col>
        </Row>
        <FormActions saving={saving} onCancel={() => navigate(returnTo)} />
      </Form>

      {warehouse && <AuditInfo record={warehouse} />}
    </Card>
  )
}
