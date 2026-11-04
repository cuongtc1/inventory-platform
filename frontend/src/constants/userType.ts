import { texts } from '../messages/texts'
import type { UserStatus, UserType } from '../types/auth'
import type { EnumDisplay } from '../types/ui'

/** Labels and badge colours for roles and user status, like the mockup. */
export const USER_TYPE: Record<UserType, EnumDisplay> = {
  ADMIN: { label: texts.user.userType.ADMIN, tone: 'green' },
  MANAGER: { label: texts.user.userType.MANAGER, tone: 'blue' },
  STAFF: { label: texts.user.userType.STAFF, tone: 'amber' },
}

export const USER_STATUS: Record<UserStatus, EnumDisplay> = {
  ACTIVE: { label: texts.user.status.ACTIVE, tone: 'green' },
  INACTIVE: { label: texts.user.status.INACTIVE, tone: 'amber' },
}

/** Options for a role Select or filter, highest role first. */
export const USER_TYPE_OPTIONS = (Object.keys(USER_TYPE) as UserType[]).map(userType => ({
  value: userType,
  label: USER_TYPE[userType].label,
}))

/** Options for a status Select or filter. */
export const USER_STATUS_OPTIONS = (Object.keys(USER_STATUS) as UserStatus[]).map(status => ({
  value: status,
  label: USER_STATUS[status].label,
}))

const ROLE_RANK: Record<UserType, number> = { STAFF: 1, MANAGER: 2, ADMIN: 3 }

/**
 * The role hierarchy, as in the backend: hasRole('ADMIN', 'MANAGER') is true. Only for hiding things: the backend
 * enforces every rule again (docs/conventions.md section 11).
 */
export function hasRole(userType: UserType, needed: UserType): boolean {
  return ROLE_RANK[userType] >= ROLE_RANK[needed]
}
