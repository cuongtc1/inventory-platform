import { ROUTES } from '../constants/routes'
import type { LoggedInUser, LoginResponse } from '../types/auth'

/**
 * Who is logged in: the token and user from the login answer, kept in localStorage so a reload or a new tab stays
 * logged in until the token expires (docs/conventions.md section 11).
 *
 * A plain module, not React state: the Axios client needs the token too, and it isn't a component. Components read
 * the session with useSession(), which re-renders them when it changes.
 */
export interface Session {
  token: string
  expiresAt: string
  user: LoggedInUser
}

const STORAGE_KEY = 'stockline.session'

let current: Session | null = readStored()
const listeners = new Set<() => void>()

/** The logged-in session, or null. An expired one counts as logged out. */
export function getSession(): Session | null {
  return current && Date.parse(current.expiresAt) > Date.now() ? current : null
}

export function saveSession(login: LoginResponse) {
  update({ token: login.accessToken, expiresAt: login.expiresAt, user: login.user })
}

/** The token stopped working: RequireLogin sends the user to the login page, and back here after logging in. */
export function clearSession() {
  update(null)
}

/**
 * The Log out button. A full page load instead of clearSession(): nothing of this user stays in memory, and the
 * next user starts at the dashboard, not on this user's page. Other tabs follow through the "storage" event.
 * A JWT can't be cancelled: the token itself stays valid until it expires.
 */
export function logOut() {
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch {
    // Storage blocked: the session was only in memory, and the page load ends it
  }
  window.location.assign(ROUTES.login)
}

/** Calls `listener` whenever the session changes; returns the function that stops this. For useSession(). */
export function subscribeToSession(listener: () => void): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

function update(session: Session | null) {
  current = session
  try {
    if (session) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  } catch {
    // Storage blocked (private window): the session lasts until the next reload
  }
  listeners.forEach(listener => listener())
}

function readStored(): Session | null {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored ? (JSON.parse(stored) as Session) : null
  } catch {
    return null
  }
}

// "storage" fires when another tab changes localStorage: logging out in one tab logs out all of them
window.addEventListener('storage', event => {
  if (event.key === STORAGE_KEY) {
    current = readStored()
    listeners.forEach(listener => listener())
  }
})
