import axios from 'axios'
import { toApiError } from '../utils/errors'
import { clearSession, getSession } from '../utils/session'

/**
 * The one Axios instance every API function uses (docs/conventions.md section 10).
 *
 * baseURL: empty by default, so calls go to "/api/..." on the page's own origin. In development the Vite dev
 * server forwards "/api" to the backend (vite.config.ts), so the browser never calls another origin and the
 * services need no CORS settings. VITE_API_BASE_URL can point somewhere else (only VITE_* variables reach the
 * browser; they are read at build time from frontend/.env).
 */
export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 15_000,
  headers: { Accept: 'application/json' },
  // Spring reads repeated parameters (sort=a,asc&sort=b,desc); Axios' default would send sort[]=...
  paramsSerializer: { indexes: null },
})

// An "interceptor" runs on every request or response. This one adds the token (nothing before login).
apiClient.interceptors.request.use(config => {
  const session = getSession()
  if (session) {
    config.headers.Authorization = `Bearer ${session.token}`
  }
  return config
})

// Every failed call is rejected with an ApiError, so pages never deal with Axios' own error shape.
apiClient.interceptors.response.use(
  response => response,
  error => {
    const apiError = toApiError(error)
    // The token stopped working, e.g. it expired or the account was deactivated. Clearing the session makes
    // RequireLogin show the login page. A wrong password at login is a 401 too, but there is no session then.
    if (apiError.status === 401 && getSession()) {
      clearSession()
    }
    return Promise.reject(apiError)
  },
)
