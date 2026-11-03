package com.stockline.auth.dto;

import com.stockline.auth.constant.UserType;

import java.time.Instant;

/**
 * Answer to a successful login. The frontend sends {@code accessToken} back in every request as
 * {@code Authorization: Bearer <accessToken>} and shows the user's name and role without decoding the token.
 *
 * @param tokenType always "Bearer" (the HTTP authentication scheme for tokens, RFC 6750)
 * @param expiresAt when the token stops working; the user has to log in again then (no refresh token)
 */
public record LoginResponse(
		String accessToken,
		String tokenType,
		Instant expiresAt,
		LoggedInUser user
) {

	/** The user who logged in: the same values as in the token's claims. */
	public record LoggedInUser(Long id, String email, String fullName, UserType userType) {
	}
}
