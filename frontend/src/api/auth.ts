import type { ChangePasswordRequest, LoginRequest, LoginResponse } from '../types/auth'
import { apiClient } from './client'

/** Calls to /api/auth (auth-service): the user's own account. */

/** POST /api/auth/login. Any wrong login rejects with code UNAUTHORIZED. */
export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await apiClient.post<LoginResponse>('/api/auth/login', request)
  return response.data
}

/** PUT /api/auth/password. A wrong current password rejects with code BAD_REQUEST. */
export async function changePassword(request: ChangePasswordRequest): Promise<void> {
  await apiClient.put('/api/auth/password', request)
}
