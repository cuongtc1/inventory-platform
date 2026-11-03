import { Alert, App, Button, Card, Col, Flex, Form, Input, InputNumber, Result, Row, Select, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { getCategoryOptions } from '../../api/categories'
import { createProduct, getProduct, updateProduct } from '../../api/products'
import { getSupplierOptions } from '../../api/suppliers'
import { PageHeader } from '../../components/PageHeader'
import { PageLoading } from '../../components/PageLoading'
import { PRODUCT_STATUS_OPTIONS } from '../../constants/productStatus'
import { ROUTES } from '../../constants/routes'
import { CODE_MAX_LENGTH, CODE_PATTERN, MONEY_DECIMALS, MONEY_MAX, PRODUCT_NAME_MAX_LENGTH } from '../../constants/validation'
import { useReturnTo } from '../../hooks/useReturnTo'
import { useSelectOptions } from '../../hooks/useSelectOptions'
import { texts } from '../../messages/texts'
import type { ProductRequest, ProductResponse } from '../../types/product'
import { errorMessage, formFieldErrors, toApiError } from '../../utils/errors'
import { formatDateTime } from '../../utils/format'

/** Default values of a new product. */
const NEW_PRODUCT: Partial<ProductRequest> = { status: 'ACTIVE', reorderLevel: 0 }

/** What loading the product to edit gave: the product, or the text of the error. */
type Loaded = { id: number; product: ProductResponse } | { id: number; error: string }

/**
 * Create and edit a product: "/products/new" and "/products/:id/edit" use the same page. `useParams()` reads the
 * `:id` part of the URL; it is missing on "/products/new".
 *
 * Save and Cancel go back to the list the form was opened from, with its page and filters (useReturnTo).
 */
export function ProductFormPage() {
  const { id } = useParams()
  const productId = id === undefined ? undefined : Number(id)
  const [loaded, setLoaded] = useState<Loaded>()
  const returnTo = useReturnTo(ROUTES.products)
  const navigate = useNavigate()

  useEffect(() => {
    if (productId === undefined || !Number.isInteger(productId) || productId <= 0) {
      return
    }
    const controller = new AbortController()
    getProduct(productId, controller.signal)
      .then(product => setLoaded({ id: productId, product }))
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          setLoaded({ id: productId, error: errorMessage(error) })
        }
      })
    return () => controller.abort()
  }, [productId])

  const title = id === undefined ? texts.product.newTitle : texts.product.editTitle(id)
  const header = (
    <PageHeader title={title} extra={<Button onClick={() => navigate(returnTo)}>{texts.common.actions.back}</Button>} />
  )

  if (productId === undefined) {
    return (
      <>
        {header}
        <ProductForm returnTo={returnTo} />
      </>
    )
  }
  // "/products/abc/edit": no request, just the 404 text
  if (!Number.isInteger(productId) || productId <= 0) {
    return (
      <>
        {header}
        <Result status="404" title={texts.pages.notFound.title} subTitle={texts.pages.notFound.description} />
      </>
    )
  }
  // Still loading, or loaded for another id (the URL changed from one product to another)
  if (loaded?.id !== productId) {
    return (
      <>
        {header}
        <PageLoading />
      </>
    )
  }
  return (
    <>
      {header}
      {'error' in loaded ? (
        <Result status="warning" title={loaded.error} />
      ) : (
        // key: a new form (with new initial values) for each product
        <ProductForm key={loaded.product.id} product={loaded.product} returnTo={returnTo} />
      )}
    </>
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
      const apiError = toApiError(error)
      if (apiError.code === 'DUPLICATE') {
        // The only unique field of a product is its SKU: show the backend's text there
        form.setFields([{ name: 'sku', errors: [apiError.detail] }])
      } else {
        form.setFields(formFieldErrors<ProductRequest>(apiError))
      }
      message.error(errorMessage(apiError))
      setSaving(false)
    }
  }

  return (
    <Card style={{ maxWidth: 960 }}>
      {(categories.error ?? suppliers.error) && (
        <Alert
          type="error"
          showIcon
          title={texts.common.validation.optionsFailed}
          description={categories.error ?? suppliers.error}
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

        <Flex gap={8} wrap>
          <Button type="primary" htmlType="submit" loading={saving}>
            {texts.common.actions.saveChanges}
          </Button>
          <Button onClick={() => navigate(returnTo)}>{texts.common.actions.cancel}</Button>
        </Flex>
      </Form>

      {product && (
        <Typography.Paragraph type="secondary" style={{ margin: '18px 0 0' }}>
          {texts.common.audit.created(formatDateTime(product.createdAt), product.createdBy)}
          <br />
          {texts.common.audit.updated(formatDateTime(product.updatedAt), product.updatedBy)}
        </Typography.Paragraph>
      )}
    </Card>
  )
}
