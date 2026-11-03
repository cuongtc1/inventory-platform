package com.stockline.auth.constant;

/**
 * A user's role, stored as a string in {@code users.user_type} and sent as the {@code role} claim of the JWT.
 *
 * <p>Declared from least to most rights: each role includes the rights of the ones before it
 * (STAFF &lt; MANAGER &lt; ADMIN). The rules that use this come with T3.4.
 */
public enum UserType {
	STAFF,
	MANAGER,
	ADMIN
}
