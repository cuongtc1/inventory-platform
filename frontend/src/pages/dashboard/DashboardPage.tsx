import { Alert, Card, Flex } from 'antd'
import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { getCategories } from '../../api/categories'
import { PageHeader } from '../../components/PageHeader'
import { ROUTES } from '../../constants/routes'
import { useHasRole } from '../../hooks/useSession'
import { texts } from '../../messages/texts'
import { errorMessage } from '../../utils/errors'

/**
 * What the connection check shows. A union of objects with a `status` field: TypeScript knows which other fields
 * exist for each status (`count` only when 'connected'). Similar to a sealed interface with records in Java 21.
 */
type Connection =
  | { status: 'checking' }
  | { status: 'connected'; count: number }
  | { status: 'failed'; message: string }

/**
 * Start page. The real dashboard (KPIs, charts) comes later; for now it shows whether product-service is reachable
 * through the dev proxy, and links to the catalog pages.
 *
 * It is also the smallest example of the React basics in frontend/README.md:
 * - state: `useState` keeps the connection status; changing it makes React render the component again;
 * - effect: `useEffect` calls the API after the first render, and cancels the call when the page is left.
 */
export function DashboardPage() {
  const [connection, setConnection] = useState<Connection>({ status: 'checking' })
  const canManage = useHasRole('MANAGER')

  useEffect(() => {
    const controller = new AbortController()
    getCategories({ page: 0, size: 1 }, controller.signal)
      .then(page => setConnection({ status: 'connected', count: page.totalElements }))
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          setConnection({ status: 'failed', message: errorMessage(error) })
        }
      })
    // Cleanup: runs when the page is left (and once extra in development, because of StrictMode)
    return () => controller.abort()
  }, []) // [] = run once, after the first render

  return (
    <>
      <PageHeader title={texts.dashboard.title} />
      <Flex vertical gap={16}>
        <Card title={texts.dashboard.connection.title}>
          {connection.status === 'checking' && <Alert type="info" title={texts.dashboard.connection.checking} />}
          {connection.status === 'connected' && (
            <Alert type="success" title={texts.dashboard.connection.connected(connection.count)} />
          )}
          {connection.status === 'failed' && (
            <Alert type="error" title={texts.dashboard.connection.failed} description={connection.message} />
          )}
        </Card>
        <Card title={texts.dashboard.quickLinks}>
          <Flex gap={16} wrap>
            <Link to={ROUTES.products}>{texts.nav.items.products}</Link>
            {canManage && <Link to={ROUTES.categories}>{texts.nav.items.categories}</Link>}
            {canManage && <Link to={ROUTES.suppliers}>{texts.nav.items.suppliers}</Link>}
          </Flex>
        </Card>
      </Flex>
    </>
  )
}
