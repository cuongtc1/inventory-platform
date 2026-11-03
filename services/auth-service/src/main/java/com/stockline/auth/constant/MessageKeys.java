package com.stockline.auth.constant;

/**
 * Keys of auth-service's {@code src/main/resources/messages.properties}. Use these constants instead of typing
 * a key, so a typo is a compile error instead of a missing message at runtime.
 *
 * <p>Texts every service shares (validation, not found, server errors, ...) are in the stockline-common library:
 * {@code com.stockline.common.constant.CommonMessageKeys}.
 */
public final class MessageKeys {

	// auth
	public static final String AUTH_LOGIN_FAILED = "auth.login.failed";

	// user (log only)
	public static final String USER_ADMIN_CREATED = "user.admin.created";

	private MessageKeys() {
	}
}
