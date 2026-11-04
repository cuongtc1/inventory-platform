import { Alert, Button, Card, Flex, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { Navigate, useLocation } from 'react-router'
import { login } from '../../api/auth'
import { ROUTES } from '../../constants/routes'
import { EMAIL_MAX_LENGTH, PASSWORD_MAX_LENGTH } from '../../constants/validation'
import { useReturnTo } from '../../hooks/useReturnTo'
import { useSession } from '../../hooks/useSession'
import { Logo } from '../../layouts/Logo'
import { texts } from '../../messages/texts'
import { colors } from '../../theme/theme'
import type { LoginRequest } from '../../types/auth'
import { errorMessage, formFieldErrors } from '../../utils/errors'
import { saveSession } from '../../utils/session'

/**
 * The mockup's login card, outside AppLayout. After a successful login the session changes, this component renders
 * again and <Navigate> goes to the page the user came from, or the dashboard. The same <Navigate> sends a user who
 * is already logged in away from this page.
 */
export function LoginPage() {
  const [form] = Form.useForm<LoginRequest>()
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string>()
  const session = useSession()
  const returnTo = useReturnTo(ROUTES.dashboard)
  const cameFromAPage = useLocation().state !== null

  if (session) {
    return <Navigate to={returnTo} replace />
  }

  const onFinish = async (values: LoginRequest) => {
    setSaving(true)
    setError(undefined)
    try {
      saveSession(await login(values))
    } catch (failure) {
      form.setFields(formFieldErrors<LoginRequest>(failure))
      setError(errorMessage(failure))
      setSaving(false)
    }
  }

  return (
    <Flex
      align="center"
      justify="center"
      style={{
        minHeight: '100vh',
        padding: 16,
        background: `radial-gradient(circle at 50% 0, ${colors.loginGlow}, ${colors.background} 65%)`,
      }}
    >
      <title>{texts.app.documentTitle(texts.login.title)}</title>
      <Card style={{ width: 380, maxWidth: '100%' }}>
        <Logo onWhite />
        <Typography.Title level={3} style={{ margin: 0 }}>
          {texts.login.title}
        </Typography.Title>
        <Typography.Paragraph type="secondary" style={{ margin: '4px 0 18px' }}>
          {texts.login.subtitle}
        </Typography.Paragraph>

        {(error ?? cameFromAPage) && (
          <Alert
            type={error ? 'error' : 'info'}
            showIcon
            title={error ?? texts.login.pleaseLogIn}
            style={{ marginBottom: 16 }}
          />
        )}

        <Form form={form} layout="vertical" onFinish={onFinish} disabled={saving} requiredMark={false}>
          <Form.Item name="email" label={texts.login.email} rules={[{ required: true, whitespace: true }]}>
            {/* inputMode: the phone keyboard with "@"; autoComplete: lets password managers fill it in */}
            <Input inputMode="email" autoComplete="username" maxLength={EMAIL_MAX_LENGTH} autoFocus />
          </Form.Item>
          <Form.Item name="password" label={texts.login.password} rules={[{ required: true }]}>
            <Input.Password autoComplete="current-password" maxLength={PASSWORD_MAX_LENGTH} />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={saving}>
            {texts.login.submit}
          </Button>
        </Form>
      </Card>
    </Flex>
  )
}
