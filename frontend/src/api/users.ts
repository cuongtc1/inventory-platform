import type { PageQuery, PageResponse } from '../types/api'
import type { CreateUserRequest, UpdateUserRequest, UserFilter, UserResponse } from '../types/auth'
import { apiClient } from './client'

/** Calls to /api/users (auth-service, ADMIN only). Users are never deleted, only deactivated. */

/** GET /api/users: one page, with optional filters. */
export async function getUsers(
  query: PageQuery & UserFilter,
  signal?: AbortSignal,
): Promise<PageResponse<UserResponse>> {
  const response = await apiClient.get<PageResponse<UserResponse>>('/api/users', { params: query, signal })
  return response.data
}

/** GET /api/users/{id}. A missing user rejects with an ApiError with code NOT_FOUND. */
export async function getUser(id: number, signal?: AbortSignal): Promise<UserResponse> {
  const response = await apiClient.get<UserResponse>(`/api/users/${id}`, { signal })
  return response.data
}

/** POST /api/users. A taken email rejects with code DUPLICATE. */
export async function createUser(request: CreateUserRequest): Promise<UserResponse> {
  const response = await apiClient.post<UserResponse>('/api/users', request)
  return response.data
}

/** PUT /api/users/{id}. Taking ADMIN away from yourself rejects with code BAD_REQUEST. */
export async function updateUser(id: number, request: UpdateUserRequest): Promise<UserResponse> {
  const response = await apiClient.put<UserResponse>(`/api/users/${id}`, request)
  return response.data
}

/** POST /api/users/{id}/deactivate. Your own account rejects with code BAD_REQUEST. */
export async function deactivateUser(id: number): Promise<UserResponse> {
  const response = await apiClient.post<UserResponse>(`/api/users/${id}/deactivate`)
  return response.data
}

/** POST /api/users/{id}/activate. */
export async function activateUser(id: number): Promise<UserResponse> {
  const response = await apiClient.post<UserResponse>(`/api/users/${id}/activate`)
  return response.data
}
