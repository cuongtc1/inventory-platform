package com.stockline.auth.controller;

import com.stockline.auth.dto.ChangePasswordRequest;
import com.stockline.auth.dto.LoginRequest;
import com.stockline.auth.dto.LoginResponse;
import com.stockline.auth.dto.UserResponse;
import com.stockline.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The signed-in user's own account. {@code @AuthenticationPrincipal Jwt}: Spring Security hands the checked token
 * of the request to the method.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	// No "Authorize" lock in Swagger UI: this is the one endpoint that works without a token
	@SecurityRequirements
	@Operation(summary = "Log in",
			description = "Returns a JWT to send as `Authorization: Bearer <accessToken>`. Wrong email, wrong "
					+ "password and a deactivated user all give the same 401 UNAUTHORIZED")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@GetMapping("/me")
	@Operation(summary = "The signed-in user", description = "Read from the database, not from the token")
	public UserResponse me(@AuthenticationPrincipal Jwt token) {
		return authService.me(CurrentUser.id(token));
	}

	@PutMapping("/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Change my password",
			description = "400 BAD_REQUEST if currentPassword is wrong. Tokens issued before stay valid until they expire")
	public void changePassword(@AuthenticationPrincipal Jwt token, @Valid @RequestBody ChangePasswordRequest request) {
		authService.changePassword(CurrentUser.id(token), request);
	}
}
