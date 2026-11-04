import { Navigate, Outlet, useLocation } from 'react-router'
import { ROUTES } from '../constants/routes'
import { useSession } from '../hooks/useSession'

/**
 * A "route guard": a route without a page of its own whose children only show when a condition holds. Without a
 * session this goes to the login page, remembering the URL (the same `returnTo` state as useReturnTo), so login
 * comes back here. `replace`: Back doesn't return to a page that would redirect again.
 */
export function RequireLogin() {
  const session = useSession()
  const location = useLocation()

  if (!session) {
    return <Navigate to={ROUTES.login} replace state={{ returnTo: location.pathname + location.search }} />
  }
  return <Outlet />
}
