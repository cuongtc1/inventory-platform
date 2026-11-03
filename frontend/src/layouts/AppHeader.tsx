import { BellOutlined, LogoutOutlined, MenuFoldOutlined, MenuOutlined, MenuUnfoldOutlined, UserOutlined } from '@ant-design/icons'
import { Avatar, Button, Flex, Layout, Tooltip } from 'antd'
import { Link, useNavigate } from 'react-router'
import { ROUTES } from '../constants/routes'
import { texts } from '../messages/texts'
import { colors } from '../theme/theme'

interface AppHeaderProps {
  isMobile: boolean
  collapsed: boolean
  /** Desktop: collapse or expand the sidebar. Mobile: open the slide-in menu. */
  onMenuClick: () => void
}

/**
 * The white top bar from the mockup: menu button on the left; notifications, avatar and "Log out" on the right.
 * On a phone the texts shrink to icons so everything fits in one row.
 *
 * Not yet here: the mockup's global search (no task for it yet), "View as" role switch and the user's real name
 * (login, Phase 3), and the unread count on the bell (notifications, Phase 8).
 */
export function AppHeader({ isMobile, collapsed, onMenuClick }: AppHeaderProps) {
  const navigate = useNavigate()

  const menuIcon = isMobile ? <MenuOutlined /> : collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />
  const menuLabel = isMobile ? texts.layout.openMenu : collapsed ? texts.layout.expandMenu : texts.layout.collapseMenu

  return (
    <Layout.Header
      style={{
        position: 'sticky',
        top: 0,
        zIndex: 10,
        display: 'flex',
        alignItems: 'center',
        gap: 12,
        height: 62,
        padding: isMobile ? '0 12px' : '0 28px',
        borderBottom: `1px solid ${colors.border}`,
        lineHeight: 'normal',
      }}
    >
      <Button type="text" icon={menuIcon} onClick={onMenuClick} aria-label={menuLabel} title={menuLabel} />

      <Flex align="center" gap={isMobile ? 4 : 12} style={{ marginInlineStart: 'auto' }}>
        <Tooltip title={texts.layout.notifications}>
          <Button
            type="text"
            icon={<BellOutlined />}
            onClick={() => navigate(ROUTES.notifications)}
            aria-label={texts.layout.notifications}
          />
        </Tooltip>

        <Link to={ROUTES.profile} aria-label={texts.layout.profile} style={{ color: colors.text }}>
          <Flex align="center" gap={8}>
            <Avatar icon={<UserOutlined />} style={{ background: colors.avatarBackground, color: colors.avatarText }} />
            {!isMobile && <span>{texts.layout.guest}</span>}
          </Flex>
        </Link>

        <Button
          icon={<LogoutOutlined />}
          onClick={() => navigate(ROUTES.login)}
          aria-label={texts.app.logOut}
          title={isMobile ? texts.app.logOut : undefined}
        >
          {!isMobile && texts.app.logOut}
        </Button>
      </Flex>
    </Layout.Header>
  )
}
