import { Alert, App, Card, Col, Form, Input, InputNumber, Row, Select } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { getCategoryOptions } from '../../api/categories'
import { createProduct, getProduct, updateProduct } from '../../api/products'
import { getSupplierOptions } from '../../api/suppliers'
import { AuditInfo } from '../../components/AuditInfo'
import { FormActions } from '../../components/FormActions'
import { RecordFormPage } from '../../components/RecordFormPage'
import { FORM_MAX_WIDTH } from '../../constants/app'
import { PRODUCT_STATUS_OPTIONS } from '../../constants/productStatus'
import { ROUTES } from '../../constants/routes'
import { CODE_MAX_LENGTH, CODE_PATTERN, MONEY_DECIMALS, MONEY_MAX, PRODUCT_NAME_MAX_LENGTH } from '../../constants/validation'
import { useSelectOptions } from '../../hooks/useSelectOptions'
import { texts } from '../../messages/texts'
import type { ProductRequest, ProductResponse } from '../../types/product'
import { errorMessage, formFieldErrors } from '../../utils/errors'

/** Default values of a new product. */
const NEW_PRODUCT: Partial<ProductRequest> = { status: 'ACTIVE', reorderLevel: 0 }

/**
 * Create and edit a product: "/products/new" and "/products/:id/edit". RecordFormPage loads the product and
 * handles "not found"; this file only has the form.
 */
export function ProductFormPage() {
  return (
    <RecordFormPage
      newTitle={texts.product.newTitle}
      editTitle={texts.product.editTitle}
      load={getProduct}
      listPath={ROUTES.products}
    >
      {(product, returnTo) => <ProductForm key={product?.id} product={product} returnTo={returnTo} />}
    </RecordFormPage>
  )
}

interface ProductFormProps {
  /** The product to edit; missing for a new product. */
  product?: ProductResponse
  returnTo: string
}

/**
 * The form itself, like the mockup's: two columns on wide screens, one on phones (Row / Col with `xs` and `md`).
 * The browser checks required fields and simple rules first; the backend checks everything again, and its
 * field errors are shown under the fields.
 */
function ProductForm({ product, returnTo }: ProductFormProps) {
  const [form] = Form.useForm<ProductRequest>()
  const [saving, setSaving] = useState(false)
  const { message } = App.useApp()
  const navigate = useNavigate()
  const categories = useSelectOptions(getCategoryOptions)
  const suppliers = useSelectOptions(getSupplierOptions)
  const fields = texts.product.fields

  const onFinish = async (values: ProductRequest) => {
    // A cleared Select gives undefined; the API wants null for "no supplier"
    const request: ProductRequest = { ...values, supplierId: values.supplierId ?? null }
    setSaving(true)
    try {
      const saved = product ? await updateProduct(product.id, request) : await createProduct(request)
      message.success(product ? texts.product.updated(saved.sku) : texts.product.created(saved.sku))
      navigate(returnTo)
    } catch (error) {
      // The only unique field of a product is its SKU
      form.setFields(formFieldErrors<ProductRequest>(error, { DUPLICATE: 'sku' }))
      message.error(errorMessage(error))
      setSaving(false)
    }
  }

  const optionsError = categories.error ?? suppliers.error

  return (
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
        initialValues={product ?? NEW_PRODUCT}
        onFinish={onFinish}
        disabled={saving}
        // Leave the check of each field until it was left once, instead of complaining on the first key press
        validateTrigger="onBlur"
      >
        <Row gutter={16}>
          <Col xs={24} md={12}>
            <Form.Item
              name="sku"
              label={fields.sku}
              rules={[
                { required: true, whitespace: true },
                { pattern: CODE_PATTERN, message: texts.common.validation.code },
              ]}
            >
              <Input maxLength={CODE_MAX_LENGTH} autoFocus={!product} />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item name="name" label={fields.name} rules={[{ required: true, whitespace: true }]}>
              <Input maxLength={PRODUCT_NAME_MAX_LENGTH} />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item name="categoryId" label={fields.category} rules={[{ required: true }]}>
              <Select
                options={categories.options}
                loading={categories.loading}
                placeholder={texts.product.selectCategory}
                showSearch={{ optionFilterProp: 'label' }}
              />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item name="supplierId" label={fields.supplier}>
              <Select
                options={suppliers.options}
                loading={suppliers.loading}
                placeholder={texts.product.noSupplier}
                allowClear
                showSearch={{ optionFilterProp: 'label' }}
              />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item name="costPrice" label={fields.costPrice} rules={[{ required: true }]}>
              {/* min/max: a value outside is corrected when the field is left */}
              <InputNumber prefix="$" min={0} max={MONEY_MAX} precision={MONEY_DECIMALS} style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item name="salePrice" label={fields.salePrice} rules={[{ required: true }]}>
              <InputNumber prefix="$" min={0.01} max={MONEY_MAX} precision={MONEY_DECIMALS} style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item name="reorderLevel" label={fields.reorderLevel} rules={[{ required: true }]}>
              <InputNumber min={0} precision={0} style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item name="status" label={fields.status} rules={[{ required: true }]}>
              <Select options={PRODUCT_STATUS_OPTIONS} />
            </Form.Item>
          </Col>
        </Row>
        <FormActions saving={saving} onCancel={() => navigate(returnTo)} />
      </Form>

      {product && <AuditInfo record={product} />}
    </Card>
  )
}
