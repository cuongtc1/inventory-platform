package com.stockline.auth.controller;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * The signed-in user of a request, from the token a controller receives with {@code @AuthenticationPrincipal Jwt}.
 */
final class CurrentUser {

	private CurrentUser() {
	}

	/** The user's id: the token's {@code sub} claim (a string in the JWT standard). */
	static Long id(Jwt token) {
		return Long.valueOf(token.getSubject());
	}
}
