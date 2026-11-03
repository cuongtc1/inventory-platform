package com.stockline.auth.controller;

import com.stockline.auth.dto.LoginRequest;
import com.stockline.auth.dto.LoginResponse;
import com.stockline.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	@Operation(summary = "Log in",
			description = "Returns a JWT to send as `Authorization: Bearer <accessToken>`. Wrong email, wrong "
					+ "password and a deactivated user all give the same 401 UNAUTHORIZED")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}
}
