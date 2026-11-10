import { App, Card, Col, Form, Input, Row } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { createCustomer, getCustomer, updateCustomer } from '../../api/customers'
import { AuditInfo } from '../../components/AuditInfo'
import { FormActions } from '../../components/FormActions'
import { RecordFormPage } from '../../components/RecordFormPage'
import { FORM_MAX_WIDTH } from '../../constants/app'
import { ROUTES } from '../../constants/routes'
import {
  CUSTOMER_NAME_MAX_LENGTH,
  EMAIL_MAX_LENGTH,
  PHONE_MAX_LENGTH,
  PHONE_PATTERN,
} from '../../constants/validation'
import { texts } from '../../messages/texts'
import type { CustomerRequest, CustomerResponse } from '../../types/customer'
import { errorMessage, formFieldErrors } from '../../utils/errors'
import { emptyToNull } from '../../utils/format'

/** Create and edit a customer: "/customers/new" and "/customers/:id/edit". Same pattern as SupplierFormPage. */
export function CustomerFormPage() {
  return (
    <RecordFormPage
      newTitle={texts.customer.newTitle}
      editTitle={texts.customer.editTitle}
      load={getCustomer}
      listPath={ROUTES.customers}
    >
      {(customer, returnTo) => <CustomerForm key={customer?.id} customer={customer} returnTo={returnTo} />}
    </RecordFormPage>
  )
}

interface CustomerFormProps {
  /** The customer to edit; missing for a new customer. */
  customer?: CustomerResponse
  returnTo: string
}

/** Only the name is required; empty contact fields are sent as null ("not set"). */
function CustomerForm({ customer, returnTo }: CustomerFormProps) {
  const [form] = Form.useForm<CustomerRequest>()
  const [saving, setSaving] = useState(false)
  const { message } = App.useApp()
  const navigate = useNavigate()
  const fields = texts.customer.fields

  const onFinish = async (values: CustomerRequest) => {
    const request: CustomerRequest = {
      name: values.name,
      email: emptyToNull(values.email),
      phone: emptyToNull(values.phone),
    }
    setSaving(true)
    try {
      const saved = customer ? await updateCustomer(customer.id, request) : await createCustomer(request)
      message.success(customer ? texts.customer.updated(saved.name) : texts.customer.created(saved.name))
      navigate(returnTo)
    } catch (error) {
      form.setFields(formFieldErrors<CustomerRequest>(error, { DUPLICATE: 'name' }))
      message.error(errorMessage(error))
      setSaving(false)
    }
  }

  return (
    <Card style={{ maxWidth: FORM_MAX_WIDTH }}>
      <Form
        form={form}
        layout="vertical"
        initialValues={customer}
        onFinish={onFinish}
        disabled={saving}
        validateTrigger="onBlur"
      >
        <Row gutter={16}>
          <Col xs={24}>
            <Form.Item name="name" label={fields.name} rules={[{ required: true, whitespace: true }]}>
              <Input maxLength={CUSTOMER_NAME_MAX_LENGTH} autoFocus={!customer} />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item name="email" label={fields.email} rules={[{ type: 'email', message: texts.common.validation.email }]}>
              {/* inputMode: phones show the keyboard with "@", without the browser's own email popup */}
              <Input inputMode="email" maxLength={EMAIL_MAX_LENGTH} />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item name="phone" label={fields.phone} rules={[{ pattern: PHONE_PATTERN, message: texts.common.validation.phone }]}>
              <Input inputMode="tel" maxLength={PHONE_MAX_LENGTH} />
            </Form.Item>
          </Col>
        </Row>
        <FormActions saving={saving} onCancel={() => navigate(returnTo)} />
      </Form>

      {customer && <AuditInfo record={customer} />}
    </Card>
  )
}
