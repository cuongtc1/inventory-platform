package com.stockline.common.constant;

/**
 * Names of the claims (fields) in Stockline's JWT, shared by auth-service, which writes them, and by the
 * api-gateway and the services, which read them (docs/conventions.md section 11).
 *
 * <p>Standard claims keep their JWT names and need no constant here: {@code sub} (the user's id), {@code iss},
 * {@code iat} (issued at) and {@code exp} (expires at).
 */
public final class JwtClaimNames {

	/** The user's login email, also used as {@code created_by} / {@code updated_by} in audit columns. */
	public static final String EMAIL = "email";

	/** The user's full name, for display. */
	public static final String NAME = "name";

	/** The user's role: {@code STAFF}, {@code MANAGER} or {@code ADMIN} (the {@code user_type} column). */
	public static final String ROLE = "role";

	private JwtClaimNames() {
	}
}
