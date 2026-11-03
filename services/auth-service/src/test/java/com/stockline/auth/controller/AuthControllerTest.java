package com.stockline.auth.controller;

import com.stockline.auth.config.SecurityConfig;
import com.stockline.auth.config.WebSecurityConfig;
import com.stockline.auth.constant.MessageKeys;
import com.stockline.auth.constant.UserStatus;
import com.stockline.auth.constant.UserType;
import com.stockline.auth.dto.ChangePasswordRequest;
import com.stockline.auth.dto.LoginRequest;
import com.stockline.auth.dto.LoginResponse;
import com.stockline.auth.dto.UserResponse;
import com.stockline.auth.service.AuthService;
import com.stockline.common.exception.InvalidRequestException;
import com.stockline.common.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static com.stockline.auth.TestTokens.as;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer with the real security rules (imported below); the service is a Mockito mock. Checks URLs, JSON,
 * validation, and that 401 has our error format.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, WebSecurityConfig.class})
class AuthControllerTest {

	private static final Instant NOW = Instant.parse("2026-11-03T08:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@Test
	void login_validCredentials_returns200WithTokenAndUser() throws Exception {
		// given
		when(authService.login(new LoginRequest("linh@corp.com", "secret-pass-1"))).thenReturn(new LoginResponse(
				"header.claims.signature", "Bearer", Instant.parse("2026-11-03T16:00:00Z"),
				new LoginResponse.LoggedInUser(7L, "linh@corp.com", "Linh Nguyen", UserType.MANAGER)));

		// when / then: no token needed
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "linh@corp.com", "password": "secret-pass-1" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").value("header.claims.signature"))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresAt").value("2026-11-03T16:00:00Z"))
				.andExpect(jsonPath("$.user.id").value(7))
				.andExpect(jsonPath("$.user.email").value("linh@corp.com"))
				.andExpect(jsonPath("$.user.fullName").value("Linh Nguyen"))
				.andExpect(jsonPath("$.user.userType").value("MANAGER"))
				// The hash must never leave the service
				.andExpect(jsonPath("$.user.passwordHash").doesNotExist());
	}

	@Test
	void login_wrongCredentials_returns401ProblemDetail() throws Exception {
		// given
		when(authService.login(any())).thenThrow(new UnauthorizedException(MessageKeys.AUTH_LOGIN_FAILED));

		// when / then
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "linh@corp.com", "password": "wrong" }
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.detail").value("Wrong email or password"))
				.andExpect(jsonPath("$.instance").value("/api/auth/login"));
	}

	@Test
	void login_blankFields_returns400WithFieldErrors() throws Exception {
		// when / then
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": " ", "password": "" }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("email", "password")));
		verifyNoInteractions(authService);
	}

	@Test
	void login_passwordLongerThan72Characters_returns400() throws Exception {
		// when / then
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "linh@corp.com", "password": "%s" }
								""".formatted("x".repeat(73))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("password"));
		verifyNoInteractions(authService);
	}

	@Test
	void me_withToken_returnsUserOfTokenSubject() throws Exception {
		// given
		when(authService.me(7L)).thenReturn(new UserResponse(7L, "linh@corp.com", "Linh Nguyen", UserType.MANAGER,
				UserStatus.ACTIVE, NOW, "system", NOW, "admin@stockline.local"));

		// when / then
		mockMvc.perform(get("/api/auth/me").with(as(7L, UserType.MANAGER)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.userType").value("MANAGER"))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.updatedBy").value("admin@stockline.local"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void me_withoutToken_returns401ProblemDetail() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.detail").value("Please log in (the token is missing, invalid or expired)"))
				.andExpect(jsonPath("$.instance").value("/api/auth/me"));
		verifyNoInteractions(authService);
	}

	@Test
	void me_withGarbageToken_returns401ProblemDetail() throws Exception {
		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer not.a.token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
		verifyNoInteractions(authService);
	}

	@Test
	void changePassword_valid_returns204() throws Exception {
		// when / then
		mockMvc.perform(put("/api/auth/password").with(as(7L, UserType.STAFF))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "currentPassword": "old-pass-1", "newPassword": "new-pass-1" }
								"""))
				.andExpect(status().isNoContent());
		verify(authService).changePassword(7L, new ChangePasswordRequest("old-pass-1", "new-pass-1"));
	}

	@Test
	void changePassword_wrongCurrentPassword_returns400() throws Exception {
		// given
		doThrow(new InvalidRequestException(MessageKeys.AUTH_PASSWORD_CURRENT_WRONG))
				.when(authService).changePassword(eq(7L), any());

		// when / then
		mockMvc.perform(put("/api/auth/password").with(as(7L, UserType.STAFF))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "currentPassword": "wrong-pass", "newPassword": "new-pass-1" }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.detail").value("The current password is wrong"));
	}

	@Test
	void changePassword_newPasswordTooShort_returns400() throws Exception {
		mockMvc.perform(put("/api/auth/password").with(as(7L, UserType.STAFF))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "currentPassword": "old-pass-1", "newPassword": "short" }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].field").value("newPassword"));
		verifyNoInteractions(authService);
	}

	@Test
	void login_get_returns405() throws Exception {
		mockMvc.perform(get("/api/auth/login").with(as(1L, UserType.ADMIN)))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
	}
}
