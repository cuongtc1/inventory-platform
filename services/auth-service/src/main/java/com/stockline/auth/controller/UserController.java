package com.stockline.auth.controller;

import com.stockline.auth.dto.CreateUserRequest;
import com.stockline.auth.dto.UpdateUserRequest;
import com.stockline.auth.dto.UserFilter;
import com.stockline.auth.dto.UserResponse;
import com.stockline.auth.service.UserService;
import com.stockline.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * User management. Only ADMIN tokens get here (WebSecurityConfig); other roles get 403 before this class runs.
 * Users are never deleted, only deactivated.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users (ADMIN)")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	@Operation(summary = "List users",
			description = "`search` matches part of the email or name, ignoring case. Filters `userType` and `status` "
					+ "are optional. Sort by `email`, `fullName`, `userType`, `status`, `createdAt`, ...")
	public PageResponse<UserResponse> list(
			@ParameterObject UserFilter filter,
			@ParameterObject @PageableDefault(size = 20, sort = "fullName") Pageable pageable) {
		return userService.list(filter, pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get one user")
	public UserResponse get(@PathVariable Long id) {
		return userService.get(id);
	}

	@PostMapping
	@Operation(summary = "Create a user", description = "The new user is ACTIVE and logs in with the given password")
	public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
		UserResponse created = userService.create(request);
		return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a user", description = "Email, name and role. You can't take ADMIN away from yourself")
	public UserResponse update(@AuthenticationPrincipal Jwt token, @PathVariable Long id,
							   @Valid @RequestBody UpdateUserRequest request) {
		return userService.update(CurrentUser.id(token), id, request);
	}

	@PostMapping("/{id}/deactivate")
	@Operation(summary = "Deactivate a user",
			description = "The user can't log in any more; tokens they already have work until they expire. "
					+ "You can't deactivate yourself")
	public UserResponse deactivate(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
		return userService.deactivate(CurrentUser.id(token), id);
	}

	@PostMapping("/{id}/activate")
	@Operation(summary = "Activate a user again")
	public UserResponse activate(@PathVariable Long id) {
		return userService.activate(id);
	}
}
