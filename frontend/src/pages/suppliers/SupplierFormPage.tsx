import { App, Card, Col, Form, Input, Row } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { createSupplier, getSupplier, updateSupplier } from '../../api/suppliers'
import { AuditInfo } from '../../components/AuditInfo'
import { FormActions } from '../../components/FormActions'
import { RecordFormPage } from '../../components/RecordFormPage'
import { FORM_MAX_WIDTH } from '../../constants/app'
import { ROUTES } from '../../constants/routes'
import {
  EMAIL_MAX_LENGTH,
  NAME_MAX_LENGTH,
  PHONE_MAX_LENGTH,
  PHONE_PATTERN,
  SUPPLIER_NAME_MAX_LENGTH,
} from '../../constants/validation'
import { texts } from '../../messages/texts'
import type { SupplierRequest, SupplierResponse } from '../../types/supplier'
import { errorMessage, formFieldErrors } from '../../utils/errors'

/** Create and edit a supplier: "/suppliers/new" and "/suppliers/:id/edit". */
export function SupplierFormPage() {
  return (
    <RecordFormPage
      newTitle={texts.supplier.newTitle}
      editTitle={texts.supplier.editTitle}
      load={getSupplier}
      listPath={ROUTES.suppliers}
    >
      {(supplier, returnTo) => <SupplierForm key={supplier?.id} supplier={supplier} returnTo={returnTo} />}
    </RecordFormPage>
  )
}

interface SupplierFormProps {
  /** The supplier to edit; missing for a new supplier. */
  supplier?: SupplierResponse
  returnTo: string
}

/** Only the name is required; empty contact fields are sent as null ("not set"). */
function SupplierForm({ supplier, returnTo }: SupplierFormProps) {
  const [form] = Form.useForm<SupplierRequest>()
  const [saving, setSaving] = useState(false)
  const { message } = App.useApp()
  const navigate = useNavigate()
  const fields = texts.supplier.fields

  const onFinish = async (values: SupplierRequest) => {
    const request: SupplierRequest = {
      name: values.name,
      contactName: emptyToNull(values.contactName),
      email: emptyToNull(values.email),
      phone: emptyToNull(values.phone),
    }
    setSaving(true)
    try {
      const saved = supplier ? await updateSupplier(supplier.id, request) : await createSupplier(request)
      message.success(supplier ? texts.supplier.updated(saved.name) : texts.supplier.created(saved.name))
      navigate(returnTo)
    } catch (error) {
      form.setFields(formFieldErrors<SupplierRequest>(error, { DUPLICATE: 'name' }))
      message.error(errorMessage(error))
      setSaving(false)
    }
  }

  return (
    <Card style={{ maxWidth: FORM_MAX_WIDTH }}>
      <Form
        form={form}
        layout="vertical"
        initialValues={supplier}
        onFinish={onFinish}
        disabled={saving}
        validateTrigger="onBlur"
      >
        <Row gutter={16}>
          <Col xs={24} md={12}>
            <Form.Item name="name" label={fields.name} rules={[{ required: true, whitespace: true }]}>
              <Input maxLength={SUPPLIER_NAME_MAX_LENGTH} autoFocus={!supplier} />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item name="contactName" label={fields.contactName}>
              <Input maxLength={NAME_MAX_LENGTH} />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item name="email" label={fields.email} rules={[{ type: 'email', message: texts.common.validation.email }]}>
              {/* inputMode: phones show the keyboard with "@". Not type="email", which would add the browser's own
                  check and popup on top of the form's message. */}
              <Input inputMode="email" maxLength={EMAIL_MAX_LENGTH} />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item name="phone" label={fields.phone} rules={[{ pattern: PHONE_PATTERN, message: texts.common.validation.phone }]}>
              {/* inputMode: phones show the number keyboard */}
              <Input inputMode="tel" maxLength={PHONE_MAX_LENGTH} />
            </Form.Item>
          </Col>
        </Row>
        <FormActions saving={saving} onCancel={() => navigate(returnTo)} />
      </Form>

      {supplier && <AuditInfo record={supplier} />}
    </Card>
  )
}

/** "" or "   " → null, so an emptied field clears the value instead of storing spaces. */
function emptyToNull(value: string | null | undefined): string | null {
  const trimmed = value?.trim()
  return trimmed ? trimmed : null
}
