import { Drawer, Layout } from 'antd'
import { useState } from 'react'
import { Outlet, useNavigation } from 'react-router'
import { PageLoading } from '../components/PageLoading'
import { SIDEBAR_COLLAPSED_WIDTH, SIDEBAR_WIDTH } from '../constants/app'
import { useIsMobile } from '../hooks/useIsMobile'
import { useStoredState } from '../hooks/useStoredState'
import { colors } from '../theme/theme'
import { AppHeader } from './AppHeader'
import { Logo } from './Logo'
import { SidebarMenu } from './SidebarMenu'

/**
 * The frame around every page, like the mockup: dark sidebar on the left, white top bar, light content area.
 *
 * Responsive:
 * - Desktop / laptop (≥ 992 px): a fixed sidebar; the top bar's button collapses it to icons (70 px) and back.
 *   The choice is remembered in this browser.
 * - Phone / tablet (< 992 px): no sidebar; the button opens the same menu as a panel sliding in from the left,
 *   which closes when an entry is chosen. Paddings are smaller.
 *
 * `<Outlet />` is where React Router puts the current page. While the next page's code downloads (lazy routes),
 * a spinner is shown instead.
 */
export function AppLayout() {
  const isMobile = useIsMobile()
  const navigation = useNavigation()
  const [collapsed, setCollapsed] = useStoredState('stockline.sidebarCollapsed', false)
  const [menuOpen, setMenuOpen] = useState(false)

  const onMenuClick = () => (isMobile ? setMenuOpen(true) : setCollapsed(!collapsed))

  return (
    <Layout style={{ minHeight: '100%' }}>
      {!isMobile && (
        <Layout.Sider
          width={SIDEBAR_WIDTH}
          collapsedWidth={SIDEBAR_COLLAPSED_WIDTH}
          collapsed={collapsed}
          trigger={null}
          style={{
            position: 'sticky',
            top: 0,
            height: '100vh',
            overflowY: 'auto',
            borderInlineEnd: `1px solid ${colors.sidebarBorder}`,
          }}
        >
          <Logo compact={collapsed} />
          <SidebarMenu collapsed={collapsed} />
        </Layout.Sider>
      )}

      {isMobile && (
        <Drawer
          placement="left"
          open={menuOpen}
          onClose={() => setMenuOpen(false)}
          size={SIDEBAR_WIDTH + 24}
          closable={false}
          styles={{ body: { padding: 0, background: colors.sidebar } }}
        >
          <Logo />
          <SidebarMenu collapsed={false} onNavigate={() => setMenuOpen(false)} />
        </Drawer>
      )}

      <Layout>
        <AppHeader isMobile={isMobile} collapsed={collapsed} onMenuClick={onMenuClick} />
        <Layout.Content style={{ padding: isMobile ? '16px 16px 32px' : '26px 28px 50px' }}>
          {/* The mockup's content is at most 1280 px wide */}
          <div style={{ maxWidth: 1280 }}>{navigation.state === 'loading' ? <PageLoading /> : <Outlet />}</div>
        </Layout.Content>
      </Layout>
    </Layout>
  )
}
