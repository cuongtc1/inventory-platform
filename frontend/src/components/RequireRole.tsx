import { Result } from 'antd'
import { Outlet } from 'react-router'
import { useHasRole } from '../hooks/useSession'
import { texts } from '../messages/texts'
import type { UserType } from '../types/auth'

interface RequireRoleProps {
  /** The lowest role that may see these pages. */
  role: UserType
}

/**
 * A route guard for pages of higher roles: for a typed URL or a bookmark, since the menu doesn't show those pages.
 * It only hides the page; the backend refuses the data anyway.
 */
export function RequireRole({ role }: RequireRoleProps) {
  const allowed = useHasRole(role)

  if (!allowed) {
    return <Result status="403" title={texts.pages.forbidden.title} subTitle={texts.pages.forbidden.description} />
  }
  return <Outlet />
}
