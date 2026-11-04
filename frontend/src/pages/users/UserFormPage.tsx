import { App, Card, Col, Form, Input, Row, Select } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { createUser, getUser, updateUser } from '../../api/users'
import { AuditInfo } from '../../components/AuditInfo'
import { FormActions } from '../../components/FormActions'
import { RecordFormPage } from '../../components/RecordFormPage'
import { FORM_MAX_WIDTH } from '../../constants/app'
import { ROUTES } from '../../constants/routes'
import { USER_TYPE_OPTIONS } from '../../constants/userType'
import { EMAIL_MAX_LENGTH, NAME_MAX_LENGTH, PASSWORD_MAX_LENGTH, PASSWORD_MIN_LENGTH } from '../../constants/validation'
import { useSession } from '../../hooks/useSession'
import { texts } from '../../messages/texts'
import type { CreateUserRequest, UserResponse } from '../../types/auth'
import { errorMessage, formFieldErrors } from '../../utils/errors'

/** Create and edit a user: "/users/new" and "/users/:id/edit" (ADMIN). */
export function UserFormPage() {
  return (
    <RecordFormPage
      newTitle={texts.user.newTitle}
      editTitle={texts.user.editTitle}
      load={getUser}
      listPath={ROUTES.users}
    >
      {(user, returnTo) => <UserForm key={user?.id} user={user} returnTo={returnTo} />}
    </RecordFormPage>
  )
}

interface UserFormProps {
  /** The user to edit; missing for a new user. */
  user?: UserResponse
  returnTo: string
}

/**
 * The password field is only there for a new user: the ADMIN chooses a first password and tells it to the user,
 * who changes it on My profile. Status is changed with Deactivate / Activate on the list.
 */
function UserForm({ user, returnTo }: UserFormProps) {
  const [form] = Form.useForm<CreateUserRequest>()
  const [saving, setSaving] = useState(false)
  const { message } = App.useApp()
  const navigate = useNavigate()
  const session = useSession()
  const editingMyself = user !== undefined && user.id === session?.user.id
  const fields = texts.user.fields

  const onFinish = async (values: CreateUserRequest) => {
    setSaving(true)
    try {
      const saved = user ? await updateUser(user.id, values) : await createUser(values)
      message.success(user ? texts.user.updated(saved.email) : texts.user.created(saved.email))
      navigate(returnTo)
    } catch (error) {
      form.setFields(formFieldErrors<CreateUserRequest>(error, { DUPLICATE: 'email', BAD_REQUEST: 'userType' }))
      message.error(errorMessage(error))
      setSaving(false)
    }
  }

  return (
    <Card style={{ maxWidth: FORM_MAX_WIDTH }}>
      <Form
        form={form}
        layout="vertical"
        initialValues={user}
        onFinish={onFinish}
        disabled={saving}
        validateTrigger="onBlur"
      >
        <Row gutter={16}>
          <Col xs={24} md={12}>
            <Form.Item name="fullName" label={fields.fullName} rules={[{ required: true, whitespace: true }]}>
              <Input maxLength={NAME_MAX_LENGTH} autoFocus={!user} />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item
              name="email"
              label={fields.email}
              rules={[{ required: true, whitespace: true }, { type: 'email', message: texts.common.validation.email }]}
            >
              <Input inputMode="email" autoComplete="off" maxLength={EMAIL_MAX_LENGTH} />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item
              name="userType"
              label={fields.userType}
              rules={[{ required: true }]}
              extra={editingMyself ? texts.user.ownRoleNote : undefined}
            >
              <Select options={USER_TYPE_OPTIONS} placeholder={texts.user.selectRole} disabled={editingMyself} />
            </Form.Item>
          </Col>
          {!user && (
            <Col xs={24} md={12}>
              <Form.Item
                name="password"
                label={fields.password}
                rules={[
                  { required: true },
                  {
                    min: PASSWORD_MIN_LENGTH,
                    message: texts.common.validation.passwordLength(PASSWORD_MIN_LENGTH, PASSWORD_MAX_LENGTH),
                  },
                ]}
              >
                <Input.Password autoComplete="new-password" maxLength={PASSWORD_MAX_LENGTH} />
              </Form.Item>
            </Col>
          )}
        </Row>
        <FormActions saving={saving} onCancel={() => navigate(returnTo)} />
      </Form>

      {user && <AuditInfo record={user} />}
    </Card>
  )
}
