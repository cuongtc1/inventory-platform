import axios from 'axios'
import { toApiError } from '../utils/errors'

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

// Every failed call is rejected with an ApiError, so pages never deal with Axios' own error shape.
// (From Phase 3: the JWT goes on in a request interceptor, and a 401 sends the user back to the login page.)
apiClient.interceptors.response.use(
  response => response,
  error => Promise.reject(toApiError(error)),
)
