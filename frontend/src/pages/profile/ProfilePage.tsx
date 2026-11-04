import { App, Button, Card, Col, Descriptions, Flex, Form, Input, Row } from 'antd'
import { useState } from 'react'
import { changePassword } from '../../api/auth'
import { PageHeader } from '../../components/PageHeader'
import { StatusBadge } from '../../components/StatusBadge'
import { FORM_MAX_WIDTH } from '../../constants/app'
import { USER_TYPE } from '../../constants/userType'
import { PASSWORD_MAX_LENGTH, PASSWORD_MIN_LENGTH } from '../../constants/validation'
import { useSession } from '../../hooks/useSession'
import { texts } from '../../messages/texts'
import type { ChangePasswordRequest } from '../../types/auth'
import { errorMessage, formFieldErrors } from '../../utils/errors'

interface PasswordFormValues extends ChangePasswordRequest {
  confirmPassword: string
}

/**
 * My profile, like the mockup: the account (only ADMINs change it, on Users & Roles) and Change password. The
 * mockup's Phone field has no column in the backend, so it isn't here.
 */
export function ProfilePage() {
  const session = useSession()

  return (
    <>
      <PageHeader title={texts.layout.profile} />
      <Flex vertical gap={18} style={{ maxWidth: FORM_MAX_WIDTH }}>
        {session && (
          <Card title={texts.profile.account}>
            <Descriptions
              column={1}
              items={[
                { key: 'fullName', label: texts.profile.fullName, children: session.user.fullName },
                { key: 'email', label: texts.profile.email, children: session.user.email },
                {
                  key: 'role',
                  label: texts.profile.role,
                  children: <StatusBadge value={session.user.userType} display={USER_TYPE} />,
                },
              ]}
            />
          </Card>
        )}
        <ChangePasswordCard />
      </Flex>
    </>
  )
}

function ChangePasswordCard() {
  const [form] = Form.useForm<PasswordFormValues>()
  const [saving, setSaving] = useState(false)
  const { message } = App.useApp()

  const onFinish = async ({ currentPassword, newPassword }: PasswordFormValues) => {
    setSaving(true)
    try {
      await changePassword({ currentPassword, newPassword })
      message.success(texts.profile.passwordChanged)
      form.resetFields()
    } catch (error) {
      form.setFields(formFieldErrors<PasswordFormValues>(error, { BAD_REQUEST: 'currentPassword' }))
      message.error(errorMessage(error))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Card title={texts.profile.changePassword}>
      <Form form={form} layout="vertical" onFinish={onFinish} disabled={saving} validateTrigger="onBlur">
        <Row gutter={16}>
          <Col xs={24} md={12}>
            <Form.Item name="currentPassword" label={texts.profile.currentPassword} rules={[{ required: true }]}>
              <Input.Password autoComplete="current-password" maxLength={PASSWORD_MAX_LENGTH} />
            </Form.Item>
          </Col>
        </Row>
        <Row gutter={16}>
          <Col xs={24} md={12}>
            <Form.Item
              name="newPassword"
              label={texts.profile.newPassword}
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
          <Col xs={24} md={12}>
            <Form.Item
              name="confirmPassword"
              label={texts.profile.confirmPassword}
              dependencies={['newPassword']}
              rules={[
                { required: true },
                // A rule that needs another field gets the form: ({ getFieldValue }) => ...
                ({ getFieldValue }) => ({
                  validator: (_, value) =>
                    !value || value === getFieldValue('newPassword')
                      ? Promise.resolve()
                      : Promise.reject(new Error(texts.profile.passwordsDiffer)),
                }),
              ]}
            >
              <Input.Password autoComplete="new-password" maxLength={PASSWORD_MAX_LENGTH} />
            </Form.Item>
          </Col>
        </Row>
        <Button type="primary" htmlType="submit" loading={saving}>
          {texts.profile.changePassword}
        </Button>
      </Form>
    </Card>
  )
}
