import { Layout } from 'antd'
import { Outlet, useNavigation } from 'react-router'
import { PageLoading } from '../components/PageLoading'

/**
 * The frame around every page. For now only the content area; T2.2 adds the mockup's dark collapsible sidebar
 * and the top bar with avatar and "Log out".
 *
 * `<Outlet />` is where React Router puts the page of the current URL (the child route). While the next page's
 * code is still downloading (lazy routes), `useNavigation()` reports "loading" and a spinner is shown.
 */
export function AppLayout() {
  const navigation = useNavigation()

  return (
    <Layout style={{ minHeight: '100%' }}>
      <Layout.Content style={{ padding: 24 }}>
        {navigation.state === 'loading' ? <PageLoading /> : <Outlet />}
      </Layout.Content>
    </Layout>
  )
}
