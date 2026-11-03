import { Alert, App, Card, Col, Form, Input, Row, Select } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { createCategory, getCategory, getCategoryOptions, updateCategory } from '../../api/categories'
import { AuditInfo } from '../../components/AuditInfo'
import { FormActions } from '../../components/FormActions'
import { RecordFormPage } from '../../components/RecordFormPage'
import { FORM_MAX_WIDTH } from '../../constants/app'
import { ROUTES } from '../../constants/routes'
import { NAME_MAX_LENGTH } from '../../constants/validation'
import { useSelectOptions } from '../../hooks/useSelectOptions'
import { texts } from '../../messages/texts'
import type { CategoryRequest, CategoryResponse } from '../../types/category'
import { errorMessage, formFieldErrors } from '../../utils/errors'

/** Create and edit a category: "/categories/new" and "/categories/:id/edit". */
export function CategoryFormPage() {
  return (
    <RecordFormPage
      newTitle={texts.category.newTitle}
      editTitle={texts.category.editTitle}
      load={getCategory}
      listPath={ROUTES.categories}
    >
      {(category, returnTo) => <CategoryForm key={category?.id} category={category} returnTo={returnTo} />}
    </RecordFormPage>
  )
}

interface CategoryFormProps {
  /** The category to edit; missing for a new category. */
  category?: CategoryResponse
  returnTo: string
}

function CategoryForm({ category, returnTo }: CategoryFormProps) {
  const [form] = Form.useForm<CategoryRequest>()
  const [saving, setSaving] = useState(false)
  const { message } = App.useApp()
  const navigate = useNavigate()
  const categories = useSelectOptions(getCategoryOptions)
  const fields = texts.category.fields

  // A category can't be its own parent. Moving it under one of its subcategories is refused by the backend
  // (BAD_REQUEST, shown under the Parent field); the list here doesn't know the tree below it.
  const parentOptions = categories.options.filter(option => option.value !== category?.id)

  const onFinish = async (values: CategoryRequest) => {
    // A cleared Select gives undefined; the API wants null for "top level"
    const request: CategoryRequest = { ...values, parentId: values.parentId ?? null }
    setSaving(true)
    try {
      const saved = category ? await updateCategory(category.id, request) : await createCategory(request)
      message.success(category ? texts.category.updated(saved.name) : texts.category.created(saved.name))
      navigate(returnTo)
    } catch (error) {
      form.setFields(formFieldErrors<CategoryRequest>(error, { DUPLICATE: 'name', BAD_REQUEST: 'parentId' }))
      message.error(errorMessage(error))
      setSaving(false)
    }
  }

  return (
    <Card style={{ maxWidth: FORM_MAX_WIDTH }}>
      {categories.error && (
        <Alert
          type="error"
          showIcon
          title={texts.common.validation.optionsFailed}
          description={categories.error}
          style={{ marginBottom: 16 }}
        />
      )}
      <Form
        form={form}
        layout="vertical"
        initialValues={category}
        onFinish={onFinish}
        disabled={saving}
        validateTrigger="onBlur"
      >
        <Row gutter={16}>
          <Col xs={24} md={12}>
            <Form.Item name="name" label={fields.name} rules={[{ required: true, whitespace: true }]}>
              <Input maxLength={NAME_MAX_LENGTH} autoFocus={!category} />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item name="parentId" label={fields.parent}>
              <Select
                options={parentOptions}
                loading={categories.loading}
                placeholder={texts.category.noParent}
                allowClear
                showSearch={{ optionFilterProp: 'label' }}
              />
            </Form.Item>
          </Col>
        </Row>
        <FormActions saving={saving} onCancel={() => navigate(returnTo)} />
      </Form>

      {category && <AuditInfo record={category} />}
    </Card>
  )
}
