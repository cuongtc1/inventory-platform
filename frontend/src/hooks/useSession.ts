import { useSyncExternalStore } from 'react'
import { hasRole } from '../constants/userType'
import type { UserType } from '../types/auth'
import { getSession, subscribeToSession } from '../utils/session'

/** The logged-in session, or null. The component re-renders on login, logout and when another tab logs out. */
export function useSession() {
  return useSyncExternalStore(subscribeToSession, getSession)
}

/** True when the logged-in user has this role or a higher one. For hiding UI only; the backend decides. */
export function useHasRole(needed: UserType): boolean {
  const session = useSession()
  return session !== null && hasRole(session.user.userType, needed)
}
