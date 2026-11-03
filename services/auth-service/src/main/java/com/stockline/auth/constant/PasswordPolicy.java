package com.stockline.auth.constant;

/**
 * Length rules for passwords, used by every request that takes one.
 */
public final class PasswordPolicy {

	/** Shortest password a user can set. */
	public static final int MIN_LENGTH = 8;

	/** BCrypt reads at most 72 bytes of a password; anything after that would be ignored silently. */
	public static final int MAX_LENGTH = 72;

	private PasswordPolicy() {
	}
}
