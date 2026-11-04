import type { Audited } from './api'

/** Backend enum `UserType`: a user's role. STAFF < MANAGER < ADMIN, see hasRole() in constants/userType.ts. */
export type UserType = 'STAFF' | 'MANAGER' | 'ADMIN'

/** Backend enum `UserStatus`. An INACTIVE user can't log in. */
export type UserStatus = 'ACTIVE' | 'INACTIVE'

/** Backend `LoginRequest`. */
export interface LoginRequest {
  email: string
  password: string
}

/** Backend `LoginResponse.LoggedInUser`. */
export interface LoggedInUser {
  id: number
  email: string
  fullName: string
  userType: UserType
}

/** Backend `LoginResponse`. */
export interface LoginResponse {
  accessToken: string
  tokenType: string
  /** ISO-8601 in UTC. There is no refresh token: after this the user logs in again. */
  expiresAt: string
  user: LoggedInUser
}

/** Backend `ChangePasswordRequest`. */
export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

/** Backend `UserResponse`. */
export interface UserResponse extends Audited {
  id: number
  email: string
  fullName: string
  userType: UserType
  status: UserStatus
}

/** Backend `CreateUserRequest`. */
export interface CreateUserRequest {
  email: string
  fullName: string
  userType: UserType
  password: string
}

/** Backend `UpdateUserRequest`: no password and no status. */
export interface UpdateUserRequest {
  email: string
  fullName: string
  userType: UserType
}

/** Filters of GET /api/users, sent as query parameters with the same names. All optional. */
export interface UserFilter {
  search?: string
  userType?: UserType
  status?: UserStatus
}
