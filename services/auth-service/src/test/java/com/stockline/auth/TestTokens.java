package com.stockline.auth;

import com.stockline.auth.constant.UserType;
import com.stockline.common.constant.JwtClaimNames;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * For {@code @WebMvcTest}s: run a MockMvc request as a signed-in user, e.g.
 * {@code mockMvc.perform(get("/api/users").with(as(1L, UserType.ADMIN)))}.
 *
 * <p>spring-security-test's {@code jwt()} puts a ready-made, already "checked" token into the request, so no
 * signing is needed. It skips our role converter, so the authority is set here the same way: "ROLE_" + role.
 */
public final class TestTokens {

	private TestTokens() {
	}

	public static RequestPostProcessor as(Long userId, UserType role) {
		return SecurityMockMvcRequestPostProcessors.jwt()
				.jwt(token -> token
						.subject(String.valueOf(userId))
						.claim(JwtClaimNames.ROLE, role.name())
						.claim(JwtClaimNames.EMAIL, role.name().toLowerCase() + "@stockline.test"))
				.authorities(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}
}
