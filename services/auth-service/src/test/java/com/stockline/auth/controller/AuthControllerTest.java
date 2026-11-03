package com.stockline.auth.controller;

import com.stockline.auth.constant.MessageKeys;
import com.stockline.auth.constant.UserType;
import com.stockline.auth.dto.LoginRequest;
import com.stockline.auth.dto.LoginResponse;
import com.stockline.auth.service.AuthService;
import com.stockline.common.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer only; the service is a Mockito mock. Checks the URL, the JSON shape, validation and the 401 format.
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

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

		// when / then
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
	void login_get_returns405() throws Exception {
		mockMvc.perform(get("/api/auth/login"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
	}
}
