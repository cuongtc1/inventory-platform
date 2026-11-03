package com.stockline.auth.constant;

/**
 * Whether a user can log in. Stored as a string in {@code users.status}. Users are deactivated, not deleted
 * (T3.2), so their name stays in the audit columns of everything they created.
 */
public enum UserStatus {
	ACTIVE,
	INACTIVE
}
