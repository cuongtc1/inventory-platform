package com.stockline.common.constant;

/**
 * A user's role, stored as a string in {@code users.user_type} and sent as the {@code role} claim of the JWT.
 *
 * <p>Declared from least to most rights: each role includes the rights of the ones before it
 * (STAFF &lt; MANAGER &lt; ADMIN, {@code JwtSecurity.roleHierarchy()}). In the library because every service's
 * role rules name these roles.
 */
public enum UserType {
	STAFF,
	MANAGER,
	ADMIN
}
